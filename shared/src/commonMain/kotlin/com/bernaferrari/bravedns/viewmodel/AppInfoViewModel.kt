package com.bernaferrari.bravedns.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.bravedns.database.AppInfo
import com.bernaferrari.bravedns.database.AppInfoDAO
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkAppSelectionAction
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallFilter
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallFilters
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallTopLevelFilter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Provided

/** One app's firewall rule before a Select-mode change, for Undo. */
data class AppRuleSnapshot(val uid: Int, val firewall: AppFirewallStatus, val connection: AppConnectionStatus)

/** One row's Wi‑Fi or mobile data tap: the rule it replaced and the rule it wrote. */
data class AppRuleChange(val before: AppRuleSnapshot, val after: AppRuleSnapshot)

/** Shared filtering and Select-mode rule changes for the app firewall list. */
@OptIn(FlowPreview::class)
@KoinViewModel
class AppInfoViewModel(
    @Provided private val appInfoDAO: AppInfoDAO,
    @Provided private val firewallRules: AppFirewallRuleMutator,
) : ViewModel() {

    private val defaultFilters = RethinkFirewallFilters(topLevel = RethinkFirewallTopLevelFilter.Installed)
    private val baseFilters = MutableStateFlow(defaultFilters.copy(query = ""))
    private val searchInput = MutableStateFlow(defaultFilters.query)
    private val bulkUpdateMutex = Mutex()

    private val effectiveFilters: StateFlow<RethinkFirewallFilters> =
        combine(baseFilters, searchInput.debounce(300).distinctUntilChanged()) { base, debouncedSearch ->
            base.copy(query = debouncedSearch.trim())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultFilters)

    /** The filtered, sorted apps; null until the first load completes (never an "empty" flash). */
    val appInfo: StateFlow<List<AppInfo>?> =
        combine(appInfoDAO.getAllAppDetailsFlow(), effectiveFilters) { apps, filters ->
            filterAndSortApps(apps, filters)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFilter(filters: RethinkFirewallFilters) {
        baseFilters.value = filters.copy(query = "")
        searchInput.value = filters.query
    }

    /**
     * Apply [action] to exactly [uids] (Select mode), never to whatever the filter shows. Returns
     * each app's previous rule so [restore] can undo it. Network actions keep the other network's
     * flag and return the app to Normal handling; mode actions clear both network flags, as the
     * per-app controls always have.
     */
    suspend fun applyToSelected(uids: Set<Int>, action: RethinkAppSelectionAction): List<AppRuleSnapshot> =
        bulkUpdateMutex.withLock {
            uids.mapNotNull { uid ->
                val before = snapshot(uid)
                val next = selectionRule(action, before)
                if (next.firewall == before.firewall && next.connection == before.connection) return@mapNotNull null
                firewallRules.updateFirewallStatus(uid, next.firewall, next.connection)
                before
            }
        }

    /** Put back the rules [applyToSelected] replaced. */
    suspend fun restore(snapshots: List<AppRuleSnapshot>) = bulkUpdateMutex.withLock {
        snapshots.forEach { firewallRules.updateFirewallStatus(it.uid, it.firewall, it.connection) }
    }

    /**
     * A row's Wi‑Fi or mobile data tap updates one network without changing handling mode.
     */
    suspend fun toggleNetwork(uid: Int, wifi: Boolean): AppRuleChange = bulkUpdateMutex.withLock {
        val before = snapshot(uid)
        if (before.firewall == AppFirewallStatus.Exclude) return@withLock AppRuleChange(before, before)
        val connection = if (wifi) {
            wifiState(blocked = !before.connection.blocksWifi, before.connection)
        } else {
            mobileState(blocked = !before.connection.blocksMobile, before.connection)
        }
        val status = before.networkEditStatus()
        firewallRules.updateFirewallStatus(uid, status, connection)
        AppRuleChange(before, AppRuleSnapshot(uid, status, connection))
    }

    private suspend fun snapshot(uid: Int) =
        AppRuleSnapshot(uid, firewallRules.firewallStatus(uid), firewallRules.connectionStatus(uid))

    suspend fun getAppCount(): Int = appInfoDAO.getAppCount()

    private fun filterAndSortApps(
        apps: List<AppInfo>,
        filters: RethinkFirewallFilters,
    ): List<AppInfo> =
        apps.asSequence()
            .filter { matchesTopLevelFilter(it, filters.topLevel) }
            .filter { filters.categories.isEmpty() || it.appCategory in filters.categories }
            .filter { matchesFirewallFilter(it, filters.status) }
            .filter { matchesSearch(it, filters.query) }
            .sortedWith(
                compareBy<AppInfo>(
                    { it.appName.ifBlank { it.packageName }.lowercase() },
                    { it.packageName.lowercase() },
                    { it.uid },
                )
            )
            .toList()

    private fun matchesTopLevelFilter(app: AppInfo, filter: RethinkFirewallTopLevelFilter): Boolean =
        when (filter) {
            RethinkFirewallTopLevelFilter.All -> true
            RethinkFirewallTopLevelFilter.Installed -> !app.isSystemApp
            RethinkFirewallTopLevelFilter.System -> app.isSystemApp
        }

    private fun matchesFirewallFilter(app: AppInfo, filter: RethinkFirewallFilter): Boolean {
        val status = app.firewallStatus
        val connection = app.connectionStatus
        val inTunnel = status != AppFirewallStatus.Exclude.id
        val regularMatch =
            when (filter) {
                RethinkFirewallFilter.All -> true
                RethinkFirewallFilter.Allowed ->
                    status == AppFirewallStatus.None.id && connection == AppConnectionStatus.Allow.id
                RethinkFirewallFilter.Blocked ->
                    inTunnel && connection in BLOCKED_CONNECTION_IDS
                RethinkFirewallFilter.BlockedWifi ->
                    inTunnel && connection in setOf(AppConnectionStatus.Unmetered.id, AppConnectionStatus.Both.id)
                RethinkFirewallFilter.BlockedMobile ->
                    inTunnel && connection in setOf(AppConnectionStatus.Metered.id, AppConnectionStatus.Both.id)
                RethinkFirewallFilter.Bypass -> status == AppFirewallStatus.BypassUniversal.id
                RethinkFirewallFilter.SkipAllFiltering -> status == AppFirewallStatus.BypassDnsFirewall.id
                RethinkFirewallFilter.Excluded -> status == AppFirewallStatus.Exclude.id
                RethinkFirewallFilter.Lockdown -> status == AppFirewallStatus.Isolate.id
            }
        return regularMatch || (filter == RethinkFirewallFilter.Bypass && app.isProxyExcluded)
    }

    private fun matchesSearch(app: AppInfo, query: String): Boolean =
        query.isBlank() || app.appName.contains(query.trim(), ignoreCase = true)

    private fun AppRuleSnapshot.networkEditStatus() =
        if (firewall == AppFirewallStatus.Untracked) AppFirewallStatus.None else firewall

    private fun selectionRule(action: RethinkAppSelectionAction, before: AppRuleSnapshot): AppRuleState {
        val connection = before.connection
        val mode = before.networkEditStatus()
        return when (action) {
            RethinkAppSelectionAction.BlockWifi -> before.withNetworkRule(mode, wifiState(blocked = true, connection))
            RethinkAppSelectionAction.BlockMobile -> before.withNetworkRule(mode, mobileState(blocked = true, connection))
            RethinkAppSelectionAction.AllowWifi -> before.withNetworkRule(mode, wifiState(blocked = false, connection))
            RethinkAppSelectionAction.AllowMobile -> before.withNetworkRule(mode, mobileState(blocked = false, connection))
            RethinkAppSelectionAction.AllowAll -> AppRuleState(AppFirewallStatus.None, AppConnectionStatus.Allow)
            RethinkAppSelectionAction.TrustedOnly -> AppRuleState(AppFirewallStatus.Isolate, connection)
            RethinkAppSelectionAction.SkipUniversalRules -> AppRuleState(AppFirewallStatus.BypassUniversal, connection)
            RethinkAppSelectionAction.SkipAllFiltering -> AppRuleState(AppFirewallStatus.BypassDnsFirewall, connection)
            RethinkAppSelectionAction.Exclude -> AppRuleState(AppFirewallStatus.Exclude, connection)
        }
    }

    private fun AppRuleSnapshot.withNetworkRule(mode: AppFirewallStatus, updated: AppConnectionStatus): AppRuleState =
        AppRuleState(mode, if (firewall == AppFirewallStatus.Exclude) connection else updated)

    private fun wifiState(blocked: Boolean, connection: AppConnectionStatus): AppConnectionStatus = when {
        blocked && connection.blocksMobile -> AppConnectionStatus.Both
        blocked -> AppConnectionStatus.Unmetered
        connection.blocksMobile -> AppConnectionStatus.Metered
        else -> AppConnectionStatus.Allow
    }

    private fun mobileState(blocked: Boolean, connection: AppConnectionStatus): AppConnectionStatus = when {
        blocked && connection.blocksWifi -> AppConnectionStatus.Both
        blocked -> AppConnectionStatus.Metered
        connection.blocksWifi -> AppConnectionStatus.Unmetered
        else -> AppConnectionStatus.Allow
    }

    private val AppConnectionStatus.blocksWifi get() = this == AppConnectionStatus.Unmetered || this == AppConnectionStatus.Both
    private val AppConnectionStatus.blocksMobile get() = this == AppConnectionStatus.Metered || this == AppConnectionStatus.Both

    private data class AppRuleState(
        val firewall: AppFirewallStatus,
        val connection: AppConnectionStatus,
    )

    private companion object {
        val BLOCKED_CONNECTION_IDS = setOf(
            AppConnectionStatus.Unmetered.id,
            AppConnectionStatus.Metered.id,
            AppConnectionStatus.Both.id,
        )
    }
}
