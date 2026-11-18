/*
 * Copyright 2026 RethinkDNS and its authors
 *
 * Android resource/service adapter for the commonMain About renderer.
 */
package com.bernaferrari.bravedns.ui.compose.about

import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.ui.compose.navigation.HomeAboutActions
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols
import com.bernaferrari.bravedns.ui.icons.keepout.BrandIcons

/**
 * Keeps Android-only resources, app state, and intent dispatch out of the renderer. The complete
 * visual tree now lives in shared/commonMain and is also used by the wasm demo.
 */
@Composable
fun AboutScreen(
    uiState: AboutUiState,
    callbacks: HomeAboutActions,
    onBackClick: () -> Unit,
) {
    RethinkAboutScreen(
        uiState = RethinkAboutUiState(
            versionName = uiState.versionName,
            installSource = uiState.installSource,
            buildNumber = uiState.buildNumber,
            slicedVersion = uiState.slicedVersion,
            isFdroid = uiState.isFdroid,
            isDebug = uiState.isDebug,
            isBugReportRunning = uiState.isBugReportRunning,
        ),
        strings = RethinkAboutStrings(
            appName = stringResource(R.string.app_name),
            about = stringResource(R.string.title_about),
            app = stringResource(R.string.about_app),
            whatsNew = stringResource(R.string.about_whats_new, uiState.slicedVersion),
            checkForUpdates = stringResource(R.string.about_app_update_check),
            joinTelegram = stringResource(R.string.about_join_telegram),
            reportABug = stringResource(R.string.about_bug_report),
            collectingLogs = stringResource(R.string.collecting_logs_progress_text),
            web = stringResource(R.string.about_web),
            website = stringResource(R.string.about_website),
            github = stringResource(R.string.about_github),
            faq = stringResource(R.string.about_faq),
            docs = stringResource(R.string.about_docs),
            privacyPolicy = stringResource(R.string.about_privacy_policy),
            terms = stringResource(R.string.about_terms_of_service),
            license = stringResource(R.string.about_license),
            connect = stringResource(R.string.about_connect),
            twitter = stringResource(R.string.about_twitter),
            email = stringResource(R.string.about_email),
            reddit = stringResource(R.string.lbl_reddit),
            element = stringResource(R.string.lbl_matrix),
            mastodon = stringResource(R.string.lbl_mastodon),
            appInfo = stringResource(R.string.about_settings_app_info),
            vpnProfile = stringResource(R.string.about_settings_vpn_profile),
            notifications = stringResource(R.string.about_settings_notification),
            diagnostics = stringResource(R.string.about_diagnostics),
            statistics = stringResource(R.string.title_statistics),
            databaseDump = stringResource(R.string.title_database_dump),
            flightRecorder = stringResource(R.string.about_flight_recorder),
            eventLogs = stringResource(R.string.event_logs_title),
            supportedBy = stringResource(R.string.about_mozilla),
            partnerMozilla = stringResource(R.string.about_partner_mozilla),
            partnerFoss = stringResource(R.string.about_partner_foss),
            partnerFlossFunds = stringResource(R.string.about_partner_floss_funds),
        ),
        actions = RethinkAboutActions(
            onTelegram = callbacks.onTelegramClick,
            onBugReport = callbacks.onBugReportClick,
            onWhatsNew = callbacks.onWhatsNewClick,
            onAppUpdate = callbacks.onAppUpdateClick,
            onWebsite = callbacks.onWebsiteClick,
            onGithub = callbacks.onGithubClick,
            onFaq = callbacks.onFaqClick,
            onDocs = callbacks.onDocsClick,
            onPrivacyPolicy = callbacks.onPrivacyPolicyClick,
            onTerms = callbacks.onTermsOfServiceClick,
            onLicense = callbacks.onLicenseClick,
            onTwitter = callbacks.onTwitterClick,
            onEmail = callbacks.onEmailClick,
            onReddit = callbacks.onRedditClick,
            onElement = callbacks.onElementClick,
            onMastodon = callbacks.onMastodonClick,
            onGeneralSettings = callbacks.onGeneralSettingsClick,
            onAppInfo = callbacks.onAppInfoClick,
            onVpnProfile = callbacks.onVpnProfileClick,
            onNotifications = callbacks.onNotificationClick,
            onStatistics = callbacks.onStatsClick,
            onDatabaseDump = callbacks.onDbStatsClick,
            onFlightRecorder = callbacks.onFlightRecordClick,
            onEventLogs = callbacks.onEventLogsClick,
            onFoss = callbacks.onFossClick,
            onFlossFunds = callbacks.onFlossFundsClick,
        ),
        onBackClick = onBackClick,
        assets = RethinkAboutAssets(
            telegram = { tint -> Icon(BrandIcons.Telegram, null, tint = tint) },
            github = { tint -> Icon(BrandIcons.Github, null, tint = tint) },
            twitter = { tint -> Icon(BrandIcons.Twitter, null, tint = tint) },
            email = { tint -> Icon(MaterialSymbols.Filled.Email, null, tint = tint) },
            reddit = { tint -> Icon(BrandIcons.Reddit, null, tint = tint) },
            element = { tint -> Icon(BrandIcons.Element, null, tint = tint) },
            mastodon = { tint -> Icon(BrandIcons.Mastodon, null, tint = tint) },
            mozillaLogo = { modifier -> Image(painterResource(R.drawable.mozilla), null, modifier = modifier, contentScale = ContentScale.FillWidth) },
            // Single-colour marks: tint with the content color so they follow the in-app theme,
            // not the system day/night resources.
            fossLogo = { modifier -> Image(painterResource(R.drawable.foss_logo), null, modifier = modifier, contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(LocalContentColor.current)) },
            flossFundsLogo = { modifier -> Image(painterResource(R.drawable.ic_floss_fund_badge), null, modifier = modifier, contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(LocalContentColor.current)) },
        ),
    )
}
