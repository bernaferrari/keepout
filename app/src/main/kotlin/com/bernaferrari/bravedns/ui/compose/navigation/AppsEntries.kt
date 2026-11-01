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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.ui.compose.app.AppInfoScreen
import com.bernaferrari.bravedns.ui.compose.app.KeepoutConnectionsScreen
import com.bernaferrari.bravedns.ui.compose.apps.AppListScreen
import com.bernaferrari.bravedns.ui.compose.common.RethinkAdaptiveWidth
import com.bernaferrari.bravedns.ui.compose.common.RethinkEmptyState
import com.bernaferrari.bravedns.ui.compose.common.isExpanded
import com.bernaferrari.bravedns.ui.compose.firewall.CustomRulesScreen
import com.bernaferrari.bravedns.ui.compose.firewall.LocalSelectedFirewallAppUid
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRulesMode
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRulesTab
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

/** The Apps tab: the app list (beside the selected app on wide windows), an app's detail and its rules. */
internal fun EntryProviderScope<NavKey>.appsEntries(
    nav: RethinkNavOps,
    eventLogger: EventLogger,
    apps: HomeAppsDependencies,
) {
    entry<RethinkRoute.AppList> {
        // The Apps tab's root: no back arrow; back goes Home like the other tabs.
        BackHandler(enabled = nav.current() is RethinkRoute.AppList) {
            nav.popBackStack()
        }
        AppListWithDetail(
            appList = { onAppClick ->
                AppListScreen(
                    viewModel = apps.appInfoViewModel,
                    eventLogger = eventLogger,
                    refreshDatabase = apps.refreshDatabase,
                    onAppClick = onAppClick,
                    onBackClick = null,
                )
            },
            appInfo = { uid, onBack -> AppInfoEntry(uid, nav, eventLogger, apps, onBack) },
            onOpenApp = { uid -> nav.push(RethinkRoute.AppInfo(uid)) },
        )
    }
    entry<RethinkRoute.AppInfo> { args ->
        AppInfoEntry(args.uid, nav, eventLogger, apps, onBack = { nav.popBackStack() })
    }
    entry<RethinkRoute.KeepoutConnections> {
        KeepoutConnectionsScreen(viewModel = apps.connectionsViewModel, onBackClick = { nav.popBackStack() })
    }
    entry<RethinkRoute.CustomRules> { args ->
        CustomRulesScreen(
            uid = args.uid,
            initialTab = RethinkRulesTab.fromValue(args.tab),
            initialMode = RethinkRulesMode.fromValue(args.mode),
            domainViewModel = apps.domainRulesViewModel,
            ipViewModel = apps.ipRulesViewModel,
            eventLogger = eventLogger,
            initialQuery = args.query,
            onBackClick = { nav.popBackStack() }
        )
    }
}

/** One app's detail, keyed by uid so switching apps in the side pane starts fresh. */
@Composable
private fun AppInfoEntry(
    uid: Int,
    nav: RethinkNavOps,
    eventLogger: EventLogger,
    apps: HomeAppsDependencies,
    onBack: () -> Unit,
) {
    key(uid) {
        AppInfoScreen(
            uid = uid,
            eventLogger = eventLogger,
            ipRulesViewModel = apps.ipRulesViewModel,
            domainRulesViewModel = apps.domainRulesViewModel,
            networkLogsViewModel = apps.connectionsViewModel,
            onBackClick = onBack,
            onSeeAllActivity = { appName -> nav.open(RethinkRoute.NetworkLogs(uid = uid, appName = appName)) },
            onSeeKeepoutConnections = { nav.push(RethinkRoute.KeepoutConnections) },
            connectionSheetNavigation = connectionSheetNavigation(nav),
            onCustomIpRulesClick = { appUid -> nav.push(customRulesRoute(appUid, CustomRulesTab.IP)) },
            onCustomDomainRulesClick = { appUid -> nav.push(customRulesRoute(appUid, CustomRulesTab.DOMAIN)) },
        )
    }
}

/**
 * Wide windows: the app list beside the selected app's rules (list–detail). Phones and 7-inch
 * tablets push the rules as their own screen.
 */
@Composable
private fun AppListWithDetail(
    appList: @Composable (onAppClick: (Int) -> Unit) -> Unit,
    appInfo: @Composable (uid: Int, onBack: () -> Unit) -> Unit,
    onOpenApp: (Int) -> Unit,
) {
    var selectedUid by rememberSaveable { mutableStateOf<Int?>(null) }
    RethinkAdaptiveWidth(Modifier.fillMaxSize()) { widthClass ->
        val twoPane = widthClass.isExpanded
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                CompositionLocalProvider(LocalSelectedFirewallAppUid provides selectedUid.takeIf { twoPane }) {
                    appList { uid -> if (twoPane) selectedUid = uid else onOpenApp(uid) }
                }
            }
            if (twoPane) {
                Box(Modifier.weight(1.15f).fillMaxHeight()) {
                    val uid = selectedUid
                    if (uid == null) {
                        RethinkEmptyState(
                            icon = MaterialSymbols.Filled.Apps,
                            title = stringResource(R.string.app_pane_empty_title),
                            message = stringResource(R.string.app_pane_empty_message),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        appInfo(uid) { selectedUid = null }
                    }
                }
            }
        }
    }
}
