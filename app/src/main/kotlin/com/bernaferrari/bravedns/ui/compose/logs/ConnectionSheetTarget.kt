/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import com.bernaferrari.bravedns.rules.IpRuleInput
import com.bernaferrari.bravedns.service.FirewallRuleset
import com.bernaferrari.bravedns.util.Constants.Companion.UID_EVERYBODY
import inet.ipaddr.IPAddress

/**
 * What a [ConnectionSheetRequest] lets the sheet act on: the app (when known), the *exact* domain
 * and the parsed IP a rule can be written for, and the [reason] behind the verdict. Pure: derived
 * once per request, read by both the rule reads and the writes.
 */
internal class ConnectionSheetTarget private constructor(
    /** The app's uid, or null for an unknown app (every app-scoped action hides). */
    val appUid: Int?,
    /** The exact domain a domain rule can name; null for wildcards and unknown domains. */
    val domain: String?,
    val ip: IPAddress?,
    val reason: BlockReason,
) {
    /** The rule group targets the domain when there is an exact one, else the IP. */
    val kind: RethinkConnectionTargetKind? = when {
        domain != null -> RethinkConnectionTargetKind.Domain
        ip != null -> RethinkConnectionTargetKind.Ip
        else -> null
    }

    /** The scope the rule group opens on: the app's own rules when there is an app. */
    val defaultScope: RethinkConnectionRuleScope =
        if (appUid != null) RethinkConnectionRuleScope.App else RethinkConnectionRuleScope.AllApps

    /** The uid whose rules [scope] edits; all apps when there is no app to scope to. */
    fun ruleUid(scope: RethinkConnectionRuleScope): Int =
        if (scope == RethinkConnectionRuleScope.App && appUid != null) appUid else UID_EVERYBODY

    /** The uid owning a rule a fix removes: all apps when [universal], else the app (null: none). */
    fun ruleOwner(universal: Boolean): Int? = if (universal) UID_EVERYBODY else appUid

    /** Only scopes the target has: without an app, the group stays on all apps. */
    fun allows(scope: RethinkConnectionRuleScope): Boolean =
        appUid != null || scope == RethinkConnectionRuleScope.AllApps

    companion object {
        fun of(request: ConnectionSheetRequest): ConnectionSheetTarget {
            val domain = request.domain?.let { DnsQueryRuleEditor.exactQueryDomain(it) }
            return ConnectionSheetTarget(
                appUid = request.uid.takeIf { it >= 0 },
                domain = domain,
                ip = request.ipAddress?.let(IpRuleInput::parse)?.address,
                reason = reasonFor(request, hasDomain = domain != null),
            )
        }

        private fun reasonFor(request: ConnectionSheetRequest, hasDomain: Boolean): BlockReason =
            if (request.isDnsLookup) {
                dnsBlockReasonFor(request.isBlocked, request.blocklists)
            } else {
                blockReasonFor(request.ruleId?.let { FirewallRuleset.getFirewallRule(it) }, hasDomain, request.blocklists)
            }
    }
}
