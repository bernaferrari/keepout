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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.ui.compose.about.AboutUiState
import com.bernaferrari.bravedns.ui.compose.home.HomeScreenUiState

/**
 * The app's navigation host: the four tabs and every screen they reach.
 *
 * Routes are registered per feature (`*Entries.kt`), each fed only its slice of [dependencies].
 * [homeUiState] and [aboutUiState] change while composed; everything else is fixed for the
 * Activity's lifetime.
 *
 * Intents and notifications arrive as [homeNavRequest]. The host pushes when the request shares
 * the current tab's root and only replaces the stack on a different-root / cold-start open;
 * [onHomeNavConsumed] fires once it has been applied.
 */
@Composable
fun HomeScreenRoot(
    homeUiState: HomeScreenUiState,
    aboutUiState: AboutUiState,
    dependencies: HomeScreenDependencies,
    startDestination: RethinkRoute,
    homeNavRequest: RethinkRoute?,
    onHomeNavConsumed: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val latestHomeUiState = rememberUpdatedState(homeUiState)
    val latestAboutUiState = rememberUpdatedState(aboutUiState)
    // The Activity tab's view (Log or Insights) is host state: Stats links and intents select
    // Insights without a second root key ([RethinkRoute.activityView]).
    val activityView = rememberSaveable { mutableStateOf(RethinkActivityView.Log) }

    var pendingRoute by remember { mutableStateOf<RethinkRoute?>(null) }
    LaunchedEffect(homeNavRequest) {
        homeNavRequest?.activityView()?.let { activityView.value = it }
        pendingRoute = homeNavRequest?.forAndroid()
    }

    RethinkAppNavigation(
        destinations = homeNavigationItems(),
        startRoute = startDestination,
        pendingRoute = pendingRoute,
        onRouteNavigated = {
            pendingRoute = null
            onHomeNavConsumed()
        },
        modifier = Modifier.fillMaxSize(),
        snackbar = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        entryBuilder = { hostNav ->
            val nav = androidNavOps(hostNav, activityView)
            val core = dependencies.core
            homeTabEntries(nav, latestHomeUiState, dependencies.protection, core.persistentState)
            appsEntries(nav, core.eventLogger, dependencies.apps)
            activityEntries(nav, activityView, core, dependencies.activity)
            settingsEntries(nav, core, dependencies.settings, latestAboutUiState)
            diagnosticsEntries(nav, core, dependencies.diagnostics)
            dnsEntries(nav, core, dependencies.dns)
            proxyEntries(nav, core, dependencies.proxy)
            rpnEntries(
                nav,
                core.persistentState,
                dependencies.rpn,
                onNavigateToProxy = dependencies.settings.configureActions.onNavigateToProxy,
            )
        },
    )
}

@Composable
private fun homeNavigationItems(): List<RethinkNavigationItem> = HomeDestination.entries.map { destination ->
    RethinkNavigationItem(
        id = destination.root.name,
        label = stringResource(destination.labelRes),
        selectedIcon = destination.selectedIcon,
        unselectedIcon = destination.unselectedIcon,
    )
}

/**
 * The host's stack ops, adapted for Android: shared keys map to Android's screens
 * ([forAndroid]), and a request that names an Activity view selects it on the way.
 */
private fun androidNavOps(host: RethinkNavOps, activityView: MutableState<RethinkActivityView>) = RethinkNavOps(
    pop = { host.popBackStack() },
    pushRoute = { route ->
        route.activityView()?.let { activityView.value = it }
        host.push(route.forAndroid())
    },
    openRoute = { route ->
        route.activityView()?.let { activityView.value = it }
        host.open(route.forAndroid())
    },
    selectRootTab = { host.selectRoot(it) },
    current = host.current,
)
