/*
 * Copyright 2024 RethinkDNS and its authors
 *
 * Android state and resources -> the common Home renderer's inputs: the UI state with its labels
 * resolved, and the screen's strings (plurals included).
 */
package com.bernaferrari.bravedns.ui.compose.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.ui.compose.common.AppIcon

@Composable
internal fun HomeScreenUiState.toCommonUiState() = RethinkHomeUiState(
    isVpnActive = isVpnActive,
    networkLogsCount = networkLogsCount,
    dnsLogsCount = dnsLogsCount,
    protectionStatus = protectionStatusLabel(protectionStatus),
    isProtectionFailing = isProtectionFailing,
    allowedToday = allowedToday,
    blockedToday = blockedToday,
    dnsBlockedToday = dnsBlockedToday,
    topBlocked = topBlocked.map { app ->
        RethinkHomeBlockedApp(
            id = app.uid.toString(),
            label = app.appName.ifBlank { app.packageName },
            count = app.count,
            icon = { AppIcon(app.packageName, appName = app.appName, size = Dp.Unspecified) },
        )
    },
    dnsName = dnsName,
    dnsLine = dnsLine(),
    isPaused = isPaused,
    pausedDetail = if (pauseRemainingMillis > 0) {
        stringResource(R.string.home_paused_left, formatPauseRemaining(pauseRemainingMillis))
    } else {
        stringResource(R.string.home_paused_until_resumed)
    },
    pauseTimed = pauseRemainingMillis > 0,
    pauseCanShorten = canShortenPause(pauseRemainingMillis),
)

@Composable
private fun HomeScreenUiState.dnsLine(): String {
    if (dnsName.isBlank()) return ""
    val presets = mapOf(
        "adult" to stringResource(R.string.home_dns_preset_family),
        "social media" to stringResource(R.string.home_dns_preset_social_media),
        "plus" to stringResource(R.string.home_dns_preset_custom),
    )
    val label = homeDnsLabel(dnsName, dnsIsKeepout, stringResource(R.string.home_dns_keepout), presets)
    return stringResource(R.string.home_dns_line, label)
}

@Composable
private fun protectionStatusLabel(status: HomeProtectionStatus?): String {
    if (status == null) return ""
    return stringResource(
        when (status) {
            HomeProtectionStatus.Protected -> R.string.home_protected
            HomeProtectionStatus.UltraSecure -> R.string.lbl_ultra_secure
            HomeProtectionStatus.Failing -> R.string.status_failing
            HomeProtectionStatus.Waiting -> R.string.status_waiting
            HomeProtectionStatus.Exposed -> R.string.status_exposed
            HomeProtectionStatus.NoNetwork -> R.string.status_no_network
            HomeProtectionStatus.Paused -> R.string.home_paused
        }
    )
}

@Composable
internal fun androidHomeStrings() = RethinkHomeStrings(
    productName = stringResource(R.string.app_name),
    protected = stringResource(R.string.home_protected),
    notActive = stringResource(R.string.home_not_active),
    start = stringResource(R.string.lbl_start),
    stop = stringResource(R.string.lbl_stop),
    protectedSubtitle = stringResource(R.string.home_protected_subtitle),
    inactiveSubtitle = stringResource(R.string.home_inactive_subtitle),
    failingSubtitle = stringResource(R.string.home_failing_subtitle),
    connections = stringResource(R.string.home_connections),
    dnsQueries = stringResource(R.string.home_dns_queries),
    networkProtection = stringResource(R.string.home_network_protection),
    live = stringResource(R.string.home_live),
    offline = stringResource(R.string.home_offline),
    reconnecting = stringResource(R.string.home_reconnecting),
    activityTitle = stringResource(R.string.home_activity_title),
    activitySubtitle = stringResource(R.string.home_activity_subtitle),
    last24Hours = stringResource(R.string.home_last_24_hours),
    allowed = stringResource(R.string.lbl_allowed),
    blocked = stringResource(R.string.lbl_blocked),
    stoppedAtDns = stoppedAtDnsLabel(),
    stoppedMost = stringResource(R.string.home_stopped_most),
    nothingStopped = stringResource(R.string.home_nothing_stopped),
    seeStatistics = stringResource(R.string.home_see_statistics),
    blockedCount = blockedCountLabel(),
    paused = stringResource(R.string.home_paused),
    pause = stringResource(R.string.home_pause),
    pauseFifteenMinutes = stringResource(R.string.home_pause_15_minutes),
    pauseOneHour = stringResource(R.string.home_pause_1_hour),
    pauseUntilResumed = stringResource(R.string.home_pause_until_resumed),
    pauseRemoveFifteenMinutes = stringResource(R.string.home_pause_remove_15_minutes),
    pauseRemoveFifteenMinutesDescription = stringResource(R.string.home_pause_remove_15_minutes_description),
    pauseAddFifteenMinutes = stringResource(R.string.home_pause_add_15_minutes),
    pauseAddFifteenMinutesDescription = stringResource(R.string.home_pause_add_15_minutes_description),
    pauseNoTimer = stringResource(R.string.home_pause_no_timer),
    pauseNoTimerDescription = stringResource(R.string.home_pause_no_timer_description),
    pauseResumeInFifteenMinutesDescription = stringResource(R.string.home_pause_resume_in_15_minutes),
    pauseResumeInOneHourDescription = stringResource(R.string.home_pause_resume_in_1_hour),
    resume = stringResource(R.string.home_resume),
    openDns = stringResource(R.string.home_open_dns),
)

@Composable
private fun stoppedAtDnsLabel(): (Long) -> String {
    val resources = LocalResources.current
    return { count -> resources.getQuantityString(R.plurals.home_stopped_at_dns_count, count.toInt(), count.toInt()) }
}

@Composable
private fun blockedCountLabel(): (Long) -> String {
    val resources = LocalResources.current
    return { count -> resources.getQuantityString(R.plurals.home_blocked_count, count.toInt(), count.toInt()) }
}
