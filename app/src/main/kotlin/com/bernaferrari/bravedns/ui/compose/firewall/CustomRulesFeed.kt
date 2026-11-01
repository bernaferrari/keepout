/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.firewall

import androidx.compose.runtime.Composable
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.bernaferrari.bravedns.database.CustomDomain
import com.bernaferrari.bravedns.database.CustomIp
import com.bernaferrari.bravedns.service.DomainRulesManager
import com.bernaferrari.bravedns.service.IpRulesManager
import com.bernaferrari.bravedns.ui.compose.theme.RethinkFirewallRuleStatus
import com.bernaferrari.bravedns.util.Constants.Companion.UNSPECIFIED_PORT
import com.bernaferrari.bravedns.viewmodel.CustomDomainViewModel
import com.bernaferrari.bravedns.viewmodel.CustomIpViewModel

/**
 * The rules list for [tab]: every rule when [everyRule], else the scope the view models were set
 * to. Rows name their owner app from [ownerNames] once loaded.
 */
@Composable
internal fun rulesFeed(
    tab: RethinkRulesTab,
    everyRule: Boolean,
    ipViewModel: CustomIpViewModel,
    domainViewModel: CustomDomainViewModel,
    ownerNames: Map<Int, String>,
): RethinkFirewallRulesFeed = when (tab) {
    RethinkRulesTab.IP -> {
        val rules = if (everyRule) ipViewModel.everyIpRule else ipViewModel.customIpDetails
        AndroidRulesFeed(rules.collectAsLazyPagingItems()) { it.toRethinkRule(ownerNames[it.uid]) }
    }
    RethinkRulesTab.DOMAIN -> {
        val rules = if (everyRule) domainViewModel.everyDomainRule else domainViewModel.customDomains
        AndroidRulesFeed(rules.collectAsLazyPagingItems()) { it.toRethinkRule(ownerNames[it.uid]) }
    }
}

private class AndroidRulesFeed<T : Any>(
    private val items: LazyPagingItems<T>,
    private val mapper: (T) -> RethinkFirewallRule,
) : RethinkFirewallRulesFeed {
    override val itemCount: Int get() = items.itemCount
    override val isLoading: Boolean get() = items.loadState.refresh is LoadState.Loading

    override fun get(index: Int): RethinkFirewallRule? = items[index]?.let(mapper)
}

private fun CustomIp.toRethinkRule(ownerName: String?): RethinkFirewallRule = RethinkFirewallRule(
    id = "ip:$uid:$ipAddress:$port:$protocol",
    uid = uid,
    value = ipAddress,
    port = port.takeUnless { it == UNSPECIFIED_PORT },
    tab = RethinkRulesTab.IP,
    status = when (IpRulesManager.IpRuleStatus.getStatus(status)) {
        IpRulesManager.IpRuleStatus.BLOCK -> RethinkFirewallRuleStatus.Block
        IpRulesManager.IpRuleStatus.TRUST -> RethinkFirewallRuleStatus.Trust
        IpRulesManager.IpRuleStatus.BYPASS_UNIVERSAL -> RethinkFirewallRuleStatus.Bypass
        IpRulesManager.IpRuleStatus.NONE -> RethinkFirewallRuleStatus.None
    },
    ownerName = ownerName,
)

private fun CustomDomain.toRethinkRule(ownerName: String?): RethinkFirewallRule = RethinkFirewallRule(
    id = "domain:$uid:$domain",
    uid = uid,
    value = domain,
    tab = RethinkRulesTab.DOMAIN,
    status = when (DomainRulesManager.Status.getStatus(status)) {
        DomainRulesManager.Status.BLOCK -> RethinkFirewallRuleStatus.Block
        DomainRulesManager.Status.TRUST -> RethinkFirewallRuleStatus.Trust
        DomainRulesManager.Status.NONE -> RethinkFirewallRuleStatus.None
    },
    ownerName = ownerName,
)
