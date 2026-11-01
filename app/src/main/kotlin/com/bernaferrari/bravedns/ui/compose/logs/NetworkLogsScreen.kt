/*
 * Copyright 2024 RethinkDNS and its authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.ActivityLogEntry
import com.bernaferrari.bravedns.database.ConnectionTrackerRepository
import com.bernaferrari.bravedns.database.DnsLogRepository
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.ui.compose.theme.RethinkOverflowItem
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols
import com.bernaferrari.bravedns.viewmodel.ActivityLogViewModel

/**
 * The log: connections and DNS lookups as one timeline (All), or either alone, filtered by verdict,
 * reason, app and search. One control row: the verdict group and a Filters button, whose sheet
 * holds Type, App and Reason; active ones show as removable chips under the row. Every row opens the Connection sheet (why it was allowed or blocked, and
 * the fix); at Expanded width rows select into a pane that opens the same sheet.
 *
 * As the Activity tab's Log view the host passes [viewSwitch] (Log · Insights) and a null
 * [onBackClick]. Pushed with a [query] (e.g. from Insights) it lists only matching entries, titled
 * by the query; pushed with an [appUid] (App detail's "See all activity") it lists that app's
 * entries only, titled by [appName].
 */
@Composable
fun NetworkLogsScreen(
    connectionTrackerRepository: ConnectionTrackerRepository,
    dnsLogRepository: DnsLogRepository,
    persistentState: PersistentState,
    eventLogger: EventLogger,
    onAppLogsClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    sheetNavigation: ConnectionSheetNavigation = ConnectionSheetNavigation(),
    query: String = "",
    appUid: Int? = null,
    appName: String = "",
    title: String? = null,
    viewSwitch: (@Composable () -> Unit)? = null,
    viewModel: ActivityLogViewModel = rememberActivityLogViewModel(),
) {
    var sheetTarget by remember { mutableStateOf<LogSheetTarget?>(null) }
    // Filters survive switching types and views (saveable), not only recomposition.
    val filters = rememberSaveable(saver = LogFilters.Saver) { LogFilters() }
    val scope = LogScope(query = query, appUid = appUid, appName = appName)
    val content: @Composable ((RethinkLogToolbarActions) -> Unit) -> Unit = { onToolbarActionsChange ->
        ActivityLogContent(
            filters = filters,
            scope = scope,
            viewModel = viewModel,
            connections = connectionTrackerRepository,
            lookups = dnsLogRepository,
            persistentState = persistentState,
            onOpenSheet = { sheetTarget = it },
            onToolbarActionsChange = onToolbarActionsChange,
        )
    }
    RethinkLogsScreenShell(
        strings = RethinkLogsStrings(
            title = title ?: appName.ifBlank { query }.ifBlank { stringResource(R.string.lbl_logs) },
            network = stringResource(R.string.firewall_act_network_monitor_tab),
            dns = stringResource(R.string.dns_mode_info_title),
            moreActions = stringResource(R.string.wireguard_fab_more_actions),
            refresh = stringResource(R.string.cd_refresh),
            clear = stringResource(R.string.fapps_filter_clear_btn),
            connections = stringResource(R.string.activity_type_connections),
            lookups = stringResource(R.string.activity_type_lookups),
        ),
        onBackClick = onBackClick,
        viewSwitch = viewSwitch,
        additionalOverflowItems = listOf(
            RethinkOverflowItem(stringResource(R.string.console_log_title), MaterialSymbols.Filled.Terminal, onClick = onAppLogsClick),
        ),
        content = content,
    )

    sheetTarget?.let { target ->
        LogConnectionSheet(
            target = target,
            eventLogger = eventLogger,
            navigation = sheetNavigation,
            loadFavIcon = persistentState.fetchFavIcon,
            onDismiss = { sheetTarget = null },
        )
    }
}

@Composable
private fun ActivityLogContent(
    filters: LogFilters,
    scope: LogScope,
    viewModel: ActivityLogViewModel,
    connections: ConnectionTrackerRepository,
    lookups: DnsLogRepository,
    persistentState: PersistentState,
    onOpenSheet: (LogSheetTarget) -> Unit,
    onToolbarActionsChange: (RethinkLogToolbarActions) -> Unit,
) {
    // Local mirror of the pref so the inline "Enable logs" action recomposes immediately.
    var logsEnabled by remember { mutableStateOf(persistentState.logsEnabled) }
    val items = viewModel.entries.collectAsLazyPagingItems()
    val dialogs = remember { LogDialogs() }

    val logQuery = filters.query(scope)
    LaunchedEffect(logQuery) { viewModel.setQuery(logQuery) }
    PublishToolbarActions(items, logsEnabled, dialogs, onToolbarActionsChange)

    // A picked app is a filter: back clears it first. An app the screen was opened for is not.
    BackHandler(enabled = filters.appUid != null && !dialogs.anyOpen) {
        filters.appUid = null
    }

    if (!logsEnabled) {
        LogsDisabled {
            persistentState.logsEnabled = true
            logsEnabled = true
        }
        return
    }

    ActivityLogPanes(
        items = items,
        filters = filters,
        emptyMessage = emptyLogMessage(filters.verdict, filters.searchLabel(scope)),
        onOpenFilters = { dialogs.filters = true },
        connections = connections,
        lookups = lookups,
        loadFavIcon = persistentState.fetchFavIcon,
        onOpenSheet = onOpenSheet,
    )
    ActivityLogDialogs(
        dialogs = dialogs,
        filters = filters,
        scope = scope,
        query = logQuery,
        viewModel = viewModel,
        onClear = {
            if (filters.type != RethinkLogType.Lookups) connections.clearAllData()
            if (filters.type != RethinkLogType.Connections) lookups.clearAllData()
        },
        onCleared = { items.refresh() },
    )
}

/** Hands the shell its Refresh and Clear: both only while logging, Clear only with entries. */
@Composable
private fun PublishToolbarActions(
    items: LazyPagingItems<ActivityLogEntry>,
    logsEnabled: Boolean,
    dialogs: LogDialogs,
    onToolbarActionsChange: (RethinkLogToolbarActions) -> Unit,
) {
    val refreshAction = remember(items) { { items.refresh() } }
    val clearAction = remember(dialogs) { { dialogs.clear = true } }
    LaunchedEffect(refreshAction, clearAction, logsEnabled, items.itemCount) {
        onToolbarActionsChange(
            RethinkLogToolbarActions(
                onRefresh = refreshAction.takeIf { logsEnabled },
                onClear = clearAction.takeIf { logsEnabled && items.itemCount > 0 },
            )
        )
    }
}

/** The empty list says what the filters ruled out, instead of a generic "no logs". */
@Composable
private fun emptyLogMessage(filter: RethinkLogFilter, search: String): String = when {
    search.isNotBlank() -> stringResource(R.string.log_empty_search, search)
    filter == RethinkLogFilter.Blocked -> stringResource(R.string.log_empty_blocked)
    filter == RethinkLogFilter.Allowed -> stringResource(R.string.log_empty_allowed)
    else -> stringResource(R.string.lbl_no_logs)
}

/** The one log-list string set: filter chips and paging states, shared by every log screen. */
@Composable
internal fun logListStrings() = RethinkLogListStrings(
    all = stringResource(R.string.lbl_all),
    allowed = stringResource(R.string.lbl_allowed),
    blocked = stringResource(R.string.lbl_blocked),
    loading = stringResource(R.string.lbl_loading),
    empty = stringResource(R.string.lbl_no_logs),
    error = stringResource(R.string.error_loading_log_file),
    retry = stringResource(R.string.cd_refresh),
)

/** Logging is off: explain why the list is empty and offer to turn it back on in place. */
@Composable
private fun LogsDisabled(onEnable: () -> Unit) {
    RethinkLogsDisabledState(
        message = stringResource(R.string.logs_disabled_message),
        title = stringResource(R.string.logs_disabled_title),
        actionLabel = stringResource(R.string.logs_enable_action),
        onEnable = onEnable,
    )
}
