package com.bernaferrari.bravedns.wireguard

import com.bernaferrari.bravedns.ui.compose.wireguard.*
import kotlin.test.*

class WireguardEditorDraftTest {
    @Test fun editingVisibleFieldsPreservesEveryImportedExtension() {
        val original = WgInterface.parse(listOf(
            "PrivateKey = old", "Address = 10.0.0.1/32", "DNS = 9.9.9.9, old.test",
            "ExcludedApplications = example.app", "ClientID = client",
            "JC = 3", "JMin = 10", "JMax = 20", "S1 = 11", "S2 = 12",
            "H1 = 1", "H2 = 2", "H3 = 3", "H4 = 4",
        ))
        val edited = RethinkWireguardEditorState(privateKey = "new", addresses = "10.0.0.2/32",
            dnsServers = "1.1.1.1, new.test").buildInterface(original)
        assertEquals(original.getAmzProps(), edited.getAmzProps())
        assertEquals(original.getClientId(), edited.getClientId())
        assertEquals(original.excludedApplications, edited.excludedApplications)
        assertEquals(setOf("1.1.1.1"), edited.dnsServers)
        assertEquals(setOf("new.test"), edited.dnsSearchDomains)
        assertEquals(setOf("9.9.9.9"), original.dnsServers)
        assertEquals(edited, WgInterface.parse(edited.toWgQuickString().lines().filter { it.isNotBlank() }))
    }

    @Test fun includedApplicationsAndBlankDnsSurviveEditing() {
        val original = WgInterface.parse(listOf("PrivateKey = key", "IncludedApplications = example.app"))
        val edited = RethinkWireguardEditorState(privateKey = "key").buildInterface(original)
        assertEquals(original.includedApplications, edited.includedApplications)
        assertTrue(edited.dnsServers.isEmpty())
        assertFalse(edited.toWgQuickString().contains("DNS ="))
    }

    @Test fun publicKeyFollowsPastedPrivateKeyAndClearsWhenEmpty() {
        val state = RethinkWireguardEditorState(privateKey = "old", publicKey = "stale")
        val edited = state.withPrivateKey("replacement")
        assertEquals(WgCrypto.parsePrivateKey("replacement").mult().base64(), edited.publicKey)
        assertEquals("", edited.withPrivateKey("").publicKey)
    }
}
