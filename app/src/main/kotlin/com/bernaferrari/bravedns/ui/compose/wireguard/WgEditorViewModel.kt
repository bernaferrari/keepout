package com.bernaferrari.bravedns.ui.compose.wireguard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import com.bernaferrari.bravedns.service.WireguardManager
import androidx.lifecycle.ViewModel
import com.bernaferrari.bravedns.wireguard.WgInterface

/** Keeps secrets in memory across rotation, never in saved-state bundles. */
internal class WgEditorViewModel(
    private val operationDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    private val saveInterface: suspend (Int, String, WgInterface) -> com.bernaferrari.bravedns.wireguard.Config? =
        { id, name, iface -> WireguardManager.addOrUpdateInterface(id, name, iface) },
) : ViewModel() {
    var state by mutableStateOf(RethinkWireguardEditorState())
        private set

    fun edit(next: RethinkWireguardEditorState) {
        if (!saving && !saved) state = next
    }
    var initial = RethinkWireguardEditorState()
    var original: WgInterface? = null
    var loaded by mutableStateOf(false)
    var saving by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
    var error by mutableStateOf<Exception?>(null)

    fun save(configId: Int) {
        if (!loaded || saving) return
        saving = true
        val snapshot = state
        viewModelScope.launch {
            try {
                withContext(operationDispatcher) {
                    val iface = snapshot.buildInterface(original)
                    checkNotNull(saveInterface(configId, snapshot.interfaceName, iface)) {
                        "WireGuard configuration no longer exists"
                    }
                }
                initial = snapshot
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
            } finally {
                saving = false
            }
        }
    }
}
