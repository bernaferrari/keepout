/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.apps

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkAppSelectionAction
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallAppListStrings
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallAppStatus
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkFirewallFilter

/** Every label on the Apps list, resolved from Android resources. */
@Composable
internal fun androidFirewallAppListStrings() = RethinkFirewallAppListStrings(
    sortTitle = stringResource(R.string.port_sort_title),
    uidLabel = { stringResource(R.string.port_uid_number, it) },
    sortLabels = listOf(stringResource(R.string.port_sort_name), stringResource(R.string.port_sort_package), stringResource(R.string.port_sort_uid)),
    title = stringResource(R.string.apps_info_title),
    searchHint = { count -> stringResource(R.string.search_apps_count_placeholder, count) },
    searchHintLoading = stringResource(R.string.apps_search_hint),
    loading = stringResource(R.string.lbl_loading),
    refresh = stringResource(R.string.cd_refresh),
    filter = stringResource(R.string.cd_filter),
    moreOptions = stringResource(R.string.cd_more),
    more = stringResource(R.string.app_more),
    clearSearch = stringResource(R.string.cd_clear_search),
    emptyTitle = stringResource(R.string.fapps_empty_title),
    emptyDescription = stringResource(R.string.fapps_empty_subtitle),
    view = stringResource(R.string.lbl_view),
    installed = stringResource(R.string.fapps_filter_parent_installed),
    system = stringResource(R.string.fapps_filter_parent_system),
    all = stringResource(R.string.lbl_all),
    status = stringResource(R.string.lbl_status),
    categories = stringResource(R.string.fapps_filter_categories_heading),
    clear = stringResource(R.string.fapps_filter_clear_btn),
    cancel = stringResource(R.string.lbl_cancel),
    close = stringResource(R.string.lbl_done),
    proxy = stringResource(R.string.lbl_proxy),
    // State first ("Wi‑Fi: blocked"), the double-tap action separately ("Allow on Wi‑Fi").
    wifiStateLabel = { isBlocked -> stringResource(if (isBlocked) R.string.app_state_wifi_blocked else R.string.app_state_wifi_allowed) },
    mobileStateLabel = { isBlocked -> stringResource(if (isBlocked) R.string.app_state_mobile_blocked else R.string.app_state_mobile_allowed) },
    wifiActionLabel = { isBlocked -> stringResource(if (isBlocked) R.string.apps_action_allow_on_wifi else R.string.apps_action_block_on_wifi) },
    mobileActionLabel = { isBlocked -> stringResource(if (isBlocked) R.string.apps_action_allow_on_mobile else R.string.apps_action_block_on_mobile) },
    select = stringResource(R.string.apps_select),
    exitSelect = stringResource(R.string.apps_exit_select),
    selectAll = stringResource(R.string.lbl_select_all),
    selectedCount = { count -> pluralStringResource(R.plurals.apps_selected_count, count, count) },
    selectionActionLabel = { action -> selectionActionLabel(action) },
    selectionConfirmTitle = { action, count -> selectionConfirmTitle(action, count) },
    selectionNetworkMessage = stringResource(R.string.apps_confirm_network_message),
    selectionModeMessage = stringResource(R.string.apps_confirm_mode_message),
    alsoAffects = { names -> stringResource(R.string.apps_also_affects, names) },
    filterLabel = { filter -> filterLabel(filter) },
    statusLabel = { status -> statusBadge(status) },
)

/** The Select-mode menu item for [action]; a mode reads as App detail names it. */
@Composable
private fun selectionActionLabel(action: RethinkAppSelectionAction): String = when (action) {
    RethinkAppSelectionAction.BlockWifi -> stringResource(R.string.apps_action_block_wifi)
    RethinkAppSelectionAction.BlockMobile -> stringResource(R.string.apps_action_block_mobile)
    RethinkAppSelectionAction.AllowAll -> stringResource(R.string.apps_action_allow_all)
    RethinkAppSelectionAction.AllowWifi -> stringResource(R.string.apps_action_allow_wifi)
    RethinkAppSelectionAction.AllowMobile -> stringResource(R.string.apps_action_allow_mobile)
    RethinkAppSelectionAction.TrustedOnly -> stringResource(R.string.ada_app_isolate)
    RethinkAppSelectionAction.SkipUniversalRules -> stringResource(R.string.ada_app_bypass_univ)
    RethinkAppSelectionAction.SkipAllFiltering -> stringResource(R.string.ada_app_bypass_dns_firewall)
    RethinkAppSelectionAction.Exclude -> stringResource(R.string.ada_app_exclude)
}

/** "Block Wi‑Fi for 3 apps?": network actions have their own plurals; modes share one, by name. */
@Composable
private fun selectionConfirmTitle(action: RethinkAppSelectionAction, count: Int): String = when (action) {
    RethinkAppSelectionAction.BlockWifi -> pluralStringResource(R.plurals.apps_confirm_block_wifi, count, count)
    RethinkAppSelectionAction.BlockMobile -> pluralStringResource(R.plurals.apps_confirm_block_mobile, count, count)
    RethinkAppSelectionAction.AllowAll -> pluralStringResource(R.plurals.apps_confirm_allow_all, count, count)
    RethinkAppSelectionAction.AllowWifi -> pluralStringResource(R.plurals.apps_confirm_allow_wifi, count, count)
    RethinkAppSelectionAction.AllowMobile -> pluralStringResource(R.plurals.apps_confirm_allow_mobile, count, count)
    RethinkAppSelectionAction.TrustedOnly,
    RethinkAppSelectionAction.SkipUniversalRules,
    RethinkAppSelectionAction.SkipAllFiltering,
    RethinkAppSelectionAction.Exclude ->
        LocalResources.current.getQuantityString(R.plurals.apps_confirm_mode, count, count, selectionActionLabel(action))
}

@Composable
private fun filterLabel(filter: RethinkFirewallFilter): String = when (filter) {
    RethinkFirewallFilter.All -> stringResource(R.string.lbl_all)
    RethinkFirewallFilter.Allowed -> stringResource(R.string.lbl_allowed)
    RethinkFirewallFilter.Blocked -> stringResource(R.string.lbl_blocked)
    RethinkFirewallFilter.Bypass -> stringResource(R.string.fapps_firewall_filter_bypass_universal)
    RethinkFirewallFilter.SkipAllFiltering -> stringResource(R.string.ada_app_status_bypass_dns_firewall)
    RethinkFirewallFilter.Excluded -> stringResource(R.string.fapps_firewall_filter_excluded)
    RethinkFirewallFilter.Lockdown -> stringResource(R.string.fapps_firewall_filter_isolate)
    RethinkFirewallFilter.BlockedWifi -> stringResource(R.string.ada_app_status_block_wifi)
    RethinkFirewallFilter.BlockedMobile -> stringResource(R.string.ada_app_status_block_md)
}

/** A row's badge, the same words as its filter. The red switches already say blocked: no badge. */
@Composable
private fun statusBadge(status: RethinkFirewallAppStatus): String = when (status) {
    RethinkFirewallAppStatus.Allowed, RethinkFirewallAppStatus.Blocked -> ""
    RethinkFirewallAppStatus.Bypass -> filterLabel(RethinkFirewallFilter.Bypass)
    RethinkFirewallAppStatus.SkipAllFiltering -> filterLabel(RethinkFirewallFilter.SkipAllFiltering)
    RethinkFirewallAppStatus.Excluded -> filterLabel(RethinkFirewallFilter.Excluded)
    RethinkFirewallAppStatus.Lockdown -> filterLabel(RethinkFirewallFilter.Lockdown)
    RethinkFirewallAppStatus.Unknown -> stringResource(R.string.network_log_app_name_unknown)
}
