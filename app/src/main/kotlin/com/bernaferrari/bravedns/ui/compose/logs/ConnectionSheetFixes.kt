/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.service.DomainRulesManager
import com.bernaferrari.bravedns.service.FirewallManager
import com.bernaferrari.bravedns.service.IpRulesManager.IpRuleStatus
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRulesTab
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

/**
 * Turns [BlockReason] fixes into the sheet's buttons and performs them through [writer]. A fix
 * changes only the rule that decided [request]; a fix already made, or one the rules read in
 * [state] make moot, has no button. A remove whose exact rule isn't there (a wildcard, range or
 * port rule matched) opens the rules list through [links] instead.
 */
internal class ConnectionSheetFixes(
    private val request: ConnectionSheetRequest,
    private val target: ConnectionSheetTarget,
    private val state: ConnectionSheetState?,
    private val writer: ConnectionSheetWriter,
    private val links: ConnectionSheetLinks,
    /** The app as the copy names it ("Allow Clock on Wi‑Fi"). */
    private val appName: String,
) {
    @Composable
    fun actionFor(fix: QuickFix?): RethinkConnectionAction? {
        val current = state ?: return null
        if (fix == null || fix in writer.applied) return null
        return when (fix) {
            is QuickFix.AllowApp -> allowApp(fix, current)
            QuickFix.AllowTemporarily -> allowTemporarily(current)
            is QuickFix.RemoveDomainRule -> removeDomainRule(fix, current)
            is QuickFix.RemoveIpRule -> removeIpRule(fix, current)
            QuickFix.TrustDomain -> trustDomain(current)
            QuickFix.TrustIp -> trustIp(current)
            is QuickFix.OpenUniversalRules -> links.openUniversalRules(fix.focusKey)
            QuickFix.OpenProxySettings -> links.openProxySettings()
        }
    }

    /** Clears the app's own flag on one network, keeping its firewall status. */
    @Composable
    private fun allowApp(fix: QuickFix.AllowApp, current: ConnectionSheetState): RethinkConnectionAction? {
        val uid = target.appUid?.takeIf { current.hasApp } ?: return null
        val status = current.connectionStatus ?: return null
        val next = status.allowing(fix.network)
        // The flag is already clear (changed since): nothing left to fix here.
        if (next == status) return null
        val copy = fix.network.allowCopy()
        return writeAction(
            fix = fix,
            label = stringResource(copy.label, appName),
            message = stringResource(copy.done, appName),
            icon = copy.icon,
            log = "allow app $uid: $status -> $next",
        ) {
            val appStatus = FirewallManager.appStatus(uid).takeUnless { it.isUntracked() } ?: FirewallManager.FirewallStatus.NONE
            val before = FirewallManager.connectionStatus(uid)
            FirewallManager.updateFirewallStatus(uid, appStatus, before.allowing(fix.network))
            return@writeAction { FirewallManager.updateFirewallStatus(uid, appStatus, before) }
        }
    }

    @Composable
    private fun allowTemporarily(current: ConnectionSheetState): RethinkConnectionAction? {
        val uid = target.appUid?.takeIf { current.hasApp && !current.tempAllowed } ?: return null
        return writeAction(
            fix = QuickFix.AllowTemporarily,
            label = stringResource(R.string.temp_allow_label),
            message = stringResource(R.string.connection_done_allowed_temporarily, appName),
            icon = MaterialSymbols.Filled.Timer,
            log = "temp allow $uid",
        ) {
            FirewallManager.updateTempAllow(uid, true)
            return@writeAction { FirewallManager.updateTempAllow(uid, false) }
        }
    }

    @Composable
    private fun removeDomainRule(fix: QuickFix.RemoveDomainRule, current: ConnectionSheetState): RethinkConnectionAction? {
        val owner = target.ruleOwner(fix.universal) ?: return null
        val name = target.domain
        if (name == null || !current.blockingDomainRule(fix.universal)) {
            // Only a wildcard matched (or no exact name): the rule lives in the list.
            return links.openRules(owner, RethinkRulesTab.DOMAIN, request.domain.orEmpty())
        }
        return writeAction(
            fix = fix,
            label = stringResource(R.string.connection_fix_remove_rule),
            message = stringResource(R.string.connection_done_rule_removed),
            icon = MaterialSymbols.Filled.DeleteOutline,
            log = "remove domain rule $name, $owner",
        ) { writeDomainRule(name, owner, "", DomainRulesManager.Status.NONE) }
    }

    @Composable
    private fun removeIpRule(fix: QuickFix.RemoveIpRule, current: ConnectionSheetState): RethinkConnectionAction? {
        val owner = target.ruleOwner(fix.universal) ?: return null
        val address = target.ip
        val port = current.blockingIpRulePort(fix.universal)
        if (address == null || port == null) {
            return links.openRules(owner, RethinkRulesTab.IP, request.ipAddress.orEmpty())
        }
        return writeAction(
            fix = fix,
            label = stringResource(R.string.connection_fix_remove_rule),
            message = stringResource(R.string.connection_done_rule_removed),
            icon = MaterialSymbols.Filled.DeleteOutline,
            log = "remove ip rule $address:$port, $owner",
        ) { writeIpRule(owner, address, IpRuleStatus.NONE, port) }
    }

    @Composable
    private fun trustDomain(current: ConnectionSheetState): RethinkConnectionAction? {
        val uid = target.appUid ?: return null
        val name = target.domain?.takeIf { current.appDomainRule != DomainRulesManager.Status.TRUST } ?: return null
        return writeAction(
            fix = QuickFix.TrustDomain,
            label = stringResource(R.string.connection_fix_allow_domain),
            message = stringResource(R.string.connection_done_domain_trusted, appName),
            icon = MaterialSymbols.Filled.Verified,
            log = "trust domain $name, $uid",
        ) { writeDomainRule(name, uid, request.ipAddress.orEmpty(), DomainRulesManager.Status.TRUST) }
    }

    @Composable
    private fun trustIp(current: ConnectionSheetState): RethinkConnectionAction? {
        val uid = target.appUid ?: return null
        val address = target.ip?.takeIf { current.appIpRule != IpRuleStatus.TRUST } ?: return null
        return writeAction(
            fix = QuickFix.TrustIp,
            label = stringResource(R.string.connection_fix_trust_ip),
            message = stringResource(R.string.connection_done_ip_trusted, appName),
            icon = MaterialSymbols.Filled.Verified,
            log = "trust ip $address, $uid",
        ) { writeIpRule(uid, address, IpRuleStatus.TRUST) }
    }

    private fun writeAction(
        fix: QuickFix,
        label: String,
        message: String,
        icon: ImageVector,
        log: String,
        write: SheetWrite,
    ) = RethinkConnectionAction(label, icon = icon, onClick = { writer.mutate(message, fix, log, write) })
}

/**
 * The sheet's links: each dismisses the sheet, then navigates. A destination the host didn't wire
 * ([ConnectionSheetNavigation]) has no action.
 */
internal class ConnectionSheetLinks(
    private val navigation: ConnectionSheetNavigation,
    private val onDismiss: () -> Unit,
) {
    @Composable
    fun openRules(uid: Int, tab: RethinkRulesTab, query: String): RethinkConnectionAction? =
        navigation.onOpenRules?.let { open ->
            RethinkConnectionAction(
                stringResource(R.string.connection_fix_open_rules),
                icon = MaterialSymbols.Filled.Rule,
                onClick = { go { open(uid, tab, query) } },
            )
        }

    @Composable
    fun openUniversalRules(focusKey: String): RethinkConnectionAction? =
        navigation.onOpenUniversalRules?.let { open ->
            RethinkConnectionAction(
                stringResource(R.string.connection_fix_open_universal),
                icon = MaterialSymbols.Filled.Tune,
                onClick = { go { open(focusKey) } },
            )
        }

    @Composable
    fun openProxySettings(): RethinkConnectionAction? =
        navigation.onOpenProxySettings?.let { open ->
            RethinkConnectionAction(
                stringResource(R.string.connection_fix_open_proxy),
                icon = MaterialSymbols.Filled.ConversionPath,
                onClick = { go(open) },
            )
        }

    /** "Open Clock": the app's page, when there is an app and somewhere to open it. */
    fun openApp(uid: Int?): (() -> Unit)? {
        val open = navigation.onOpenApp ?: return null
        val app = uid ?: return null
        return { go { open(app) } }
    }

    private fun go(destination: () -> Unit) {
        onDismiss()
        destination()
    }
}

/** An Allow-app fix's button label, its done message, and the network's glyph. */
private class AllowAppCopy(@StringRes val label: Int, @StringRes val done: Int, val icon: ImageVector)

private fun BlockedNetwork.allowCopy(): AllowAppCopy = when (this) {
    BlockedNetwork.Wifi -> AllowAppCopy(
        R.string.connection_fix_allow_app_wifi,
        R.string.connection_done_allowed_wifi,
        MaterialSymbols.Filled.Wifi,
    )
    BlockedNetwork.MobileData -> AllowAppCopy(
        R.string.connection_fix_allow_app_mobile,
        R.string.connection_done_allowed_mobile,
        MaterialSymbols.Filled.SignalCellularAlt,
    )
    BlockedNetwork.Everywhere -> AllowAppCopy(
        R.string.connection_fix_allow_app_everywhere,
        R.string.connection_done_allowed,
        MaterialSymbols.Filled.CheckCircle,
    )
}
