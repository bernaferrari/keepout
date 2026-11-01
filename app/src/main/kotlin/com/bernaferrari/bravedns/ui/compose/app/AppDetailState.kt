/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bernaferrari.bravedns.data.AppConfig
import com.bernaferrari.bravedns.database.AppInfo
import com.bernaferrari.bravedns.service.FirewallManager
import com.bernaferrari.bravedns.service.FirewallManager.ConnectionStatus
import com.bernaferrari.bravedns.service.FirewallManager.FirewallStatus
import com.bernaferrari.bravedns.service.ProxyManager
import com.bernaferrari.bravedns.service.ProxyManager.ID_NONE
import com.bernaferrari.bravedns.service.WireguardManager

/**
 * What App detail shows for one uid: the app, its firewall flags, temporary access and proxy
 * bypass. Edits write here first (optimistically); [AppDetailEditor] persists them.
 */
@Stable
internal class AppDetailState {
    var loaded by mutableStateOf(false)
        private set
    var appInfo by mutableStateOf<AppInfo?>(null)
        private set
    var appStatus by mutableStateOf(FirewallStatus.NONE)
        private set
    var connStatus by mutableStateOf(ConnectionStatus.ALLOW)
        private set
    /** The network flags to restore when a mode goes back to Normal (modes clear them). */
    var baselineConnStatus by mutableStateOf(ConnectionStatus.ALLOW)
    var isProxyExcluded by mutableStateOf(false)
    var tempAllowExpiry by mutableLongStateOf(0L)
    var proxyDetails by mutableStateOf("")
        private set
    var anyProxySetUp by mutableStateOf(false)
        private set
    /** A write is in flight: the controls wait for it. */
    var busy by mutableStateOf(false)
    /** Bumped by every edit, so an older edit's Undo knows it was superseded. */
    var epoch by mutableIntStateOf(0)

    /** The app's label, or its package when it has none. */
    val appName: String
        get() = appInfo?.appName?.takeIf { it.isNotBlank() } ?: appInfo?.packageName.orEmpty()

    /** Take what [load] read; null means no such app (the flags keep their last values). */
    fun onLoaded(load: AppInfoLoad?) {
        appInfo = load?.info
        if (load != null) {
            show(load.appStatus, load.connStatus)
            isProxyExcluded = load.isProxyExcluded
            tempAllowExpiry = load.tempAllowExpiry
            proxyDetails = load.proxyDetails
            anyProxySetUp = load.anyProxySetUp
        }
        loaded = true
    }

    /** Show [status] and [connection]; in Normal mode the flags are also the new baseline. */
    fun show(status: FirewallStatus, connection: ConnectionStatus) {
        appStatus = status
        connStatus = connection
        if (status == FirewallStatus.NONE) baselineConnStatus = connection
    }
}

internal data class AppInfoLoad(
    val info: AppInfo,
    val appStatus: FirewallStatus,
    val connStatus: ConnectionStatus,
    val isProxyExcluded: Boolean,
    val tempAllowExpiry: Long,
    val proxyDetails: String,
    val anyProxySetUp: Boolean,
)

/** The app behind [uid], or null when it isn't installed (or was uninstalled). */
internal suspend fun loadAppInfo(uid: Int, wireguardAppsProxyMapDesc: String, appConfig: AppConfig): AppInfoLoad? {
    FirewallManager.ensureLoaded()
    val info = FirewallManager.getAppInfoByUid(uid)
    if (info == null || info.tombstoneTs > 0) return null
    val proxy = ProxyManager.getProxyIdForApp(uid).takeIf { it.isNotEmpty() && it != ID_NONE }
        ?.let { wireguardAppsProxyMapDesc.format(it) }
        .orEmpty()
    return AppInfoLoad(
        info = info,
        appStatus = FirewallManager.appStatus(info.uid),
        connStatus = FirewallManager.connectionStatus(info.uid),
        isProxyExcluded = info.isProxyExcluded,
        tempAllowExpiry = FirewallManager.tempAllowExpiry(info.uid),
        proxyDetails = proxy,
        anyProxySetUp = anyProxySetUp(appConfig),
    )
}

/**
 * Whether any proxy is set up or on, from the same reads as the Proxy & VPN screen: a WireGuard
 * tunnel exists, a SOCKS5 or HTTP server is saved, or a proxy (Orbot included) is on.
 */
private suspend fun anyProxySetUp(appConfig: AppConfig): Boolean =
    appConfig.isProxyEnabled() ||
        appConfig.isOrbotProxyEnabled() ||
        WireguardManager.getNumberOfMappings() > 0 ||
        !runCatching { appConfig.getSocks5ProxyDetails() }.getOrNull()?.proxyIP.isNullOrBlank() ||
        !runCatching { appConfig.getHttpProxyDetails() }.getOrNull()?.proxyIP.isNullOrBlank()
