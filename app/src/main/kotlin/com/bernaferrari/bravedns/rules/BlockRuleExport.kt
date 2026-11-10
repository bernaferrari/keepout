package com.bernaferrari.bravedns.rules

import com.bernaferrari.bravedns.database.AppDatabase
import com.bernaferrari.bravedns.service.DomainRulesManager
import com.bernaferrari.bravedns.service.IpRulesManager
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRulesTab

internal suspend fun exportBlockRules(database: AppDatabase, uid: Int, tab: RethinkRulesTab): String {
    val entries = when (tab) {
        RethinkRulesTab.DOMAIN -> database.customDomainEndpointDAO().getRulesCursor().asSequence()
            .filter { it.uid == uid && it.deletedTs == 0L && it.status == DomainRulesManager.Status.BLOCK.id }
            .map { it.domain }
        RethinkRulesTab.IP -> database.customIpEndpointDao().getRulesByUid(uid).asSequence()
            .filter { it.isActive && it.status == IpRulesManager.IpRuleStatus.BLOCK.id }
            .map { if (it.port > 0) "[${it.ipAddress}]:${it.port}" else it.ipAddress }
    }
    return entries.distinct().sorted().joinToString("\n", postfix = "\n")
}
