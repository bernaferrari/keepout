package com.bernaferrari.bravedns.service

import android.content.Context
import android.net.VpnService
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.bravedns.database.WgHopMap
import com.bernaferrari.bravedns.database.WgHopMapRepository
import com.bernaferrari.bravedns.service.ProxyManager.ID_WG_BASE
import com.bernaferrari.bravedns.ui.HomeScreenActivity
import com.bernaferrari.bravedns.wireguard.Config
import com.bernaferrari.bravedns.wireguard.KeyPair
import com.bernaferrari.bravedns.wireguard.WgHopManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WireguardHopActivationInstrumentedTest : KoinComponent {
    private val hops by inject<WgHopMapRepository>()

    @Test
    fun inactiveHopIsAddedWhileItsSourceIsBeingEnabled() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assumeTrue("Run on a test device with VPN consent already granted", VpnService.prepare(context) == null)
        assumeFalse("Requires a stopped tunnel on the test device", VpnController.hasTunnel())

        ActivityScenario.launch(HomeScreenActivity::class.java).use {
            var source: Int? = null
            var hop: Int? = null
            try {
                VpnController.start(context)
                withTimeout(20_000) {
                    while (!VpnController.hasTunnel()) delay(50)
                }
                val sourceId = addTestConfig(1)
                source = sourceId
                val hopId = addTestConfig(2)
                hop = hopId
                val sourceKey = ID_WG_BASE + sourceId
                val hopKey = ID_WG_BASE + hopId
                hops.insert(WgHopMap(0, sourceKey, hopKey, true, ""))
                WgHopManager.load(forceRefresh = true)
                check(!WireguardManager.isConfigActive(hopKey))

                withTimeout(10_000) {
                    WireguardManager.enableConfig(checkNotNull(WireguardManager.getConfigFilesById(sourceId)))
                }

                assertTrue(WireguardManager.isConfigActive(sourceKey))
                assertTrue(WireguardManager.isConfigActive(hopKey))
                assertNotNull(VpnController.getProxyStatusById(sourceKey).first)
                assertNotNull(VpnController.getProxyStatusById(hopKey).first)
            } finally {
                if (source != null && hop != null) WgHopManager.removeHop(source, hop)
                source?.let { WireguardManager.deleteConfig(it) }
                hop?.let { WireguardManager.deleteConfig(it) }
                VpnController.stop("instrumented test finished", context)
            }
        }
    }

    private suspend fun addTestConfig(host: Int): Int {
        val name = "activation-test-${UUID.randomUUID()}"
        val config = Config.parse(
            """
            [Interface]
            PrivateKey = ${KeyPair().getPrivateKey().base64()}
            Address = 198.18.1.$host/32

            [Peer]
            PublicKey = ${KeyPair().getPublicKey().base64()}
            AllowedIPs = 198.18.2.0/24
            Endpoint = 192.0.2.$host:51820
            """.trimIndent(),
        )
        WireguardManager.addConfig(config, name)
        return WireguardManager.getAllMappings().single { it.name == name }.id
    }
}
