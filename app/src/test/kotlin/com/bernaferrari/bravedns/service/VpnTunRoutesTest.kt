package com.bernaferrari.bravedns.service

import com.bernaferrari.bravedns.data.AppConfig
import com.bernaferrari.bravedns.util.InternetProtocol
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class VpnTunRoutesTest {
    @Test
    fun forcedProtocolsSetOnlyTheRequestedAddressFamilies() {
        val config = mockk<AppConfig>()
        val routes = VpnTunRoutes(mockk(relaxed = true), config, mockk())
        val network = BraveVPNService.Networks(null, BraveVPNService.OverlayNetworks())
        val cases = mapOf(
            InternetProtocol.IPv4 to (true to false),
            InternetProtocol.IPv6 to (false to true),
            InternetProtocol.ALWAYSv46 to (true to true),
        )
        cases.forEach { (protocol, expected) ->
            every { config.getInternetProtocol() } returns protocol
            assertEquals(expected, routes.determineRoutes(network))
        }
    }
}
