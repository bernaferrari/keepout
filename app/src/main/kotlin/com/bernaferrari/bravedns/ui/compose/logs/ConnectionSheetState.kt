/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import com.bernaferrari.bravedns.service.DomainRulesManager
import com.bernaferrari.bravedns.service.FirewallManager
import com.bernaferrari.bravedns.service.FirewallManager.ConnectionStatus
import com.bernaferrari.bravedns.service.IpRuleNormalizer
import com.bernaferrari.bravedns.service.IpRulesManager
import com.bernaferrari.bravedns.service.IpRulesManager.IpRuleStatus
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRuleAction
import com.bernaferrari.bravedns.util.Constants.Companion.UID_EVERYBODY

/** What the sheet read for its target, app and scope; reloaded after every write and undo. */
internal data class ConnectionSheetState(
    val hasApp: Boolean,
    val connectionStatus: ConnectionStatus?,
    val tempAllowed: Boolean,
    /** The app's and the universal exact domain rule, for "Remove this rule" and "Allow this domain". */
    val appDomainRule: DomainRulesManager.Status,
    val universalDomainRule: DomainRulesManager.Status,
    /** The exact IP rule port (connection port, else any port) that blocks it, per scope. */
    val appIpBlockPort: Int?,
    val universalIpBlockPort: Int?,
    val appIpRule: IpRuleStatus,
    /** The rule group: the exact rule in the selected scope, and whether another rule decides. */
    val groupAction: RethinkRuleAction,
    val groupInherited: Boolean,
) {
    fun blockingDomainRule(universal: Boolean) =
        (if (universal) universalDomainRule else appDomainRule) == DomainRulesManager.Status.BLOCK

    fun blockingIpRulePort(universal: Boolean) = if (universal) universalIpBlockPort else appIpBlockPort
}

/** Reads the app's flags and the exact rules for [target]; the rule group reads [groupScope]. */
internal suspend fun loadConnectionSheetState(
    request: ConnectionSheetRequest,
    target: ConnectionSheetTarget,
    groupScope: RethinkConnectionRuleScope,
): ConnectionSheetState {
    val app = target.appUid?.takeIf { FirewallManager.hasUid(it) }
    val rules = ExactRules(request, target)
    val group = rules.group(target.ruleUid(groupScope), groupScope)
    return ConnectionSheetState(
        hasApp = app != null,
        connectionStatus = app?.let { FirewallManager.connectionStatus(it) },
        tempAllowed = app != null && FirewallManager.isTempAllowed(app),
        appDomainRule = rules.domain(target.appUid),
        universalDomainRule = rules.domain(UID_EVERYBODY),
        appIpBlockPort = rules.ipBlockPort(target.appUid),
        universalIpBlockPort = rules.ipBlockPort(UID_EVERYBODY),
        appIpRule = rules.ip(target.appUid, port = 0),
        groupAction = group.action,
        groupInherited = group.inherited,
    )
}

/** The rule group's exact rule, and whether a wider rule (wildcard, range, port) decides instead. */
private data class GroupRule(val action: RethinkRuleAction, val inherited: Boolean)

/** The exact domain and IP rules for one target, per uid; a null uid (no app) has none. */
private class ExactRules(private val request: ConnectionSheetRequest, private val target: ConnectionSheetTarget) {
    private val storage = target.ip?.let(IpRuleNormalizer::storageText)

    suspend fun domain(uid: Int?): DomainRulesManager.Status {
        val domain = target.domain
        if (uid == null || domain == null) return DomainRulesManager.Status.NONE
        return DomainRulesManager.Status.getStatus(DomainRulesManager.getObj(uid, domain)?.status)
    }

    suspend fun ip(uid: Int?, port: Int): IpRuleStatus {
        if (uid == null || storage == null) return IpRuleStatus.NONE
        return IpRuleStatus.getStatus(IpRulesManager.getObj(uid, storage, port)?.status)
    }

    /** The port of the exact rule that blocks it: the connection's own port first, then any port. */
    suspend fun ipBlockPort(uid: Int?): Int? =
        listOf(request.port, 0).distinct().firstOrNull { ip(uid, it) == IpRuleStatus.BLOCK }

    suspend fun group(uid: Int, scope: RethinkConnectionRuleScope): GroupRule {
        val domain = target.domain
        return when {
            domain != null -> {
                val exact = domain(uid)
                val inherited = exact == DomainRulesManager.Status.NONE &&
                    DomainRulesManager.status(domain, uid) != DomainRulesManager.Status.NONE
                GroupRule(exact.toRuleAction(), inherited)
            }
            target.ip != null -> {
                val exact = ip(uid, port = 0)
                val effective = IpRulesManager.hasRule(uid, request.ipAddress.orEmpty(), request.port)
                GroupRule(exact.toRuleAction(scope), inherited = exact == IpRuleStatus.NONE && effective != IpRuleStatus.NONE)
            }
            else -> GroupRule(RethinkRuleAction.None, inherited = false)
        }
    }
}

private fun DomainRulesManager.Status.toRuleAction() = when (this) {
    DomainRulesManager.Status.TRUST -> RethinkRuleAction.Trust
    DomainRulesManager.Status.BLOCK -> RethinkRuleAction.Block
    DomainRulesManager.Status.NONE -> RethinkRuleAction.None
}

/**
 * The firewall honours an all-apps IP "trust" only as BYPASS_UNIVERSAL (a universal TRUST row is
 * ignored), so for all apps Trust reads that status; per app it is TRUST.
 */
private fun IpRuleStatus.toRuleAction(scope: RethinkConnectionRuleScope) = when (this) {
    IpRuleStatus.BLOCK -> RethinkRuleAction.Block
    IpRuleStatus.TRUST -> if (scope == RethinkConnectionRuleScope.App) RethinkRuleAction.Trust else RethinkRuleAction.None
    IpRuleStatus.BYPASS_UNIVERSAL -> if (scope == RethinkConnectionRuleScope.AllApps) RethinkRuleAction.Trust else RethinkRuleAction.None
    IpRuleStatus.NONE -> RethinkRuleAction.None
}
