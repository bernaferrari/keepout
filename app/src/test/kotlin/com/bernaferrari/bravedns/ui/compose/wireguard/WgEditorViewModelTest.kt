package com.bernaferrari.bravedns.ui.compose.wireguard

import com.bernaferrari.bravedns.wireguard.WgCrypto
import com.bernaferrari.bravedns.wireguard.WgKeyHandle
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WgEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkObject(WgCrypto)
        every { WgCrypto.parsePrivateKey(any()) } returns mockk<WgKeyHandle>(relaxed = true)
    }
    @After fun cleanup() { unmockkAll(); Dispatchers.resetMain() }

    @Test fun malformedPrivateKeyClearsStalePublicKey() {
        every { WgCrypto.parsePrivateKey("invalid") } throws IllegalArgumentException("Invalid key")
        val edited = RethinkWireguardEditorState(publicKey = "old-public").withPrivateKey("invalid")
        assertEquals("invalid", edited.privateKey)
        assertEquals("", edited.publicKey)
    }

    @Test fun failedSavePreservesDraftAndCanBeRetriedWithoutDuplicateWrites() = runTest(dispatcher) {
        val save = mockk<suspend (Int, String, com.bernaferrari.bravedns.wireguard.WgInterface) -> com.bernaferrari.bravedns.wireguard.Config?>()
        val draft = WgEditorViewModel(dispatcher, save)
        draft.loaded = true
        draft.state = RethinkWireguardEditorState(interfaceName = "Office", privateKey = "key")
        val snapshot = draft.state
        coEvery { save(any(), any(), any()) } throws IOException("disk full")
        draft.save(7)
        draft.save(7)
        advanceUntilIdle()
        coVerify(exactly = 1) { save(7, "Office", any()) }
        assertSame(snapshot, draft.state)
        assertTrue(draft.error is IOException)
        assertFalse(draft.saved)
        assertFalse(draft.saving)
        draft.error = null
        coEvery { save(any(), any(), any()) } returns mockk(relaxed = true)
        draft.save(7)
        advanceUntilIdle()
        assertTrue(draft.saved)
        assertEquals(snapshot, draft.initial)
    }
}
