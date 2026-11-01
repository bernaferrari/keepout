/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.firewall

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R

@Composable
internal fun androidCustomRulesStrings(): RethinkCustomRulesStrings {
    val portFormat = stringResource(R.string.custom_rule_port_format)
    return androidCustomRulesStrings(port = { portFormat.format(it) })
}

@Composable
private fun androidCustomRulesStrings(port: (Int) -> String): RethinkCustomRulesStrings {
    val countFormat = stringResource(R.string.rules_tab_count)
    val onlyApp = stringResource(R.string.rules_edit_only_app)
    return RethinkCustomRulesStrings(
        port = port,
        domains = stringResource(R.string.rules_tab_domains),
        ips = stringResource(R.string.rules_tab_ips),
        withCount = { label, count -> countFormat.format(label, count) },
        searchDomains = stringResource(R.string.rules_search_domains),
        searchIps = stringResource(R.string.rules_search_ips),
        clearSearch = stringResource(R.string.cd_clear_search),
        loading = stringResource(R.string.lbl_loading),
        noIpRules = stringResource(R.string.ci_no_rules_text),
        noDomainRules = stringResource(R.string.cd_no_rules_text),
        add = stringResource(R.string.add_rule_title),
        cancel = stringResource(R.string.lbl_cancel),
        delete = stringResource(R.string.lbl_delete),
        block = stringResource(R.string.ci_block),
        trust = stringResource(R.string.ci_trust_rule),
        bypass = stringResource(R.string.firewall_status_whitelisted),
        noRule = stringResource(R.string.ci_no_rule),
        uid = { value -> "UID $value" },
        scopeAll = stringResource(R.string.rules_scope_all),
        scopeAllApps = stringResource(R.string.rules_scope_all_apps),
        scopeChooseApp = stringResource(R.string.add_rule_choose_app),
        allAppsHeader = stringResource(R.string.lbl_all_apps),
        deleteRuleTitle = stringResource(R.string.custom_rule_delete_title),
        deleteSelectedTitle = { count -> pluralStringResource(R.plurals.custom_rules_delete_selected_title, count, count) },
        selectedCount = { count -> pluralStringResource(R.plurals.custom_rules_selected_count, count, count) },
        clearSelection = stringResource(R.string.custom_rules_clear_selection),
        select = stringResource(R.string.custom_rules_select),
        deleteRule = stringResource(R.string.custom_rules_delete_rule),
        action = stringResource(R.string.lbl_action),
        onlyApp = { app -> onlyApp.format(app) },
    )
}
