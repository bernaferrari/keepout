package com.bernaferrari.bravedns.ui.compose.wireguard

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.bernaferrari.bravedns.wireguard.Peer
import com.bernaferrari.bravedns.wireguard.WgCrypto
import com.bernaferrari.bravedns.wireguard.WgKeyHandle
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class WgPeerEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkObject(WgCrypto)
        every { WgCrypto.parsePublicKey(any()) } returns mockk<WgKeyHandle>(relaxed = true)
    }
    @After fun cleanup() { unmockkAll(); Dispatchers.resetMain() }

    @Test fun repeatedSubmissionPersistsOnceAndFreezesDraftUntilDone() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        val persist = mockk<suspend (Int, Peer?, Peer) -> Unit>()
        coEvery { persist(any(), any(), any()) } coAnswers { release.await() }
        val draft = WgPeerEditorViewModel(dispatcher, persist)
        draft.initialize(null)
        val submitted = RethinkWireguardPeerState(publicKey = "key", endpoint = "example.test:51820")
        draft.edit(submitted)
        draft.save(7)
        draft.save(7)
        runCurrent()
        draft.edit(submitted.copy(endpoint = "other.test:51820"))
        draft.reset()
        assertEquals(submitted, draft.state)
        assertTrue(draft.saving)
        release.complete(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) { persist(7, null, any()) }
        assertTrue(draft.saved)
    }

    @Test fun recreatedOwnerUsesTheRetainedDraftAndReopeningStartsFresh() {
        val store = ViewModelStore()
        val first = ViewModelProvider.create(store = store)[WgPeerEditorViewModel::class]
        first.initialize(null)
        first.edit(RethinkWireguardPeerState(publicKey = "draft key", presharedKey = "secret"))
        val recreated = ViewModelProvider.create(store = store)[WgPeerEditorViewModel::class]
        recreated.initialize(null)
        assertSame(first, recreated)
        assertEquals("draft key", recreated.state.publicKey)
        assertEquals("secret", recreated.state.presharedKey)
        recreated.reset()
        recreated.initialize(null)
        assertEquals(RethinkWireguardPeerState(), recreated.state)
        store.clear()
    }

    @Test fun failedPeerSaveKeepsDraftAndAllowsRetry() = runTest(dispatcher) {
        var fail = true
        val draft = WgPeerEditorViewModel(dispatcher) { _, _, _ -> if (fail) throw java.io.IOException("disk full") }
        draft.initialize(null)
        draft.edit(RethinkWireguardPeerState(publicKey = "key"))
        draft.save(7)
        advanceUntilIdle()
        assertTrue(draft.error is java.io.IOException)
        assertFalse(draft.saved)
        assertFalse(draft.saving)
        assertEquals("key", draft.state.publicKey)
        fail = false
        draft.save(7)
        advanceUntilIdle()
        assertTrue(draft.saved)
    }
}
