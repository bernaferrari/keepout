/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.ActivityLogKinds
import com.bernaferrari.bravedns.database.ActivityLogQuery
import com.bernaferrari.bravedns.database.ActivityVerdict
import com.bernaferrari.bravedns.service.FirewallRuleset

/** What the screen was opened for: a search or one app. Fixed for its lifetime, unlike [LogFilters]. */
@Immutable
internal data class LogScope(val query: String, val appUid: Int?, val appName: String)

/** The filters the user sets; they survive switching views and tabs. */
@Stable
internal class LogFilters(
    type: RethinkLogType = RethinkLogType.All,
    verdict: RethinkLogFilter = RethinkLogFilter.All,
    rules: List<String> = emptyList(),
    appUid: Int? = null,
    appName: String = "",
) {
    var type by mutableStateOf(type)
    var verdict by mutableStateOf(verdict)
    var rules by mutableStateOf(rules)
    /** The app picked in the app filter (by uid: names collide), and its name for the empty state. */
    var appUid by mutableStateOf(appUid)
    var appName by mutableStateOf(appName)

    /** The query these filters ask for within [scope]: the screen's own app wins over a picked one. */
    fun query(scope: LogScope) = ActivityLogQuery(
        kinds = type.toKinds(),
        verdict = verdict.toVerdict(),
        rules = rules.toSet(),
        search = scope.query,
        uid = scope.appUid ?: appUid,
    )

    /**
     * The reasons the Reason filter offers. Reasons are the firewall's: lookups have none, so the
     * filter is offered wherever connections are.
     */
    fun reasonOptions(): List<FirewallRuleset> = if (type == RethinkLogType.Lookups) {
        emptyList()
    } else {
        when (verdict) {
            RethinkLogFilter.Blocked -> FirewallRuleset.getBlockedRules()
            RethinkLogFilter.Allowed -> FirewallRuleset.getAllowedRules()
            RethinkLogFilter.All -> FirewallRuleset.entries.toList()
        }
    }

    /** What the empty list names as searched: the scope's app, else the picked app, else the query. */
    fun searchLabel(scope: LogScope): String =
        scope.appName.ifBlank { appName.takeIf { appUid != null }.orEmpty() }.ifBlank { scope.query }

    fun selectVerdict(value: RethinkLogFilter) {
        verdict = value
        rules = emptyList()
    }

    fun selectType(value: RethinkLogType) {
        type = value
        // Lookups have no firewall reason: a reason filter would only hide them.
        if (value == RethinkLogType.Lookups) rules = emptyList()
    }

    fun toggleRule(ruleId: String) {
        rules = if (ruleId in rules) rules - ruleId else rules + ruleId
    }

    companion object {
        val Saver = listSaver<LogFilters, Any?>(
            save = { listOf(it.type.name, it.verdict.name, ArrayList(it.rules), it.appUid, it.appName) },
            restore = { saved ->
                LogFilters(
                    type = RethinkLogType.valueOf(saved[0] as String),
                    verdict = RethinkLogFilter.valueOf(saved[1] as String),
                    rules = (saved[2] as List<*>).filterIsInstance<String>(),
                    appUid = saved[3] as Int?,
                    appName = saved[4] as String,
                )
            },
        )
    }
}

/** The non-default filters the user set, each a chip that removes itself; the screen's own app is not one. */
@Composable
internal fun activeFilters(filters: LogFilters): List<RethinkActiveLogFilter> = buildList {
    when (filters.type) {
        RethinkLogType.All -> Unit
        RethinkLogType.Connections ->
            add(RethinkActiveLogFilter("type", stringResource(R.string.activity_type_connections)) { filters.type = RethinkLogType.All })
        RethinkLogType.Lookups ->
            add(RethinkActiveLogFilter("type", stringResource(R.string.activity_type_lookups)) { filters.type = RethinkLogType.All })
    }
    if (filters.appUid != null) {
        add(RethinkActiveLogFilter("app", filters.appName) { filters.appUid = null })
    }
    for (id in filters.rules) {
        val title = FirewallRuleset.getFirewallRule(id)?.title?.let { stringResource(it) } ?: id
        add(RethinkActiveLogFilter("rule:$id", stringResource(R.string.activity_filter_reason_chip, title)) { filters.rules = filters.rules - id })
    }
}

private fun RethinkLogType.toKinds() = when (this) {
    RethinkLogType.All -> ActivityLogKinds.All
    RethinkLogType.Connections -> ActivityLogKinds.Connections
    RethinkLogType.Lookups -> ActivityLogKinds.Lookups
}

private fun RethinkLogFilter.toVerdict() = when (this) {
    RethinkLogFilter.All -> ActivityVerdict.All
    RethinkLogFilter.Allowed -> ActivityVerdict.Allowed
    RethinkLogFilter.Blocked -> ActivityVerdict.Blocked
}
