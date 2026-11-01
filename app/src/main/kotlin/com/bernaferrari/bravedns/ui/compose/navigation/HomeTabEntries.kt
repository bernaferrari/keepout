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

import androidx.compose.runtime.State
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.ui.compose.alerts.AlertsScreen
import com.bernaferrari.bravedns.ui.compose.home.HomeProtectionActions
import com.bernaferrari.bravedns.ui.compose.home.HomeScreen
import com.bernaferrari.bravedns.ui.compose.home.HomeScreenUiState
import com.bernaferrari.bravedns.ui.compose.home.WelcomeScreen

/**
 * The Home tab: the protection hero, first-run Welcome and Alerts.
 *
 * [uiState] is read when Home composes, so the entry always shows the latest state even though
 * the entry itself was built earlier.
 */
internal fun EntryProviderScope<NavKey>.homeTabEntries(
    nav: RethinkNavOps,
    uiState: State<HomeScreenUiState>,
    protection: HomeProtectionActions,
    persistentState: PersistentState,
) {
    entry<RethinkRoute.Home> {
        HomeScreen(
            uiState = uiState.value,
            actions = protection,
            onActivityClick = { nav.open(RethinkRoute.Statistics) },
            onDnsClick = { nav.push(RethinkRoute.DnsSettings) },
            onAppClick = { uid -> nav.push(RethinkRoute.AppInfo(uid)) },
            onReviewApps = { nav.open(RethinkRoute.AppList) },
            onChooseBlocklists = { nav.push(RethinkRoute.Blocklists) },
        )
    }
    entry<RethinkRoute.Welcome> {
        WelcomeScreen(
            onTurnOn = {
                persistentState.firstTimeLaunch = false
                nav.open(RethinkRoute.Home)
                protection.onTurnOnFromWelcome()
            },
            onNotNow = {
                persistentState.firstTimeLaunch = false
                nav.open(RethinkRoute.Home)
            },
        )
    }
    entry<RethinkRoute.Alerts> {
        AlertsScreen(onBackClick = { nav.popBackStack() })
    }
}
