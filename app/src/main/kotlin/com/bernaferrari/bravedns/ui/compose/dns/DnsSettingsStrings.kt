/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.dns

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R

/** Every label on DNS and DNS › Advanced, resolved from Android resources. */
@Composable
internal fun androidDnsSettingsStrings() = RethinkDnsSettingsStrings(
    title = stringResource(R.string.lbl_dns),
    refresh = stringResource(R.string.rules_load_failure_reload),
    modesSection = stringResource(R.string.dc_other_dns_heading),
    systemDns = stringResource(R.string.network_dns),
    systemDnsDescription = stringResource(R.string.dns_mode_system_desc),
    customDns = stringResource(R.string.dc_custom_dns_radio),
    customDnsDescription = stringResource(R.string.dns_mode_other_desc),
    rethinkDns = stringResource(R.string.dc_rethink_dns_radio),
    rethinkDnsDescription = stringResource(R.string.dns_choice_rethink_desc),
    smartDns = stringResource(R.string.smart_dns),
    smartDnsDescription = stringResource(R.string.dns_mode_smart_desc),
    connectedDescription = stringResource(R.string.rethink_sky_desc),
    blockSection = stringResource(R.string.dc_block_heading),
    localBlocklists = stringResource(R.string.dc_local_block_heading),
    localBlocklistsDescription = { pluralStringResource(R.plurals.dns_device_on_desc, it, it) },
    localBlocklistsDisabledDescription = stringResource(R.string.dns_device_off_desc),
    enabled = stringResource(R.string.dc_local_block_enabled),
    disabled = stringResource(R.string.lbl_disabled),
    customDownloader = stringResource(R.string.settings_custom_downloader_heading),
    customDownloaderDescription = stringResource(R.string.settings_custom_downloader_desc),
    periodicUpdates = stringResource(R.string.dc_check_update_heading),
    periodicUpdatesDescription = stringResource(R.string.dc_check_update_desc_compact),
    filteringSection = stringResource(R.string.dc_filtering_heading),
    dnsAlg = stringResource(R.string.cd_dns_alg_heading),
    dnsAlgDescription = stringResource(R.string.cd_dns_alg_desc),
    splitDns = stringResource(R.string.cd_split_dns_heading),
    splitDnsDescription = stringResource(R.string.cd_split_dns_desc),
    rulesAsFirewall = stringResource(R.string.cd_treat_dns_rules_firewall_heading),
    rulesAsFirewallDescription = stringResource(R.string.cd_treat_dns_rules_firewall_desc),
    recordTypes = stringResource(R.string.cd_allowed_dns_record_types_heading),
    recordTypesDescription = stringResource(R.string.cd_allowed_dns_record_types_desc),
    auto = stringResource(R.string.dns_record_types_auto_mode_status),
    blockFreeSection = stringResource(R.string.block_free_dns_mode_title),
    blockFreeLabel = { mode -> stringResource(mode.blockFreeLabelRes()) },
    blockFreeDescription = { mode -> stringResource(mode.blockFreeDescriptionRes()) },
    trustedEndpoint = stringResource(R.string.dns_trusted_endpoint),
    trustedEndpointDescription = stringResource(R.string.dns_trusted_endpoint_desc),
    advancedSection = stringResource(R.string.lbl_advanced),
    favicons = stringResource(R.string.dc_dns_website_heading),
    faviconsDescription = stringResource(R.string.dc_dns_website_desc),
    dnsCache = stringResource(R.string.dc_setting_dns_cache_heading),
    dnsCacheDescription = stringResource(R.string.dc_setting_dns_cache_desc),
    proxyDns = stringResource(R.string.dc_proxy_dns_heading),
    proxyDnsDescription = stringResource(R.string.dc_proxy_dns_desc),
    undelegatedDomains = stringResource(R.string.dc_use_sys_dns_undelegated_heading),
    undelegatedDomainsDescription = stringResource(R.string.dc_use_sys_dns_undelegated_desc),
    fallbackDns = stringResource(R.string.use_fallback_dns_to_bypass),
    fallbackDnsDescription = stringResource(R.string.use_fallback_dns_to_bypass_desc),
    preventLeaks = stringResource(R.string.dc_dns_leaks_heading),
    preventLeaksDescription = stringResource(R.string.dc_dns_leaks_desc),
    aboutItem = { stringResource(R.string.cd_about_item, it) },
    inUse = stringResource(R.string.dns_in_use),
    medianLookup = stringResource(R.string.dns_median_lookup),
    recentLookups = stringResource(R.string.dns_recent_lookups),
    noLookupsYet = stringResource(R.string.dns_no_lookups_yet),
    choiceSection = stringResource(R.string.dns_choice_section),
    recommended = stringResource(R.string.dns_choice_recommended),
    rethinkDetail = rethinkDetailFormatter(),
    providerDetail = providerDetailFormatter(),
    smartNote = stringResource(R.string.dns_choice_smart_note),
    heroFailing = stringResource(R.string.dns_hero_failing),
    tryAnotherServer = stringResource(R.string.dns_hero_try_another),
    deviceSection = stringResource(R.string.dns_device_section),
    deviceOn = stringResource(R.string.dns_device_on),
    deviceOff = stringResource(R.string.dns_device_off),
    deviceSetUp = stringResource(R.string.dns_device_set_up),
    deviceTurnOn = stringResource(R.string.dns_device_turn_on),
    deviceChoose = stringResource(R.string.dns_device_choose),
    deviceManage = stringResource(R.string.dns_device_manage),
    advancedDescription = stringResource(R.string.dns_adv_row_desc),
    advancedTitle = stringResource(R.string.dns_adv_title),
    serversSection = stringResource(R.string.dns_adv_servers),
    autoSelect = stringResource(R.string.dns_adv_auto_select),
    autoSelectDescription = stringResource(R.string.dns_adv_auto_select_desc),
    use = stringResource(R.string.dns_adv_use),
    rethinkNetwork = stringResource(R.string.dns_adv_rethink_network),
    rethinkFastest = stringResource(R.string.dns_adv_rethink_fastest),
    rethinkFastestDescription = stringResource(R.string.dns_adv_rethink_fastest_desc),
    rethinkPrivate = stringResource(R.string.dns_adv_rethink_private),
    rethinkPrivateDescription = stringResource(R.string.dns_adv_rethink_private_desc),
    downloadsSection = stringResource(R.string.dns_adv_downloads),
    fallbackSection = stringResource(R.string.dns_adv_fallback),
    fallbackServer = stringResource(R.string.settings_default_dns_heading),
    fallbackServerDescription = stringResource(R.string.settings_default_dns_desc),
    moreSection = stringResource(R.string.dns_adv_more),
)

@Composable
private fun rethinkDetailFormatter(): (String) -> String {
    val template = stringResource(R.string.dns_choice_rethink_detail)
    return { preset -> template.format(preset) }
}

@Composable
private fun providerDetailFormatter(): (String, String) -> String {
    val template = stringResource(R.string.dns_choice_provider_detail)
    return { name, protocol -> template.format(name, protocol) }
}

private fun RethinkBlockFreeDnsMode.blockFreeLabelRes() = when (this) {
    RethinkBlockFreeDnsMode.Auto -> R.string.bfdm_option_auto_label
    RethinkBlockFreeDnsMode.Global -> R.string.bfdm_option_global_label
    RethinkBlockFreeDnsMode.Fallback -> R.string.bfdm_option_fallback_label
}

private fun RethinkBlockFreeDnsMode.blockFreeDescriptionRes() = when (this) {
    RethinkBlockFreeDnsMode.Auto -> R.string.bfdm_option_auto_desc
    RethinkBlockFreeDnsMode.Global -> R.string.bfdm_option_global_desc
    RethinkBlockFreeDnsMode.Fallback -> R.string.bfdm_option_fallback_desc
}
