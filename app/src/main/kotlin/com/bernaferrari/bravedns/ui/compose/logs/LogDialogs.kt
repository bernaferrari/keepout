/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.ActivityAppCount
import com.bernaferrari.bravedns.database.ActivityLogQuery
import com.bernaferrari.bravedns.service.FirewallRuleset
import com.bernaferrari.bravedns.ui.compose.common.AppIcon
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.viewmodel.ActivityLogViewModel

/**
 * Which of the log's dialogs are open, and the app picker's search and options. The picker's state
 * outlives the dialog, so reopening it keeps the search and shows the last options while reloading.
 */
@Stable
internal class LogDialogs {
    var filters by mutableStateOf(false)
    var reasons by mutableStateOf(false)
    var appFilter by mutableStateOf(false)
    var clear by mutableStateOf(false)

    var appQuery by mutableStateOf("")
    var appOptions by mutableStateOf<List<ActivityAppCount>>(emptyList())
        private set
    var appOptionsLoading by mutableStateOf(false)
        private set

    val anyOpen: Boolean get() = filters || reasons || appFilter || clear

    suspend fun loadAppOptions(load: suspend () -> List<ActivityAppCount>) {
        appOptionsLoading = true
        appOptions = load()
        appOptionsLoading = false
    }
}

/**
 * The log's Filters sheet and the Reason and App pickers it opens, plus Clear. The App picker lists
 * the apps behind the current type, verdict and reasons, reloaded while it is open.
 */
@Composable
internal fun ActivityLogDialogs(
    dialogs: LogDialogs,
    filters: LogFilters,
    scope: LogScope,
    query: ActivityLogQuery,
    viewModel: ActivityLogViewModel,
    onClear: suspend () -> Unit,
    onCleared: () -> Unit,
) {
    LaunchedEffect(dialogs.appFilter, query.kinds, query.verdict, query.rules) {
        if (dialogs.appFilter) dialogs.loadAppOptions { viewModel.loggedApps(query) }
    }
    val reasons = filters.reasonOptions()
    if (dialogs.filters) LogFiltersSheet(dialogs, filters, scope, hasReasons = reasons.isNotEmpty())
    if (dialogs.reasons && reasons.isNotEmpty()) LogReasonsDialog(reasons, filters, onDismiss = { dialogs.reasons = false })
    if (dialogs.appFilter) LogAppFilterDialog(dialogs, filters)
    ClearLogsDialog(
        show = dialogs.clear,
        type = filters.type,
        onDismiss = { dialogs.clear = false },
        onDelete = onClear,
        onRefresh = onCleared,
    )
}

/** Type, App and Reason, each summarised; App and Reason open their own pickers. */
@Composable
private fun LogFiltersSheet(dialogs: LogDialogs, filters: LogFilters, scope: LogScope, hasReasons: Boolean) {
    val anyReason = stringResource(R.string.activity_filter_any_reason)
    val reasonTitles = filters.rules.mapNotNull { id -> FirewallRuleset.getFirewallRule(id)?.title }.map { stringResource(it) }
    // The screen's own app is fixed: no App row then.
    val appSummary = if (scope.appUid != null) {
        null
    } else {
        filters.appName.takeIf { filters.appUid != null } ?: stringResource(R.string.activity_filter_all_apps)
    }
    RethinkLogFiltersSheet(
        type = filters.type,
        onType = filters::selectType,
        appSummary = appSummary,
        onApp = { dialogs.appFilter = true },
        reasonSummary = if (hasReasons) reasonTitles.joinToString().ifEmpty { anyReason } else null,
        onReason = { dialogs.reasons = true },
        strings = RethinkLogFiltersStrings(
            title = stringResource(R.string.activity_filters),
            type = stringResource(R.string.activity_filter_type),
            all = stringResource(R.string.activity_type_all),
            connections = stringResource(R.string.activity_type_connections),
            lookups = stringResource(R.string.activity_type_lookups),
            app = stringResource(R.string.log_filter_app),
            reason = stringResource(R.string.log_filter_reason),
        ),
        onDismiss = { dialogs.filters = false },
    )
}

@Composable
private fun LogReasonsDialog(reasons: List<FirewallRuleset>, filters: LogFilters, onDismiss: () -> Unit) {
    RethinkLogRulesDialog(
        rules = reasons.map { rule ->
            RethinkLogRuleOption(
                id = rule.id,
                title = stringResource(rule.title),
                supporting = rethinkHtmlToAnnotatedString(stringResource(rule.desc)),
                leadingIcon = { tint -> PlatformLogRuleIcon(rule.id, tint) },
            )
        },
        selectedIds = filters.rules.toSet(),
        strings = RethinkLogRulesStrings(
            title = stringResource(R.string.log_filter_reason),
            clear = stringResource(R.string.fapps_filter_clear_btn),
            dismissDescription = stringResource(R.string.lbl_dismiss),
        ),
        onToggle = filters::toggleRule,
        onClear = { filters.rules = emptyList() },
        onDismiss = onDismiss,
    )
}

@Composable
private fun LogAppFilterDialog(dialogs: LogDialogs, filters: LogFilters) {
    val options = dialogs.appOptions
    RethinkLogAppFilterDialog(
        options = options.toRethinkLogAppOptions(),
        selectedId = filters.appUid?.toString(),
        searchQuery = dialogs.appQuery,
        isLoading = dialogs.appOptionsLoading,
        strings = logAppFilterStrings(options.size),
        onSearchQueryChange = { dialogs.appQuery = it },
        onSelect = { picked ->
            val app = options.firstOrNull { it.uid.toString() == picked }
            filters.appUid = app?.uid
            filters.appName = app?.appName.orEmpty()
            dialogs.appFilter = false
        },
        onClearSelection = { filters.appUid = null },
        onDismiss = { dialogs.appFilter = false },
        appIcon = { option -> PlatformLogAppIcon(option) },
    )
}

@Composable
private fun logAppFilterStrings(appCount: Int) = RethinkLogAppFilterStrings(
    all = stringResource(R.string.lbl_all),
    searchPlaceholder = stringResource(R.string.search_apps_count_placeholder, appCount),
    clear = stringResource(R.string.fapps_filter_clear_btn),
    clearSearchDescription = stringResource(R.string.cd_clear_search),
    loading = stringResource(R.string.lbl_loading),
)

private fun List<ActivityAppCount>.toRethinkLogAppOptions() = map { app ->
    RethinkLogAppOption(
        id = app.uid.toString(),
        label = app.appName,
        count = app.count,
        iconKey = app.packageName,
    )
}

/** Filter options pair the app with a 24dp glyph-sized icon, like the "All" globe beside it. */
@Composable
private fun PlatformLogAppIcon(option: RethinkLogAppOption) {
    AppIcon(option.iconKey, appName = option.label, size = SharedDimensions.iconSizeMd)
}
