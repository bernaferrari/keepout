/*
 * Copyright 2024 RethinkDNS and its authors
 *
 * Android adapter for the common Home renderer. Keep platform resources and live VPN state here;
 * the actual Compose screen lives in :shared/commonMain.
 */
package com.bernaferrari.bravedns.ui.compose.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.AppBlockCount
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

enum class HomeProtectionStatus {
    Protected,
    UltraSecure,
    Failing,
    Waiting,
    Exposed,
    NoNetwork,
    Paused,
}

data class HomeScreenUiState(
    val isVpnActive: Boolean = false,
    val networkLogsCount: Long = 0,
    val dnsLogsCount: Long = 0,
    val protectionStatus: HomeProtectionStatus? = null,
    val isProtectionFailing: Boolean = false,
    val allowedToday: Long = 0,
    val blockedToday: Long = 0,
    val dnsBlockedToday: Long = 0,
    val topBlocked: List<AppBlockCount> = emptyList(),
    val dnsName: String = "",
    /** The resolver is Keepout DNS (Rethink remote), so the hero reads "DNS: Keepout · <preset>". */
    val dnsIsKeepout: Boolean = false,
    val isPaused: Boolean = false,
    /** Time left on the pause; 0 while paused means it lasts until resumed. */
    val pauseRemainingMillis: Long = 0,
    val health: HomeHealthIssue? = null,
    val card: HomeCard? = null,
)

/**
 * What Home asks the Activity to do: the VPN lifecycle (start, stop, pause, resume, consent) and
 * the device-level fixes. Navigation stays with the nav host.
 */
data class HomeProtectionActions(
    /** Start when off, stop when on (the hero's Start and Stop). */
    val onStartStop: () -> Unit,
    /** Welcome's "Turn on protection": start (never stop) and show the success card once on. */
    val onTurnOnFromWelcome: () -> Unit,
    val onPause: (HomePauseDuration) -> Unit,
    val onResume: () -> Unit,
    /** Fixes for the device-level banners (always-on VPN, Private DNS). */
    val onHealthAction: (HomeHealthIssue) -> Unit,
    val onDismissProtectedCard: () -> Unit,
    val onAllowNotifications: () -> Unit,
    val onDeclineNotifications: () -> Unit,
    /** The paused hero's time controls (±15 min, until resumed, a fresh timer). */
    val onAdjustPause: (HomePauseAdjustment) -> Unit = {},
) {
    companion object {
        val None = HomeProtectionActions({}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Composable
fun HomeScreen(
    uiState: HomeScreenUiState,
    actions: HomeProtectionActions,
    onActivityClick: (() -> Unit)? = null,
    onDnsClick: (() -> Unit)? = null,
    onAppClick: ((Int) -> Unit)? = null,
    onReviewApps: (() -> Unit)? = null,
    onChooseBlocklists: (() -> Unit)? = null,
) {
    RethinkHomeScreen(
        uiState = uiState.toCommonUiState(),
        strings = androidHomeStrings(),
        onStartStopClick = actions.onStartStop,
        onActivityClick = onActivityClick,
        onDnsClick = onDnsClick,
        onBlockedAppClick = onAppClick?.let { open -> { id: String -> id.toIntOrNull()?.let(open) } },
        onPause = { choice -> actions.onPause(choice.toHomePauseDuration()) },
        onResume = actions.onResume,
        onAdjustPause = { adjustment -> actions.onAdjustPause(adjustment.toHomePauseAdjustment()) },
        banner = uiState.health?.let { healthBanner(it, uiState, actions, onDnsClick) },
        card = uiState.card?.let { homeCard(it, uiState, actions, onReviewApps, onChooseBlocklists) },
    )
}

private fun RethinkPauseChoice.toHomePauseDuration() = when (this) {
    RethinkPauseChoice.FifteenMinutes -> HomePauseDuration.FifteenMinutes
    RethinkPauseChoice.OneHour -> HomePauseDuration.OneHour
    RethinkPauseChoice.UntilResumed -> HomePauseDuration.UntilResumed
}

private fun RethinkPauseAdjustment.toHomePauseAdjustment() = when (this) {
    RethinkPauseAdjustment.RemoveFifteenMinutes -> HomePauseAdjustment.RemoveFifteenMinutes
    RethinkPauseAdjustment.AddFifteenMinutes -> HomePauseAdjustment.AddFifteenMinutes
    RethinkPauseAdjustment.UntilResumed -> HomePauseAdjustment.UntilResumed
    RethinkPauseAdjustment.FifteenMinutes -> HomePauseAdjustment.FifteenMinutes
    RethinkPauseAdjustment.OneHour -> HomePauseAdjustment.OneHour
}

@Composable
private fun healthBanner(
    issue: HomeHealthIssue,
    uiState: HomeScreenUiState,
    actions: HomeProtectionActions,
    onDnsClick: (() -> Unit)?,
): RethinkHomeBanner = when (issue) {
    HomeHealthIssue.OtherAlwaysOnVpn -> RethinkHomeBanner(
        title = stringResource(R.string.always_on_dialog_heading),
        body = stringResource(R.string.always_on_dialog),
        actionLabel = stringResource(R.string.always_on_dialog_positive_btn),
        onAction = { actions.onHealthAction(issue) },
    )
    HomeHealthIssue.PrivateDnsOverride -> RethinkHomeBanner(
        title = stringResource(R.string.private_dns_dialog_heading),
        body = stringResource(R.string.private_dns_dialog_desc),
        actionLabel = stringResource(R.string.home_health_open_settings),
        onAction = { actions.onHealthAction(issue) },
    )
    HomeHealthIssue.DnsFailing -> RethinkHomeBanner(
        title = stringResource(
            R.string.home_health_dns_failing_title,
            if (uiState.dnsIsKeepout || uiState.dnsName.isBlank()) stringResource(R.string.home_health_dns_keepout) else uiState.dnsName,
        ),
        body = stringResource(R.string.home_health_dns_failing_body),
        actionLabel = stringResource(R.string.home_health_dns_failing_action),
        onAction = { onDnsClick?.invoke() ?: actions.onHealthAction(issue) },
    )
}

@Composable
private fun homeCard(
    card: HomeCard,
    uiState: HomeScreenUiState,
    actions: HomeProtectionActions,
    onReviewApps: (() -> Unit)?,
    onChooseBlocklists: (() -> Unit)?,
): RethinkHomeCard = when (card) {
    HomeCard.Protected -> {
        val blocklists = onChooseBlocklists?.takeIf { uiState.dnsIsKeepout }
        RethinkHomeCard(
            title = stringResource(R.string.home_card_protected_title),
            body = stringResource(if (uiState.dnsIsKeepout) R.string.home_card_protected_body_keepout else R.string.home_card_protected_body),
            icon = MaterialSymbols.Filled.GppGood,
            primaryLabel = stringResource(R.string.home_card_review_apps),
            onPrimary = {
                actions.onDismissProtectedCard()
                onReviewApps?.invoke()
            },
            secondaryLabel = blocklists?.let { stringResource(R.string.home_card_choose_blocklists) },
            onSecondary = blocklists?.let {
                {
                    actions.onDismissProtectedCard()
                    it()
                }
            },
            dismissLabel = stringResource(R.string.lbl_dismiss),
            onDismiss = actions.onDismissProtectedCard,
        )
    }
    HomeCard.Notifications -> RethinkHomeCard(
        title = stringResource(R.string.home_card_notifications_title),
        body = stringResource(R.string.home_card_notifications_body),
        icon = MaterialSymbols.Filled.Notifications,
        primaryLabel = stringResource(R.string.home_card_notifications_allow),
        onPrimary = actions.onAllowNotifications,
        secondaryLabel = stringResource(R.string.home_card_notifications_decline),
        onSecondary = actions.onDeclineNotifications,
    )
}
