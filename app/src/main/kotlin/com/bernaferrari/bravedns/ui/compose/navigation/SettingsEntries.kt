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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.bravedns.ui.compose.about.AboutScreen
import com.bernaferrari.bravedns.ui.compose.about.AboutUiState
import com.bernaferrari.bravedns.ui.compose.configure.ConfigureScreen
import com.bernaferrari.bravedns.ui.compose.configure.SettingsSearchDestination
import com.bernaferrari.bravedns.ui.compose.configure.route
import com.bernaferrari.bravedns.ui.compose.database.DatabaseScreen
import com.bernaferrari.bravedns.ui.compose.events.EventsScreen
import com.bernaferrari.bravedns.ui.compose.firewall.FirewallSettingsScreen
import com.bernaferrari.bravedns.ui.compose.settings.AntiCensorshipScreen
import com.bernaferrari.bravedns.ui.compose.settings.AppLockScreen
import com.bernaferrari.bravedns.ui.compose.settings.AppearanceSettingsSection
import com.bernaferrari.bravedns.ui.compose.settings.ConsoleLogScreen
import com.bernaferrari.bravedns.ui.compose.settings.DiagnosticsActions
import com.bernaferrari.bravedns.ui.compose.settings.LoggingSettingsScreen
import com.bernaferrari.bravedns.ui.compose.settings.MiscSettingsScreen
import com.bernaferrari.bravedns.ui.compose.settings.PingTestScreen
import com.bernaferrari.bravedns.ui.compose.settings.TunnelSettingsScreen

/**
 * The Settings tab: its searchable hub and the screens it opens (firewall, network, other
 * settings, anti-censorship, logging, About), plus the app lock gate.
 */
internal fun EntryProviderScope<NavKey>.settingsEntries(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    settings: HomeSettingsDependencies,
    aboutUiState: State<AboutUiState>,
) {
    entry<RethinkRoute.Configure> {
        SettingsHub(nav, core, settings)
    }
    entry<RethinkRoute.FirewallSettings> { args ->
        FirewallEntry(nav, core, settings, focusKey = args.focusKey)
    }
    // The universal toggles moved onto the Firewall screen; old stacks and intents land there.
    entry<RethinkRoute.UniversalFirewall> {
        FirewallEntry(nav, core, settings, focusKey = "")
    }
    entry<RethinkRoute.AntiCensorship> {
        AntiCensorshipScreen(
            persistentState = core.persistentState,
            eventLogger = core.eventLogger,
            onBackClick = { nav.popBackStack() }
        )
    }
    entry<RethinkRoute.TunnelSettings> { args ->
        TunnelSettingsScreen(
            persistentState = core.persistentState,
            appConfig = core.appConfig,
            eventLogger = core.eventLogger,
            onOpenVpnProfile = settings.onOpenVpnProfile,
            initialFocusKey = args.focusKey.asFocusKey(),
            onBackClick = { nav.popBackStack() }
        )
    }
    entry<RethinkRoute.MiscSettings> { args ->
        MiscSettingsScreen(
            persistentState = core.persistentState,
            eventLogger = core.eventLogger,
            onNotificationsClick = settings.aboutActions.onNotificationClick,
            initialFocusKey = args.focusKey.asFocusKey(),
            onBackClick = { nav.popBackStack() },
        )
    }
    entry<RethinkRoute.LoggingSettings> { args ->
        LoggingSettingsScreen(
            persistentState = core.persistentState,
            eventLogger = core.eventLogger,
            diagnostics = diagnosticsActions(nav, settings),
            initialFocusKey = args.focusKey.asFocusKey(),
            onBackClick = { nav.popBackStack() },
        )
    }
    entry<RethinkRoute.About> {
        AboutEntry(aboutUiState.value, settings.aboutActions, onBackClick = { nav.popBackStack() })
    }
    entry<RethinkRoute.AppLock> {
        AppLockScreen(
            persistentState = core.persistentState,
            onAuthResult = { result ->
                settings.onAppLockResult(result)
                nav.popBackStack()
            }
        )
    }
}

/** Logs & diagnostics' troubleshooting screens, reached from Logging settings. */
internal fun EntryProviderScope<NavKey>.diagnosticsEntries(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    diagnostics: HomeDiagnosticsDependencies,
) {
    entry<RethinkRoute.Events> {
        EventsScreen(
            viewModel = diagnostics.eventsViewModel,
            eventDao = diagnostics.eventDao,
            onBackClick = { nav.popBackStack() }
        )
    }
    entry<RethinkRoute.ConsoleLogs> {
        ConsoleLogScreen(
            viewModel = diagnostics.consoleLogViewModel,
            consoleLogRepository = diagnostics.consoleLogRepository,
            persistentState = core.persistentState,
            onShareClick = diagnostics.onShareConsoleLogs,
            onDeleteComplete = diagnostics.onConsoleLogsDeleteComplete,
            onBackClick = { nav.popBackStack() }
        )
    }
    entry<RethinkRoute.PingTest> {
        PingTestScreen(onBackClick = { nav.popBackStack() })
    }
    entry<RethinkRoute.Database> {
        DatabaseScreen(onBackClick = { nav.popBackStack() }, appDatabase = diagnostics.appDatabase)
    }
}

/** The Settings tab's root: no back arrow; back goes Home like the other tabs. */
@Composable
private fun SettingsHub(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    settings: HomeSettingsDependencies,
) {
    BackHandler(enabled = nav.current() is RethinkRoute.Configure) {
        nav.popBackStack()
    }
    ConfigureScreen(
        isDebug = settings.isDebug,
        onOpen = { destination ->
            when {
                // The DNS tile checks Private DNS first (a dialog on the Activity).
                destination is SettingsSearchDestination.Dns && destination.focusKey.isBlank() ->
                    settings.configureActions.onConfigureDnsClick()
                destination == SettingsSearchDestination.Notifications -> settings.aboutActions.onNotificationClick()
                else -> destination.route()?.let { nav.push(it) }
            }
        },
        appearance = {
            AppearanceSettingsSection(
                persistentState = core.persistentState,
                eventLogger = core.eventLogger,
                onThemeModeChanged = settings.onThemeModeChanged,
                onThemeColorChanged = settings.onThemeColorChanged,
            )
        },
    )
}

/** The Firewall screen, optionally scrolled to the rule named by [focusKey]. */
@Composable
private fun FirewallEntry(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    settings: HomeSettingsDependencies,
    focusKey: String,
) {
    FirewallSettingsScreen(
        persistentState = core.persistentState,
        eventLogger = core.eventLogger,
        connTrackerRepository = core.connectionTrackerRepository,
        onRulesClick = { nav.push(allRulesRoute) },
        // A rule row lists what that rule blocked, on this tab's stack.
        onNavigateToLogs = { ruleId -> nav.push(RethinkRoute.NetworkLogs(ruleId)) },
        onOpenAccessibilitySettings = settings.configureActions.onOpenAccessibilitySettings,
        initialFocusKey = focusKey.asFocusKey(),
        onBackClick = { nav.popBackStack() },
    )
}

private fun diagnosticsActions(nav: RethinkNavOps, settings: HomeSettingsDependencies) = DiagnosticsActions(
    isDebug = settings.isDebug,
    onEventLogs = { nav.push(RethinkRoute.Events) },
    onAppLogs = { nav.push(RethinkRoute.ConsoleLogs) },
    onConnectivityChecks = { nav.push(RethinkRoute.PingTest) },
    onNetworkStats = settings.aboutActions.onStatsClick,
    onDatabase = { nav.push(RethinkRoute.Database) },
    onFlightRecorder = settings.aboutActions.onFlightRecordClick,
)

@Composable
private fun AboutEntry(uiState: AboutUiState, actions: HomeAboutActions, onBackClick: () -> Unit) {
    AboutScreen(
        uiState = uiState,
        onTelegramClick = actions.onTelegramClick,
        onBugReportClick = actions.onBugReportClick,
        onWhatsNewClick = actions.onWhatsNewClick,
        onAppUpdateClick = actions.onAppUpdateClick,
        onWebsiteClick = actions.onWebsiteClick,
        onGithubClick = actions.onGithubClick,
        onFaqClick = actions.onFaqClick,
        onDocsClick = actions.onDocsClick,
        onPrivacyPolicyClick = actions.onPrivacyPolicyClick,
        onTermsOfServiceClick = actions.onTermsOfServiceClick,
        onLicenseClick = actions.onLicenseClick,
        onTwitterClick = actions.onTwitterClick,
        onEmailClick = actions.onEmailClick,
        onRedditClick = actions.onRedditClick,
        onElementClick = actions.onElementClick,
        onMastodonClick = actions.onMastodonClick,
        onGeneralSettingsClick = actions.onGeneralSettingsClick,
        onAppInfoClick = actions.onAppInfoClick,
        onVpnProfileClick = actions.onVpnProfileClick,
        onNotificationClick = actions.onNotificationClick,
        onStatsClick = actions.onStatsClick,
        onDbStatsClick = actions.onDbStatsClick,
        onFlightRecordClick = actions.onFlightRecordClick,
        onEventLogsClick = actions.onEventLogsClick,
        onTokenClick = actions.onTokenClick,
        onFossClick = actions.onFossClick,
        onFlossFundsClick = actions.onFlossFundsClick,
        onBackClick = onBackClick,
    )
}
