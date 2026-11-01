/*
 * Copyright 2021 RethinkDNS and its authors
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
package com.bernaferrari.bravedns.ui.compose.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.data.AppConfig
import com.bernaferrari.bravedns.database.ConnectionTracker
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.FirewallManager
import com.bernaferrari.bravedns.ui.compose.apps.RethinkAppInfoActions
import com.bernaferrari.bravedns.ui.compose.apps.RethinkAppInfoActivityItem
import com.bernaferrari.bravedns.ui.compose.apps.RethinkAppInfoScreen
import com.bernaferrari.bravedns.ui.compose.apps.RethinkAppInfoState
import com.bernaferrari.bravedns.ui.compose.apps.RethinkAppInfoStrings
import com.bernaferrari.bravedns.ui.compose.apps.RethinkAppMode
import com.bernaferrari.bravedns.ui.compose.common.AppIcon
import com.bernaferrari.bravedns.ui.compose.logs.ConnectionSheetHost
import com.bernaferrari.bravedns.ui.compose.logs.ConnectionSheetNavigation
import com.bernaferrari.bravedns.ui.compose.logs.connectionLogModel
import com.bernaferrari.bravedns.ui.compose.logs.toConnectionSheetRequest
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.util.Constants.Companion.INVALID_UID
import com.bernaferrari.bravedns.util.Constants.Companion.TIME_FORMAT_1
import com.bernaferrari.bravedns.util.UIUtils.openAndroidAppInfo
import com.bernaferrari.bravedns.util.Utilities
import com.bernaferrari.bravedns.viewmodel.AppConnectionsViewModel
import com.bernaferrari.bravedns.viewmodel.CustomDomainViewModel
import com.bernaferrari.bravedns.viewmodel.CustomIpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * App detail for one uid: reads the app's firewall flags, temporary access and recent traffic,
 * and writes every change through [FirewallManager] with an Undo snackbar ([AppDetailEditor]).
 * Recent activity rows open the Connection sheet ([connectionSheetNavigation] says where its
 * links lead).
 */
@Composable
fun AppInfoScreen(
    uid: Int,
    eventLogger: EventLogger,
    ipRulesViewModel: CustomIpViewModel,
    domainRulesViewModel: CustomDomainViewModel,
    networkLogsViewModel: AppConnectionsViewModel,
    onBackClick: () -> Unit,
    onCustomIpRulesClick: (Int) -> Unit,
    onCustomDomainRulesClick: (Int) -> Unit,
    /** The Activity log for this app, by uid; the String is its name, which titles the log. */
    onSeeAllActivity: ((query: String) -> Unit)? = null,
    /** Keepout's own App detail: its own connections, which the Activity log never holds. */
    onSeeKeepoutConnections: (() -> Unit)? = null,
    connectionSheetNavigation: ConnectionSheetNavigation = ConnectionSheetNavigation(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val state = remember(uid) { AppDetailState() }
    var reloadToken by remember(uid) { mutableIntStateOf(0) }
    var selectedConnection by remember(uid) { mutableStateOf<ConnectionTracker?>(null) }
    // Keepout's own traffic is logged apart from every other app's.
    val isKeepout = uid == context.applicationInfo.uid

    AppDetailLoadEffect(uid, reloadToken, state, ipRulesViewModel, domainRulesViewModel)
    val minutesLeft = rememberTempAllowMinutesLeft(uid, state)
    val activityRows = rememberRecentActivity(uid, isKeepout, networkLogsViewModel)
    val domainRuleCount by remember(uid) { domainRulesViewModel.domainRulesCount(uid) }.collectAsState(initial = null)
    val ipRuleCount by remember(uid) { ipRulesViewModel.ipRulesCount(uid) }.collectAsState(initial = null)

    val info = state.appInfo
    val appName = state.appName
    val strings = appInfoStrings()
    val editor = AppDetailEditor(uid, state, scope, snackbar, eventLogger, context, appDetailCopy(appName, strings))

    RethinkAppInfoScreen(
        state = rethinkAppInfoState(state, minutesLeft, activityItems(activityRows), domainRuleCount, ipRuleCount),
        strings = strings,
        actions = RethinkAppInfoActions(
            onBackClick = onBackClick,
            onWifiClick = editor::toggleWifi,
            onMobileClick = editor::toggleMobile,
            onModeChange = editor::changeMode,
            onTempAllowChange = editor::setTempAllow,
            onProxyExcludedChange = editor::setProxyExcluded,
            onSystemAppInfo = { info?.let { openAndroidAppInfo(context, it.packageName) } },
            onIpRules = { onCustomIpRulesClick(uid) },
            onDomainRules = { onCustomDomainRulesClick(uid) },
            // Keepout's own rows aren't in the connection log the Connection sheet edits.
            onActivityClick = { item: RethinkAppInfoActivityItem ->
                val row = activityRows?.firstOrNull { it.key == item.id }
                if (row != null) selectedConnection = row.latest
            }.takeUnless { isKeepout },
            // The Activity log never holds Keepout's own uid: its See all opens Keepout's own log.
            onSeeAllActivity = if (isKeepout) {
                onSeeKeepoutConnections
            } else {
                onSeeAllActivity?.let { open -> { open(appName) } }?.takeIf { appName.isNotBlank() }
            },
        ),
        appNotes = info?.let { app -> { AppNotesSection(uid, app) } },
        snackbarHostState = snackbar,
        // The page header: a step up from the 40dp list rows.
        titleLeading = info?.let { app -> { AppIcon(app.packageName, appName = app.appName, size = SharedDimensions.iconContainerLg) } },
    )

    selectedConnection?.let { connection ->
        AppConnectionSheet(
            connection = connection,
            eventLogger = eventLogger,
            navigation = connectionSheetNavigation,
            onDismiss = {
                selectedConnection = null
                reloadToken++
            },
            onRulesChanged = { reloadToken++ },
        )
    }
}

/** (Re)load the app whenever [uid] or [reloadToken] changes, and point the rule counts at it. */
@Composable
private fun AppDetailLoadEffect(
    uid: Int,
    reloadToken: Int,
    state: AppDetailState,
    ipRulesViewModel: CustomIpViewModel,
    domainRulesViewModel: CustomDomainViewModel,
) {
    val wireguardAppsProxyMapDesc = stringResource(R.string.wireguard_apps_proxy_map_desc)
    val appConfig = koinInject<AppConfig>()
    LaunchedEffect(uid, reloadToken) {
        if (uid == INVALID_UID) {
            state.onLoaded(null)
            return@LaunchedEffect
        }
        ipRulesViewModel.setUid(uid)
        domainRulesViewModel.setUid(uid)
        state.onLoaded(withContext(Dispatchers.IO) { loadAppInfo(uid, wireguardAppsProxyMapDesc, appConfig) })
    }
}

/** Live countdown from the stored expiry; when it passes, re-read what the firewall now holds. */
@Composable
private fun rememberTempAllowMinutesLeft(uid: Int, state: AppDetailState): Int? {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.tempAllowExpiry) {
        while (state.tempAllowExpiry > 0L) {
            now = System.currentTimeMillis()
            if (now >= state.tempAllowExpiry) {
                state.tempAllowExpiry = withContext(Dispatchers.IO) { FirewallManager.tempAllowExpiry(uid) }
                break
            }
            delay(1_000L)
        }
    }
    return minutesLeft(state.tempAllowExpiry, now)
}

/** Recent activity, one row per destination and verdict; null while loading. */
@Composable
private fun rememberRecentActivity(
    uid: Int,
    isKeepout: Boolean,
    networkLogsViewModel: AppConnectionsViewModel,
): List<RecentActivityRow>? {
    val recentFlow = remember(uid) {
        if (isKeepout) networkLogsViewModel.recentKeepoutConnections() else networkLogsViewModel.recentConnections(uid)
    }
    val recent by recentFlow.collectAsState(initial = null)
    return remember(recent) { recent?.let { foldRecentActivity(it) } }
}

/** The Connection sheet over App detail; already on this app's page, so no "Open <app>". */
@Composable
private fun AppConnectionSheet(
    connection: ConnectionTracker,
    eventLogger: EventLogger,
    navigation: ConnectionSheetNavigation,
    onDismiss: () -> Unit,
    onRulesChanged: () -> Unit,
) {
    val model = connectionLogModel(connection)
    ConnectionSheetHost(
        request = connection.toConnectionSheetRequest(model.timeLabel, model.typeLabel, model.details),
        eventLogger = eventLogger,
        onDismiss = onDismiss,
        navigation = navigation.copy(onOpenApp = null),
        onRulesChanged = onRulesChanged,
    )
}

@Composable
private fun rethinkAppInfoState(
    state: AppDetailState,
    minutesLeft: Int?,
    activity: List<RethinkAppInfoActivityItem>?,
    domainRuleCount: Int?,
    ipRuleCount: Int?,
): RethinkAppInfoState {
    val kind = appStatusKind(state.appStatus, state.connStatus, minutesLeft != null)
    val appName = state.appName
    return RethinkAppInfoState(
        loading = !state.loaded,
        appAvailable = state.appInfo != null,
        title = if (!state.loaded) "" else appName.ifBlank { stringResource(R.string.bsct_app_info) },
        subtitle = state.appInfo?.packageName?.takeIf { it.isNotBlank() && it != appName },
        status = appStatusText(kind, minutesLeft),
        statusTone = kind.tone,
        statusDetail = state.proxyDetails.takeIf { it.isNotBlank() },
        wifiBlocked = state.connStatus.wifiBlocked,
        mobileBlocked = state.connStatus.mobileBlocked,
        mode = state.appStatus.toAppMode(),
        tempAllowMinutesLeft = minutesLeft,
        proxyExcluded = state.isProxyExcluded,
        showProxyExclude = showProxyExclude(state.anyProxySetUp, state.isProxyExcluded),
        activity = activity,
        domainRuleCount = domainRuleCount,
        ipRuleCount = ipRuleCount,
        enabled = !state.busy,
    )
}

@Composable
private fun appStatusText(kind: AppStatusKind, minutesLeft: Int?): String = when (kind) {
    AppStatusKind.TemporarilyAllowed -> pluralStringResource(R.plurals.app_status_temp_allowed, minutesLeft ?: 0, minutesLeft ?: 0)
    AppStatusKind.NoInternet -> stringResource(R.string.app_status_no_internet)
    AppStatusKind.BlockedOnWifi -> stringResource(R.string.ada_app_status_block_wifi)
    AppStatusKind.BlockedOnMobileData -> stringResource(R.string.ada_app_status_block_md)
    AppStatusKind.CanUseInternet -> stringResource(R.string.app_status_can_use_internet)
    AppStatusKind.TrustedOnly -> stringResource(R.string.app_status_trusted_only)
    AppStatusKind.SkipsAllFiltering -> stringResource(R.string.ada_app_status_bypass_dns_firewall)
    AppStatusKind.SkipsUniversalRules -> stringResource(R.string.app_status_skips_universal)
    AppStatusKind.Excluded -> stringResource(R.string.app_status_excluded)
    AppStatusKind.Unknown -> stringResource(R.string.ada_app_status_unknown)
}

/** "Blocked · 14:02 · 3 times": verdict, when (minutes are enough), and how often. */
@Composable
private fun activityItems(rows: List<RecentActivityRow>?): List<RethinkAppInfoActivityItem>? {
    val blockedLabel = stringResource(R.string.lbl_blocked)
    val allowedLabel = stringResource(R.string.lbl_allowed)
    return rows?.map { row ->
        RethinkAppInfoActivityItem(
            id = row.key,
            title = row.destination,
            subtitle = listOf(
                if (row.blocked) blockedLabel else allowedLabel,
                // Seconds made the line wrap at larger font sizes.
                Utilities.convertLongToTime(row.latest.timeStamp, TIME_FORMAT_1).substringBeforeLast(':'),
                pluralStringResource(R.plurals.app_activity_count, row.count, row.count),
            ).joinToString(" · "),
            blocked = row.blocked,
        )
    }
}

@Composable
private fun appDetailCopy(appName: String, strings: RethinkAppInfoStrings): AppDetailCopy {
    val modeChangedFormat = stringResource(R.string.app_mode_changed)
    val modeLabels = mapOf(
        RethinkAppMode.Normal to strings.normal,
        RethinkAppMode.TrustedOnly to strings.trustedOnly,
        RethinkAppMode.SkipAllFiltering to strings.skipAllFiltering,
        RethinkAppMode.SkipUniversalRules to strings.skipUniversalRules,
        RethinkAppMode.Excluded to strings.exclude,
    )
    return AppDetailCopy(
        undo = stringResource(R.string.lbl_undo),
        excludeUnknownPackage = stringResource(R.string.exclude_no_package_err_toast),
        tempAllowed = stringResource(R.string.connection_done_allowed_temporarily, appName),
        tempEnded = stringResource(R.string.app_temp_access_ended),
        modeChanged = { mode -> modeChangedFormat.format(appName, modeLabels.getValue(mode)) },
    )
}

@Composable
private fun appInfoStrings(): RethinkAppInfoStrings {
    val noRules = stringResource(R.string.app_rules_none)
    val wifiAllowed = stringResource(R.string.app_state_wifi_allowed)
    val wifiBlocked = stringResource(R.string.app_state_wifi_blocked)
    val mobileAllowed = stringResource(R.string.app_state_mobile_allowed)
    val mobileBlocked = stringResource(R.string.app_state_mobile_blocked)
    val resources = LocalResources.current
    return RethinkAppInfoStrings(
        unavailable = stringResource(R.string.ada_noapp_dialog_message),
        loading = stringResource(R.string.lbl_loading),
        internetAccess = stringResource(R.string.app_internet_access),
        wifi = stringResource(R.string.ada_app_unmetered),
        mobile = stringResource(R.string.lbl_mobile_data),
        allowed = stringResource(R.string.lbl_allowed),
        blocked = stringResource(R.string.lbl_blocked),
        temporaryAllow = stringResource(R.string.temp_allow_label),
        stopTemporaryAccess = { minutes -> resources.getQuantityString(R.plurals.app_stop_temp_access, minutes, minutes) },
        recentActivity = stringResource(R.string.app_recent_activity),
        seeAllActivity = stringResource(R.string.app_see_all_activity),
        noRecentActivity = stringResource(R.string.app_no_recent_activity),
        rules = stringResource(R.string.lbl_rules),
        domainAndIpRules = stringResource(R.string.app_rules_row),
        rulesSummary = { domains, ips ->
            when {
                domains == null || ips == null -> null
                domains == 0 && ips == 0 -> noRules
                else -> resources.getQuantityString(R.plurals.app_rules_domains, domains, domains) + " · " +
                    resources.getQuantityString(R.plurals.app_rules_ips, ips, ips)
            }
        },
        modeTitle = stringResource(R.string.ada_app_mode_title),
        normal = stringResource(R.string.ada_app_mode_normal),
        normalDescription = stringResource(R.string.ada_app_mode_normal_desc),
        trustedOnly = stringResource(R.string.ada_app_isolate),
        trustedOnlyDescription = stringResource(R.string.ada_app_isolate_desc),
        skipAllFiltering = stringResource(R.string.ada_app_bypass_dns_firewall),
        skipAllFilteringDescription = stringResource(R.string.ada_app_bypass_dns_firewall_desc),
        skipUniversalRules = stringResource(R.string.ada_app_bypass_univ),
        skipUniversalRulesDescription = stringResource(R.string.ada_app_bypass_univ_desc),
        exclude = stringResource(R.string.ada_app_exclude),
        excludeDescription = stringResource(R.string.ada_app_exclude_desc),
        excludeWarning = stringResource(R.string.app_mode_exclude_warning),
        skipAllFilteringWarning = stringResource(R.string.app_mode_skip_all_warning),
        apply = stringResource(R.string.lbl_apply),
        cancel = stringResource(R.string.lbl_cancel),
        more = stringResource(R.string.app_more),
        proxyExclude = stringResource(R.string.exclude_apps_from_proxy),
        proxyExcludeDescription = stringResource(R.string.exclude_apps_from_proxy_desc),
        systemAppInfo = stringResource(R.string.about_settings_app_info),
        wifiState = { blocked -> if (blocked) wifiBlocked else wifiAllowed },
        mobileState = { blocked -> if (blocked) mobileBlocked else mobileAllowed },
    )
}
