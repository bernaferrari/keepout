package com.bernaferrari.bravedns.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.bernaferrari.bravedns.ui.compose.wireguard.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WireguardEditingTest {
    @Test fun interfaceCannotBeEditedOrDismissedWhileSaving() = runSkikoComposeUiTest(size = Size(700f, 1200f)) {
        setContent {
            MaterialTheme {
                RethinkWireguardEditor(
                    RethinkWireguardEditorState(interfaceName = "Office"),
                    RethinkWireguardEditorStrings("Editor", "Configuration", "Setup", "Network", "Advanced",
                        "Name", "Addresses", "DNS servers", "Private key", "Public key", "Port", "MTU",
                        "Generate keys", "Copy key", "Cancel", "Save"),
                    0.dp, {}, {}, {}, {}, {}, saveEnabled = false, editingEnabled = false,
                )
            }
        }
        onNodeWithText("Name").assertIsNotEnabled()
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithContentDescription("Generate keys").performScrollTo().assertIsNotEnabled()
    }

    @Test fun peerFieldsAndActionsFollowHostSavingState() = runSkikoComposeUiTest(size = Size(700f, 1200f)) {
        val enabled = mutableStateOf(false)
        setContent {
            MaterialTheme {
                RethinkWireguardPeerEditor(
                    state = RethinkWireguardPeerState(publicKey = "draft"), onStateChange = {},
                    strings = RethinkWireguardPeerEditorStrings("Peer", "Public key", "Preshared key", "Keepalive",
                        "Endpoint", "Allowed IPs", "Save", "Dismiss"),
                    onSave = {}, onDismiss = {}, enabled = enabled.value,
                )
            }
        }
        onNodeWithText("Public key").assertIsNotEnabled()
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Dismiss").assertIsNotEnabled()
        runOnIdle { enabled.value = true }
        onNodeWithText("Public key").assertIsEnabled()
        onNodeWithText("Save").assertIsEnabled()
    }
}
