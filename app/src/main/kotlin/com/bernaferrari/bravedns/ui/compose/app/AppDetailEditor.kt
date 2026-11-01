/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.app

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import com.bernaferrari.bravedns.database.AppInfo
import com.bernaferrari.bravedns.database.EventSource
import com.bernaferrari.bravedns.database.EventType
import com.bernaferrari.bravedns.database.Severity
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.FirewallManager
import com.bernaferrari.bravedns.service.FirewallManager.ConnectionStatus
import com.bernaferrari.bravedns.service.FirewallManager.FirewallStatus
import com.bernaferrari.bravedns.ui.compose.apps.RethinkAppMode
import com.bernaferrari.bravedns.ui.compose.firewall.launchReversibleRuleMutation
import com.bernaferrari.bravedns.util.Utilities.showToastUiCentered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The messages App detail's edits show: snackbars, their Undo, and the exclude toast. */
internal data class AppDetailCopy(
    val undo: String,
    val excludeUnknownPackage: String,
    val tempAllowed: String,
    val tempEnded: String,
    val modeChanged: (RethinkAppMode) -> String,
)

/**
 * App detail's edits for [uid]: each shows in [state] at once, is written through
 * [FirewallManager], and (but for the Wi‑Fi and mobile data tiles, whose second tap is the undo)
 * offers Undo on the snackbar.
 */
internal class AppDetailEditor(
    private val uid: Int,
    private val state: AppDetailState,
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
    private val eventLogger: EventLogger,
    private val context: Context,
    private val copy: AppDetailCopy,
) {
    fun toggleWifi() = applyFirewallRule(FirewallStatus.NONE, state.connStatus.toggledWifi())

    fun toggleMobile() = applyFirewallRule(FirewallStatus.NONE, state.connStatus.toggledMobile())

    fun changeMode(mode: RethinkAppMode) {
        val (status, connection) = modeChange(state.appStatus, state.connStatus, state.baselineConnStatus, mode) ?: return
        if (state.appStatus == FirewallStatus.NONE) state.baselineConnStatus = state.connStatus
        applyFirewallRule(status, connection, copy.modeChanged(mode))
    }

    fun setTempAllow(enabled: Boolean) = if (enabled) allowTemporarily() else endTemporaryAccess()

    fun setProxyExcluded(excluded: Boolean) {
        state.isProxyExcluded = excluded
        scope.launch(Dispatchers.IO) { FirewallManager.updateIsProxyExcluded(uid, excluded) }
    }

    /**
     * Write [status]/[connection] now (optimistically). With a [message], offer Undo back to what it
     * was; without one just apply.
     */
    private fun applyFirewallRule(status: FirewallStatus, connection: ConnectionStatus, message: String? = null) {
        val app = state.appInfo ?: return
        if (status == FirewallStatus.EXCLUDE && FirewallManager.isUnknownPackage(uid)) {
            showToastUiCentered(context, copy.excludeUnknownPackage, Toast.LENGTH_LONG)
            return
        }
        val previousStatus = state.appStatus
        val previousConnection = state.connStatus
        val previousBaseline = state.baselineConnStatus
        state.show(status, connection)
        val epoch = beginEdit()
        if (message == null) {
            scope.launch {
                state.busy = true
                try {
                    writeFirewallStatus(eventLogger, app, status, connection)
                } finally {
                    state.busy = false
                }
            }
            return
        }
        launchUndoable(epoch, message, apply = {
            writeFirewallStatus(eventLogger, app, status, connection)
            true
        }) {
            state.show(previousStatus, previousConnection)
            state.baselineConnStatus = previousBaseline
            writeFirewallStatus(eventLogger, app, previousStatus, previousConnection, undo = true)
        }
    }

    private fun allowTemporarily() {
        launchUndoable(beginEdit(), copy.tempAllowed, apply = {
            state.tempAllowExpiry = withContext(Dispatchers.IO + NonCancellable) {
                FirewallManager.updateTempAllow(uid, true)
                FirewallManager.tempAllowExpiry(uid)
            }
            state.tempAllowExpiry > 0L
        }) {
            withContext(Dispatchers.IO + NonCancellable) { FirewallManager.updateTempAllow(uid, false) }
            state.tempAllowExpiry = 0L
        }
    }

    private fun endTemporaryAccess() {
        beginEdit()
        scope.launch {
            state.busy = true
            withContext(Dispatchers.IO + NonCancellable) { FirewallManager.updateTempAllow(uid, false) }
            state.tempAllowExpiry = 0L
            state.busy = false
            snackbar.showSnackbar(copy.tempEnded)
        }
    }

    /** A new edit supersedes the last one: its snackbar goes, and its Undo no longer applies. */
    private fun beginEdit(): Int {
        state.epoch += 1
        snackbar.currentSnackbarData?.dismiss()
        return state.epoch
    }

    private fun launchUndoable(epoch: Int, message: String, apply: suspend () -> Boolean, undo: suspend () -> Unit) {
        scope.launchReversibleRuleMutation(
            snackbarHostState = snackbar,
            editedMessage = message,
            undoLabel = copy.undo,
            epoch = epoch,
            currentEpoch = { state.epoch },
            setApplying = { state.busy = it },
            apply = apply,
            undo = undo,
        )
    }
}

/** Persist one firewall edit (or its undo) and log it. */
private suspend fun writeFirewallStatus(
    eventLogger: EventLogger,
    app: AppInfo,
    status: FirewallStatus,
    connection: ConnectionStatus,
    undo: Boolean = false,
) {
    withContext(Dispatchers.IO + NonCancellable) { FirewallManager.updateFirewallStatus(app.uid, status, connection) }
    eventLogger.log(
        type = EventType.FW_RULE_MODIFIED,
        severity = Severity.LOW,
        message = if (undo) "Firewall status restored" else "Firewall status changed",
        source = EventSource.MANAGER,
        userAction = true,
        details = "Firewall status ${if (undo) "restored" else "changed"} for ${app.appName} (${app.uid}), new status: $status, conn status: $connection",
    )
}
