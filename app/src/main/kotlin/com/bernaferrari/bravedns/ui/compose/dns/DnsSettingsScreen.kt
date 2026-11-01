/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.dns

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R

/** Android resources for the shared DNS screen: hero, the three choices, on-device blocking. */
@Composable
fun DnsSettingsScreen(
    uiState: DnsSettingsUiState,
    navigation: RethinkDnsSettingsNavigation,
    initialFocusKey: String? = null,
    onBackClick: (() -> Unit)? = null,
) {
    RethinkDnsSettingsScreen(
        state = uiState.toRethinkDnsSettingsState(rethinkPresetLabel(uiState)),
        strings = androidDnsSettingsStrings(),
        navigation = navigation,
        onBackClick = onBackClick,
        focusedSettingId = initialFocusKey,
    )
}

/** Android preferences behind DNS › Advanced. */
@Composable
fun DnsAdvancedSettingsScreen(
    uiState: DnsSettingsUiState,
    navigation: RethinkDnsSettingsNavigation,
    viewModel: DnsSettingsViewModel,
    initialFocusKey: String? = null,
    onBackClick: (() -> Unit)? = null,
) {
    RethinkDnsAdvancedScreen(
        state = uiState.toRethinkDnsSettingsState(rethinkPresetLabel(uiState)),
        strings = androidDnsSettingsStrings(),
        navigation = navigation,
        actions = RethinkDnsAdvancedActions(
            onCustomDownloaderChange = { viewModel.setUseCustomDownloadManager(it) },
            onPeriodicUpdateChange = { viewModel.setPeriodicallyCheckBlocklistUpdate(it) },
            onDnsAlgChange = { viewModel.setDnsAlgEnabled(it) },
            onSplitDnsChange = { viewModel.setSplitDns(it) },
            onRulesAsFirewallChange = { viewModel.setBypassBlockInDns(it) },
            onFaviconsChange = { viewModel.setFavIconEnabled(it) },
            onDnsCacheChange = { viewModel.setEnableDnsCache(it) },
            onProxyDnsChange = { viewModel.setProxyDns(it) },
            onUndelegatedDomainsChange = { viewModel.setUseSystemDnsForUndelegatedDomains(it) },
            onFallbackDnsChange = { viewModel.setUseFallbackDnsToBypass(it) },
            onBlockFreeModeChange = { viewModel.setBlockFreeDnsMode(it.toAndroidBlockFreeDnsMode()) },
            onPreventLeaksChange = { viewModel.setPreventDnsLeaksEnabled(it) },
        ),
        onBackClick = onBackClick,
        focusedSettingId = initialFocusKey,
        smartDnsSettings = { SmartDnsProfileSetting() },
        additionalSettings = { CaptivePortalSetting() },
    )
}

/**
 * The name people see for a Rethink DNS endpoint: "Default", "Family", "Custom" for the stock
 * ones, the stored name otherwise. Display only; stored names and URLs are unchanged.
 */
@Composable
fun rethinkPresetName(storedName: String): String =
    RethinkDnsPreset.fromStoredName(storedName)?.let { stringResource(it.labelRes()) } ?: storedName

@Composable
private fun rethinkPresetLabel(uiState: DnsSettingsUiState): String =
    if (uiState.isRethinkDnsConnected) rethinkPresetName(connectedDnsDisplayName(uiState.connectedDnsName)) else ""

internal fun RethinkDnsPreset.labelRes(): Int = when (this) {
    RethinkDnsPreset.Default -> R.string.rethink_preset_default
    RethinkDnsPreset.Family -> R.string.rethink_preset_family
    RethinkDnsPreset.Piracy -> R.string.rethink_preset_piracy
    RethinkDnsPreset.SocialMedia -> R.string.rethink_preset_social
    RethinkDnsPreset.Security -> R.string.rethink_preset_security
    RethinkDnsPreset.Privacy -> R.string.rethink_preset_privacy
    RethinkDnsPreset.Custom -> R.string.rethink_preset_custom
}

internal fun RethinkDnsPreset.descriptionRes(): Int = when (this) {
    RethinkDnsPreset.Default -> R.string.rethink_preset_default_desc
    RethinkDnsPreset.Family -> R.string.rethink_preset_family_desc
    RethinkDnsPreset.Piracy -> R.string.rethink_preset_piracy_desc
    RethinkDnsPreset.SocialMedia -> R.string.rethink_preset_social_desc
    RethinkDnsPreset.Security -> R.string.rethink_preset_security_desc
    RethinkDnsPreset.Privacy -> R.string.rethink_preset_privacy_desc
    RethinkDnsPreset.Custom -> R.string.rethink_preset_custom_desc
}
