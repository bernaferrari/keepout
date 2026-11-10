package com.bernaferrari.bravedns.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import inet.ipaddr.IPAddressString
import com.bernaferrari.bravedns.service.IpRulesManager.IpRuleStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IpRulesReloadInstrumentedTest {
    @Test
    fun nativeTriePreservesExactSubnetAndPortPrecedenceAfterReload() = runBlocking {
        val uid = 29991
        val ip = "198.18.0.7"
        try {
            IpRulesManager.addIpRule(uid, IPAddressString("198.18.0.0/24").address, null, IpRuleStatus.BLOCK, "", "")
            IpRulesManager.addIpRule(uid, IPAddressString(ip).address, null, IpRuleStatus.TRUST, "", "")
            IpRulesManager.addIpRule(uid, IPAddressString(ip).address, 443, IpRuleStatus.BLOCK, "", "")

            repeat(2) {
                assertEquals(IpRuleStatus.BLOCK, IpRulesManager.hasRule(uid, ip, 443))
                assertEquals(IpRuleStatus.TRUST, IpRulesManager.hasRule(uid, ip, 80))
                assertEquals(IpRuleStatus.BLOCK, IpRulesManager.hasRule(uid, "198.18.0.8", 80))
                assertEquals(IpRuleStatus.NONE, IpRulesManager.hasRule(uid + 1, ip, 443))
                IpRulesManager.load()
            }
        } finally {
            IpRulesManager.deleteRulesByUid(uid)
        }
    }
}
