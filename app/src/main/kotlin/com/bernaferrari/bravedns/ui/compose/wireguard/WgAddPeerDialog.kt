/*
 * Copyright 2023 RethinkDNS and its authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package com.bernaferrari.bravedns.ui.compose.wireguard

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.ui.compose.wireguard.RethinkWireguardPeerEditor
import com.bernaferrari.bravedns.ui.compose.wireguard.RethinkWireguardPeerEditorStrings
import com.bernaferrari.bravedns.ui.compose.wireguard.RethinkWireguardPeerState
import com.bernaferrari.bravedns.ui.compose.wireguard.RethinkWireguardDialog
import com.bernaferrari.bravedns.ui.compose.wireguard.RethinkWireguardDialogColumn
import com.bernaferrari.bravedns.util.UIUtils.getDurationInHumanReadableFormat
import com.bernaferrari.bravedns.util.Utilities
import com.bernaferrari.bravedns.wireguard.Peer
import com.bernaferrari.bravedns.wireguard.util.ErrorMessages
import Logger

/** Android validation, persistence, and toast adapter for the shared WireGuard peer editor. */
@Composable
fun WgAddPeerDialog(
    configId: Int,
    wgPeer: Peer?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val draft: WgPeerEditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "wg-peer:$configId:${wgPeer?.getPublicKey()?.base64() ?: "new"}",
    )
    androidx.compose.runtime.LaunchedEffect(draft, wgPeer) { draft.initialize(wgPeer) }
    val dismiss = { if (!draft.saving) { draft.reset(); onDismiss() } }
    androidx.activity.compose.BackHandler(enabled = draft.saving) { }
    androidx.compose.runtime.LaunchedEffect(draft.saved, draft.error) {
        if (draft.saved) dismiss()
        draft.error?.let { error ->
            draft.error = null
            Logger.e(Logger.LOG_TAG_UI, "Unable to save WireGuard peer", error)
            Utilities.showToastUiCentered(context, ErrorMessages[context, error], Toast.LENGTH_SHORT)
        }
    }
    RethinkWireguardDialog(onDismissRequest = dismiss) {
        RethinkWireguardDialogColumn(scrollable = true) {
            RethinkWireguardPeerEditor(
                state = draft.state,
                onStateChange = draft::edit,
                enabled = !draft.saving,
                strings = RethinkWireguardPeerEditorStrings(
                    title = stringResource(R.string.add_peer),
                    publicKey = stringResource(R.string.lbl_public_key),
                    presharedKey = stringResource(R.string.lbl_preshared_key),
                    persistentKeepalive = stringResource(R.string.lbl_persistent_keepalive),
                    endpoint = stringResource(R.string.parse_error_inet_endpoint),
                    allowedIps = stringResource(R.string.lbl_allowed_ips),
                    save = stringResource(R.string.lbl_save),
                    dismiss = stringResource(R.string.lbl_dismiss),
                ),
                keepaliveHint = { value -> value.toIntOrNull()?.let { getDurationInHumanReadableFormat(context, it) } },
                onSave = { draft.save(configId) },
                onDismiss = dismiss,
            )
        }
    }
}
