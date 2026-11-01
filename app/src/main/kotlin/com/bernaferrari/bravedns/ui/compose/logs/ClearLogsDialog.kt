/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import Logger
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.ui.compose.theme.RethinkConfirmDialog
import com.bernaferrari.bravedns.util.Utilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Confirms clearing the logs of [type], then runs [onDelete] off the main thread and [onRefresh]
 * after it, with a toast either way.
 */
@Composable
internal fun ClearLogsDialog(
    show: Boolean,
    type: RethinkLogType,
    onDismiss: () -> Unit,
    onDelete: suspend () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val refreshCompleteText = stringResource(R.string.refresh_complete)
    val failureText = stringResource(R.string.blocklist_update_check_failure)
    if (!show) return
    ConfirmClearLogsDialog(
        title = stringResource(clearTitle(type)),
        onDismiss = onDismiss,
        onConfirm = {
            onDismiss()
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { onDelete() }
                    onRefresh()
                    Utilities.showToastUiCentered(context, refreshCompleteText, Toast.LENGTH_SHORT)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: android.database.SQLException) {
                    Logger.e(Logger.LOG_TAG_UI, "Failed to clear logs", error)
                    Utilities.showToastUiCentered(context, failureText, Toast.LENGTH_LONG)
                } catch (error: IllegalStateException) {
                    Logger.e(Logger.LOG_TAG_UI, "Log database unavailable", error)
                    Utilities.showToastUiCentered(context, failureText, Toast.LENGTH_LONG)
                }
            }
        },
    )
}

private fun clearTitle(type: RethinkLogType): Int = when (type) {
    RethinkLogType.All -> R.string.activity_clear_all_title
    RethinkLogType.Connections -> R.string.conn_track_clear_logs_title
    RethinkLogType.Lookups -> R.string.activity_clear_lookups_title
}

@Composable
private fun ConfirmClearLogsDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    RethinkConfirmDialog(
        onDismissRequest = onDismiss,
        title = title,
        message = stringResource(R.string.conn_track_clear_logs_message),
        confirmText = stringResource(R.string.lbl_delete),
        dismissText = stringResource(R.string.lbl_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isConfirmDestructive = true,
    )
}
