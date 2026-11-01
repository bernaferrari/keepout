/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.apps

import androidx.compose.ui.platform.LocalResources
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkAppSort
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.AppInfo
import com.bernaferrari.bravedns.database.EventSource
import com.bernaferrari.bravedns.database.EventType
import com.bernaferrari.bravedns.database.RefreshDatabase
import com.bernaferrari.bravedns.database.Severity
import com.bernaferrari.bravedns.database.hasInternetPermission
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.FirewallManager
import com.bernaferrari.bravedns.service.FirewallManager.ConnectionStatus
import com.bernaferrari.bravedns.service.FirewallManager.FirewallStatus
import com.bernaferrari.bravedns.service.ProxyManager
import com.bernaferrari.bravedns.ui.compose.app.mobileBlocked
import com.bernaferrari.bravedns.ui.compose.app.wifiBlocked
import com.bernaferrari.bravedns.ui.compose.common.AppIcon
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallApp
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallAppListActions
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallAppListScreen
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallAppStatus
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkAppSelectionAction
import com.bernaferrari.bravedns.ui.compose.firewall.showRuleUndoSnackbar
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallFilters
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallTopLevelFilter
import com.bernaferrari.bravedns.ui.compose.firewall.rethinkUsageText
import com.bernaferrari.bravedns.ui.compose.theme.RethinkConfirmDialog
import com.bernaferrari.bravedns.util.Utilities
import com.bernaferrari.bravedns.util.formatByteCount
import com.bernaferrari.bravedns.viewmodel.AppInfoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun defaultAppFilters() =
    RethinkFirewallFilters(topLevel = RethinkFirewallTopLevelFilter.Installed)

/** Android data/service adapter for the portable firewall app-list renderer. */
@Composable
fun AppListScreen(
    viewModel: AppInfoViewModel,
    eventLogger: EventLogger,
    refreshDatabase: RefreshDatabase,
    onAppClick: ((Int) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = org.koin.compose.koinInject<com.bernaferrari.bravedns.service.PersistentState>()
    var sortOrder by rememberSaveable {
        mutableStateOf(RethinkAppSort.entries.firstOrNull { it.name == preferences.appListSortName } ?: RethinkAppSort.Name)
    }
    val refreshCompleteText = stringResource(R.string.refresh_complete)
    var queryText by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentFilters by remember { mutableStateOf(defaultAppFilters()) }
    val snackbar = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.lbl_undo)
    val resources = LocalResources.current
    var pendingToggle by remember { mutableStateOf<AndroidFirewallToggleRequest?>(null) }

    fun applyFilters(filters: RethinkFirewallFilters) {
        currentFilters = filters
        queryText = filters.query
        viewModel.setFilter(filters)
    }

    /** Select mode: change exactly [apps]' UIDs, then offer Undo back to each app's own rule. */
    fun applySelection(action: RethinkAppSelectionAction, apps: List<RethinkFirewallApp>) {
        val uids = apps.map { it.uid }.toSet()
        if (uids.isEmpty()) return
        scope.launch {
            val previous = withContext(Dispatchers.IO + NonCancellable) { viewModel.applyToSelected(uids, action) }
            eventLogger.log(
                EventType.FW_RULE_MODIFIED,
                Severity.LOW,
                "App list, selected apps",
                EventSource.UI,
                true,
                "Selection ${action.name} for UIDs ${uids.joinToString()}",
            )
            val message = resources.getQuantityString(R.plurals.apps_bulk_done, uids.size, uids.size)
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showRuleUndoSnackbar(message, undoLabel)) {
                withContext(Dispatchers.IO + NonCancellable) { viewModel.restore(previous) }
                eventLogger.log(
                    EventType.FW_RULE_MODIFIED,
                    Severity.LOW,
                    "App list, selected apps",
                    EventSource.UI,
                    true,
                    "Undo selection ${action.name} for UIDs ${uids.joinToString()}",
                )
            }
        }
    }

    fun refreshAppList(action: Int = RefreshDatabase.ACTION_REFRESH_INTERACTIVE, showToast: Boolean = true) {
        if (isRefreshing) return
        isRefreshing = true
        scope.launch(Dispatchers.IO) {
            refreshDatabase.refresh(action) {
                scope.launch(Dispatchers.Main) {
                    isRefreshing = false
                    if (showToast) Utilities.showToastUiCentered(context, refreshCompleteText, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        applyFilters(defaultAppFilters())
        if (withContext(Dispatchers.IO) { viewModel.getAppCount() } == 0) {
            refreshAppList(RefreshDatabase.ACTION_REFRESH_FORCE, showToast = false)
        }
    }

    val loadedApps by viewModel.appInfo.collectAsState()
    // Null until the first load: the list shows loading, not "No apps found" / "Search 0 apps".
    val portableApps = remember(loadedApps, context) {
        loadedApps?.map { it.toRethinkFirewallApp(context) }
    }
    val haptics = LocalHapticFeedback.current

    /** One row's Wi‑Fi / mobile data tap: write it now. No Undo: tapping the switch again is the undo. */
    fun commitConnectionToggle(app: RethinkFirewallApp, wifi: Boolean) {
        // As App detail's tiles: the toggle haptic follows the state being left.
        val wasBlocked = if (wifi) app.wifiBlocked else app.mobileBlocked
        haptics.performHapticFeedback(if (wasBlocked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
        scope.launch {
            val change = withContext(Dispatchers.IO + NonCancellable) { viewModel.toggleNetwork(app.uid, wifi) }
            logToggle(eventLogger, app, change.after.connection.name)
            snackbar.currentSnackbarData?.dismiss()
        }
    }

    fun requestConnectionToggle(app: RethinkFirewallApp, wifi: Boolean) {
        scope.launch {
            val packages = withContext(Dispatchers.IO) { FirewallManager.getAppNamesByUid(app.uid) }
            if (packages.size > 1) {
                pendingToggle = AndroidFirewallToggleRequest(app, wifi, packages)
            } else {
                commitConnectionToggle(app, wifi)
            }
        }
    }

    RethinkFirewallAppListScreen(
        filterState = com.bernaferrari.bravedns.ui.compose.firewall.RethinkAppListFilterState(queryText, currentFilters.status, currentFilters),
        sorting = com.bernaferrari.bravedns.ui.compose.firewall.RethinkAppSorting(sortOrder) { sortOrder = it; preferences.appListSortName = it.name },
        apps = portableApps,
        strings = androidFirewallAppListStrings(),
        isRefreshing = isRefreshing,
        actions = RethinkFirewallAppListActions(
            onQueryChange = { query -> applyFilters(currentFilters.copy(query = query)) },
            onRefresh = { refreshAppList() },
            onFiltersChange = ::applyFilters,
            onQuickFilterChange = { filter -> applyFilters(currentFilters.copy(status = filter)) },
            onSelectionAction = ::applySelection,
            loadCategories = { topLevel ->
                withContext(Dispatchers.IO) {
                    when (topLevel) {
                        RethinkFirewallTopLevelFilter.Installed -> FirewallManager.getCategoriesForInstalledApps()
                        RethinkFirewallTopLevelFilter.System -> FirewallManager.getCategoriesForSystemApps()
                        RethinkFirewallTopLevelFilter.All -> FirewallManager.getAllCategories()
                    }
                }
            },
            onAppClick = { app -> onAppClick?.invoke(app.uid) },
            onWifiToggle = { app -> requestConnectionToggle(app, wifi = true) },
            onMobileToggle = { app -> requestConnectionToggle(app, wifi = false) },
            loadAlsoAffects = { apps ->
                // A rule is per UID: name the other packages that share a selected UID.
                val selectedNames = apps.map { it.appName }.toSet()
                withContext(Dispatchers.IO) {
                    apps.map { it.uid }.distinct().flatMap { FirewallManager.getAppNamesByUid(it) }
                }.filterNot { it in selectedNames }.distinct()
            },
        ),
        appIcon = { app -> AppIcon(app.packageName, appName = app.appName) },
        onBackClick = onBackClick,
        snackbarHostState = snackbar,
        selectionBackHandler = { enabled, onBack -> BackHandler(enabled = enabled, onBack = onBack) },
    )

    pendingToggle?.let { request ->
        RethinkConfirmDialog(
            onDismissRequest = { pendingToggle = null },
            title = stringResource(R.string.ctbs_block_other_apps, request.app.appName, request.packages.size.toString()),
            confirmText = stringResource(R.string.lbl_proceed),
            dismissText = stringResource(R.string.ctbs_dialog_negative_btn),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingXs)) {
                    request.packages.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            onConfirm = {
                pendingToggle = null
                commitConnectionToggle(request.app, request.wifi)
            },
            onDismiss = { pendingToggle = null },
        )
    }
}

private data class AndroidFirewallToggleRequest(
    val app: RethinkFirewallApp,
    val wifi: Boolean,
    val packages: List<String>,
)

private fun AppInfo.toRethinkFirewallApp(context: Context): RethinkFirewallApp {
    val appStatus = FirewallStatus.getStatus(firewallStatus)
    val connection = ConnectionStatus.getStatus(connectionStatus)
    // The network flags only apply in Normal mode; any other mode decides on its own.
    val normalMode = appStatus == FirewallStatus.NONE
    return RethinkFirewallApp(
        id = "$uid:$packageName",
        uid = uid,
        appName = appName,
        packageName = packageName,
        status = firewallAppStatus(appStatus, connection),
        wifiBlocked = normalMode && connection.wifiBlocked,
        mobileBlocked = normalMode && connection.mobileBlocked,
        dataUsage = dataUsageText(this),
        proxyEnabled = !isProxyExcluded && hasProxy(uid),
        hasInternetPermission = hasInternetPermission(context.packageManager),
        tombstoned = tombstoneTs > 0,
        canToggleConnections = packageName != context.packageName,
    )
}

private fun firewallAppStatus(status: FirewallStatus, connection: ConnectionStatus): RethinkFirewallAppStatus = when (status) {
    FirewallStatus.NONE -> if (connection == ConnectionStatus.ALLOW) RethinkFirewallAppStatus.Allowed else RethinkFirewallAppStatus.Blocked
    FirewallStatus.BYPASS_UNIVERSAL -> RethinkFirewallAppStatus.Bypass
    FirewallStatus.BYPASS_DNS_FIREWALL -> RethinkFirewallAppStatus.SkipAllFiltering
    FirewallStatus.EXCLUDE -> RethinkFirewallAppStatus.Excluded
    FirewallStatus.ISOLATE -> RethinkFirewallAppStatus.Lockdown
    FirewallStatus.UNTRACKED -> RethinkFirewallAppStatus.Unknown
}

/** Whether [uid] is routed through a proxy (WireGuard or another) rather than none. */
private fun hasProxy(uid: Int): Boolean =
    ProxyManager.getProxyIdForApp(uid).let { it.isNotEmpty() && it != ProxyManager.ID_NONE }

private fun dataUsageText(app: AppInfo): String? {
    if (app.uploadBytes <= 0L && app.downloadBytes <= 0L) return null
    return rethinkUsageText(
        download = formatByteCount(app.downloadBytes),
        upload = formatByteCount(app.uploadBytes),
    )
}

private fun logToggle(eventLogger: EventLogger, app: RethinkFirewallApp, connection: String, undo: Boolean = false) {
    eventLogger.log(
        EventType.FW_RULE_MODIFIED,
        Severity.LOW,
        "App list, rule change",
        EventSource.UI,
        true,
        "${if (undo) "Undo: " else ""}UID: ${app.uid}, App: ${app.appName}, New FW status: $connection",
    )
}
