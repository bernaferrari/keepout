package com.bernaferrari.bravedns.ui.compose.wireguard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.bravedns.service.WireguardManager
import com.bernaferrari.bravedns.wireguard.Peer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Peer secrets stay in the entry's ViewModelStore, never in a saved-state bundle. */
internal class WgPeerEditorViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val persist: suspend (Int, Peer?, Peer) -> Unit = { id, previous, peer ->
        if (previous == null) WireguardManager.addPeer(id, peer)
        else WireguardManager.replacePeer(id, previous, peer)
    },
) : ViewModel() {
    var state by mutableStateOf(RethinkWireguardPeerState())
        private set
    private var initialized = false
    private var original: Peer? = null
    var saving by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set
    var error by mutableStateOf<Exception?>(null)

    fun initialize(peer: Peer?) {
        if (initialized) return
        original = peer
        state = RethinkWireguardPeerState(
            publicKey = peer?.getPublicKey()?.base64().orEmpty(),
            presharedKey = peer?.getPreSharedKey()?.base64().orEmpty(),
            allowedIps = peer?.getAllowedIps()?.joinToString { it.toString() }.orEmpty(),
            endpoint = peer?.getEndpoint()?.toString().orEmpty(),
            persistentKeepalive = peer?.persistentKeepalive?.toString().orEmpty(),
        )
        initialized = true
    }

    fun edit(next: RethinkWireguardPeerState) { if (!saving && !saved) state = next }

    fun reset() {
        if (saving) return
        initialized = false
        original = null
        state = RethinkWireguardPeerState()
        saved = false
        error = null
    }

    fun save(configId: Int) {
        if (saving || !initialized || saved) return
        saving = true
        val snapshot = state
        viewModelScope.launch {
            try {
                withContext(dispatcher) { persist(configId, original, snapshot.buildPeer()) }
                saved = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: com.bernaferrari.bravedns.wireguard.BadConfigException) {
                error = failure
            } catch (failure: java.io.IOException) {
                error = failure
            } catch (failure: androidx.sqlite.SQLiteException) {
                error = failure
            } catch (failure: IllegalStateException) {
                error = failure
            } catch (failure: SecurityException) {
                error = failure
            } finally { saving = false }
        }
    }
}

internal fun RethinkWireguardPeerState.buildPeer(): Peer {
    val builder = Peer.Builder()
    if (allowedIps.isNotEmpty()) builder.parseAllowedIPs(allowedIps)
    if (endpoint.isNotEmpty()) builder.parseEndpoint(endpoint)
    if (persistentKeepalive.isNotEmpty()) builder.parsePersistentKeepalive(persistentKeepalive)
    if (presharedKey.isNotEmpty()) builder.parsePreSharedKey(presharedKey)
    if (publicKey.isNotEmpty()) builder.parsePublicKey(publicKey)
    return builder.build()
}
