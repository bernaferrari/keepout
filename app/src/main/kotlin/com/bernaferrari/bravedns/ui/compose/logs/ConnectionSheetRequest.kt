/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.compose.runtime.Immutable
import com.bernaferrari.bravedns.database.ConnectionTracker
import com.bernaferrari.bravedns.database.DnsLog
import com.bernaferrari.bravedns.service.FirewallRuleset
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRulesTab

/**
 * One log row as the Connection sheet needs it, from either log. Build it with
 * [ConnectionTracker.toConnectionSheetRequest] or [DnsLog.toConnectionSheetRequest]; Stats and
 * App detail rows can fill it directly. Labels arrive formatted, as the rows already format them.
 */
@Immutable
data class ConnectionSheetRequest(
    /** The app's uid; negative (unknown app) hides every app-scoped action. */
    val uid: Int,
    val appName: String,
    val packageName: String? = null,
    /** The domain looked up, when known. Only an exact name becomes a domain rule. */
    val domain: String? = null,
    val ipAddress: String? = null,
    val port: Int = 0,
    val timeLabel: String,
    /** "HTTPS" for connections, the record type ("A") for lookups. */
    val protocolLabel: String? = null,
    val isBlocked: Boolean,
    /** Connections: `ConnectionTracker.blockedByRule`, a [FirewallRuleset] id. Null for lookups. */
    val ruleId: String? = null,
    val isDnsLookup: Boolean = false,
    /** Names of the DNS blocklists that stopped it ([parseBlocklists]). */
    val blocklists: List<String> = emptyList(),
    /** The key/value table behind "Details" (the rows' existing details lists fit as is). */
    val details: List<RethinkLogDetail> = emptyList(),
)

fun ConnectionTracker.toConnectionSheetRequest(
    timeLabel: String,
    protocolLabel: String?,
    details: List<RethinkLogDetail> = emptyList(),
) = ConnectionSheetRequest(
    uid = uid,
    appName = appName,
    packageName = packageName.takeIf { it.isNotBlank() },
    domain = dnsQuery?.takeIf { it.isNotBlank() },
    ipAddress = ipAddress.takeIf { it.isNotBlank() },
    port = port,
    timeLabel = timeLabel,
    protocolLabel = protocolLabel,
    isBlocked = isBlocked,
    ruleId = blockedByRule.takeIf { it.isNotBlank() },
    blocklists = parseBlocklists(blocklists),
    details = details,
)

fun DnsLog.toConnectionSheetRequest(
    timeLabel: String,
    typeLabel: String?,
    details: List<RethinkLogDetail> = emptyList(),
) = ConnectionSheetRequest(
    uid = uid,
    appName = appName,
    packageName = packageName.takeIf { it.isNotBlank() },
    domain = queryStr.takeIf { it.isNotBlank() },
    timeLabel = timeLabel,
    protocolLabel = typeLabel,
    isBlocked = isBlocked,
    isDnsLookup = true,
    blocklists = parseBlocklists(blockLists),
    details = details,
)

/**
 * Where the sheet's links lead. Every one is optional: a null destination hides its action, so
 * the sheet works before navigation is wired (WS1 wires these; the sheet dismisses itself first).
 */
@Immutable
data class ConnectionSheetNavigation(
    /** Universal rules with the row for a [FirewallRuleset] id highlighted. */
    val onOpenUniversalRules: ((focusKey: String) -> Unit)? = null,
    val onOpenProxySettings: (() -> Unit)? = null,
    /** The rules list for [uid] (UID_EVERYBODY for all apps) on [tab], searching [query]. */
    val onOpenRules: ((uid: Int, tab: RethinkRulesTab, query: String) -> Unit)? = null,
    /** "Open Clock": the app's page in Keepout (App detail). */
    val onOpenApp: ((uid: Int) -> Unit)? = null,
)
