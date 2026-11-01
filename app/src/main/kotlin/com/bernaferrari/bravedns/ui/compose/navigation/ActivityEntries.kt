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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.data.SummaryStatisticsType
import com.bernaferrari.bravedns.ui.compose.logs.ActivityScreen
import com.bernaferrari.bravedns.ui.compose.logs.ConnectionSheetNavigation
import com.bernaferrari.bravedns.ui.compose.logs.DomainConnectionsInputType
import com.bernaferrari.bravedns.ui.compose.logs.DomainConnectionsScreen
import com.bernaferrari.bravedns.ui.compose.logs.NetworkLogsScreen
import com.bernaferrari.bravedns.ui.compose.statistics.DetailedStatisticsScreen
import com.bernaferrari.bravedns.ui.compose.statistics.InsightsRowActions
import com.bernaferrari.bravedns.ui.compose.statistics.SummaryStatisticsScreen
import com.bernaferrari.bravedns.viewmodel.DomainConnectionsViewModel
import com.bernaferrari.bravedns.viewmodel.SummaryStatisticsViewModel

/**
 * The Activity tab (the log and Insights, one root whose view is [activityView]) and the lists it
 * drills into: one app's or one search's log, a domain's or ASN's connections, a full stat list.
 */
internal fun EntryProviderScope<NavKey>.activityEntries(
    nav: RethinkNavOps,
    activityView: MutableState<RethinkActivityView>,
    core: HomeCoreDependencies,
    activity: HomeActivityDependencies,
) {
    val sheetNavigation = connectionSheetNavigation(nav)
    val insightsRowActions = insightsRowActions(nav, core, sheetNavigation)

    val activityTab: @Composable () -> Unit = {
        ActivityTab(
            nav = nav,
            view = activityView.value,
            onViewChange = { activityView.value = it },
            core = core,
            activity = activity,
            sheetNavigation = sheetNavigation,
            insightsRowActions = insightsRowActions,
        )
    }
    entry<RethinkRoute.Activity> { activityTab() }
    // Only a back stack saved before Stats moved into Activity still holds this key.
    entry<RethinkRoute.Statistics> { activityTab() }
    // The unfiltered log is the Activity tab; this entry is one app's log (by uid) or the log
    // searched for a domain or address, pushed on whichever tab asked (App detail, Insights, rules).
    entry<RethinkRoute.NetworkLogs> { args ->
        NetworkLogsScreen(
            connectionTrackerRepository = core.connectionTrackerRepository,
            dnsLogRepository = core.dnsLogRepository,
            persistentState = core.persistentState,
            eventLogger = core.eventLogger,
            onAppLogsClick = { nav.push(RethinkRoute.ConsoleLogs) },
            onBackClick = { nav.popBackStack() },
            sheetNavigation = sheetNavigation,
            query = args.query,
            appUid = args.uid,
            appName = args.appName,
        )
    }
    entry<RethinkRoute.DomainConnections> { args ->
        DomainConnectionsScreen(
            viewModel = activity.domainConnectionsViewModel,
            type = DomainConnectionsInputType.fromValue(args.typeId),
            flag = args.flag,
            domain = args.domain,
            asn = args.asn,
            ip = args.ip,
            isBlocked = args.isBlocked,
            timeCategory = DomainConnectionsViewModel.TimeCategory.fromValue(args.timeCategory)
                ?: DomainConnectionsViewModel.TimeCategory.ONE_HOUR,
            onBackClick = { nav.popBackStack() },
            onOpenApp = { uid -> nav.push(RethinkRoute.AppInfo(uid)) },
            onOpenLogs = { query -> nav.push(RethinkRoute.NetworkLogs(query)) },
        )
    }
    entry<RethinkRoute.DetailedStatistics> { args ->
        DetailedStatisticsScreen(
            type = SummaryStatisticsType.getType(args.typeId),
            timeCategory = SummaryStatisticsViewModel.TimeCategory.fromValue(args.timeCategory)
                ?: SummaryStatisticsViewModel.TimeCategory.TWENTY_FOUR_HOUR,
            viewModel = activity.detailedStatsViewModel,
            onBackClick = { nav.popBackStack() },
            rowActions = insightsRowActions,
        )
    }
}

/** The tab's root: Log or Insights behind one view switch. Back from the root goes Home. */
@Composable
private fun ActivityTab(
    nav: RethinkNavOps,
    view: RethinkActivityView,
    onViewChange: (RethinkActivityView) -> Unit,
    core: HomeCoreDependencies,
    activity: HomeActivityDependencies,
    sheetNavigation: ConnectionSheetNavigation,
    insightsRowActions: InsightsRowActions,
) {
    BackHandler(enabled = nav.current().let { it == RethinkRoute.Activity || it == RethinkRoute.Statistics }) {
        nav.popBackStack()
    }
    val title = stringResource(R.string.nav_activity)
    ActivityScreen(
        view = view,
        onViewChange = onViewChange,
        log = { viewSwitch ->
            NetworkLogsScreen(
                connectionTrackerRepository = core.connectionTrackerRepository,
                dnsLogRepository = core.dnsLogRepository,
                persistentState = core.persistentState,
                eventLogger = core.eventLogger,
                onAppLogsClick = { nav.push(RethinkRoute.ConsoleLogs) },
                sheetNavigation = sheetNavigation,
                title = title,
                viewSwitch = viewSwitch,
            )
        },
        insights = { viewSwitch ->
            SummaryStatisticsScreen(
                viewModel = activity.summaryViewModel,
                persistentState = core.persistentState,
                onSeeMoreClick = activity.onOpenDetailedStats,
                rowActions = insightsRowActions,
                title = title,
                viewSwitch = viewSwitch,
            )
        },
    )
}

/** Insights rows open the app, or an ASN's connections, on the tab that shows them. */
private fun insightsRowActions(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    sheetNavigation: ConnectionSheetNavigation,
) = InsightsRowActions(
    eventLogger = core.eventLogger,
    connections = core.connectionTrackerRepository,
    lookups = core.dnsLogRepository,
    onOpenApp = { uid -> nav.push(RethinkRoute.AppInfo(uid)) },
    onOpenAsn = { asn, blocked, window ->
        nav.push(
            RethinkRoute.DomainConnections(
                typeId = DomainConnectionsInputType.ASN.type,
                flag = "",
                domain = "",
                asn = asn,
                ip = "",
                isBlocked = blocked,
                timeCategory = window.value,
            )
        )
    },
    sheetNavigation = sheetNavigation,
    loadFavIcon = core.persistentState.fetchFavIcon,
)
