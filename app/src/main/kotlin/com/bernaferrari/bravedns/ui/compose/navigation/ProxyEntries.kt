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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bernaferrari.bravedns.ui.compose.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.bravedns.service.ProxyManager
import com.bernaferrari.bravedns.ui.compose.proxy.TcpProxyMainScreen
import com.bernaferrari.bravedns.ui.compose.settings.ProxySettingsScreen
import com.bernaferrari.bravedns.ui.compose.wireguard.WgConfigDetailScreen
import com.bernaferrari.bravedns.ui.compose.wireguard.WgConfigEditorScreen
import com.bernaferrari.bravedns.ui.compose.wireguard.WgIncludeAppsScreen
import com.bernaferrari.bravedns.ui.compose.wireguard.WgMainScreen
import com.bernaferrari.bravedns.ui.compose.wireguard.WgType

/** Proxies: the proxy settings hub, Orbot's app picker, the TCP proxy and WireGuard. */
internal fun EntryProviderScope<NavKey>.proxyEntries(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    proxy: HomeProxyDependencies,
) {
    entry<RethinkRoute.ProxySettings> { args ->
        ProxySettingsScreen(
            appConfig = core.appConfig,
            persistentState = core.persistentState,
            eventLogger = core.eventLogger,
            mappingViewModel = proxy.mappingViewModel,
            initialFocusKey = args.focusKey.asFocusKey(),
            onWireguardClick = { nav.push(RethinkRoute.WireGuard) },
            onOpenOrbotApps = { nav.push(RethinkRoute.OrbotAppSelect) },
            onNavigateToDns = { nav.push(RethinkRoute.DnsDetail()) },
            onBackClick = { nav.popBackStack() }
        )
    }
    entry<RethinkRoute.OrbotAppSelect> {
        WgIncludeAppsScreen(
            viewModel = proxy.mappingViewModel,
            proxyId = ProxyManager.ID_ORBOT_BASE,
            proxyName = ProxyManager.ORBOT_PROXY_NAME,
            onDismiss = { nav.popBackStack() }
        )
    }
    entry<RethinkRoute.TcpProxy> {
        TcpProxyMainScreen(
            appConfig = core.appConfig,
            mappingViewModel = proxy.mappingViewModel,
            onBackClick = { nav.popBackStack() }
        )
    }
    wireGuardEntries(nav, core, proxy)
}

private fun EntryProviderScope<NavKey>.wireGuardEntries(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    proxy: HomeProxyDependencies,
) {
    entry<RethinkRoute.WireGuard> {
        WgMainScreen(
            wgConfigViewModel = proxy.wgConfigViewModel,
            persistentState = core.persistentState,
            appConfig = core.appConfig,
            eventLogger = core.eventLogger,
            onBackClick = { nav.popBackStack() },
            onCreateClick = proxy.wgActions.onWgCreateClick,
            onImportClick = proxy.wgActions.onWgImportClick,
            onQrScanClick = proxy.wgActions.onWgQrScanClick,
            onConfigDetailClick = { configId, wgType -> nav.push(wireGuardDetailRoute(configId, wgType)) }
        )
    }
    entry<RethinkRoute.WireGuardDetail> { args ->
        WgConfigDetailScreen(
            configId = args.configId,
            wgType = WgType.fromInt(args.wgType),
            persistentState = core.persistentState,
            eventLogger = core.eventLogger,
            mappingViewModel = proxy.mappingViewModel,
            onEditConfig = { id, type -> nav.push(wireGuardEditorRoute(id, type)) },
            onBackClick = { nav.popBackStack() }
        )
    }
    entry<RethinkRoute.WireGuardEditor> { args ->
        WgConfigEditorScreen(
            configId = args.configId,
            wgType = WgType.fromInt(args.wgType),
            persistentState = core.persistentState,
            onBackClick = { nav.popBackStack() },
            onSaveSuccess = { nav.popBackStack() }
        )
    }
}
