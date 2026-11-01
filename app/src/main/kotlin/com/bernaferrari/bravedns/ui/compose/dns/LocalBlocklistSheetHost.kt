/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.dns

import Logger
import Logger.LOG_TAG_UI
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkManager
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.download.AppDownloadManager
import com.bernaferrari.bravedns.download.AppDownloadManager.DownloadManagerStatus
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.ui.compose.theme.RethinkConfirmDialog
import com.bernaferrari.bravedns.util.Constants
import com.bernaferrari.bravedns.util.Constants.Companion.INIT_TIME_MS
import com.bernaferrari.bravedns.util.Utilities.convertLongToTime
import com.bernaferrari.bravedns.util.workInfosByTagFlow

/** The local-blocklist sheet, when open, with its download and lockdown confirmations. */
@Composable
internal fun LocalBlocklistSheetHost(
    blocklists: LocalBlocklistController,
    persistentState: PersistentState,
) {
    val state = blocklists.state
    if (state.sheetVisible) {
        RethinkLocalBlocklistSheet(
            state = localBlocklistSheetState(state, persistentState),
            strings = localBlocklistStrings(),
            onDismiss = blocklists::dismiss,
            onEnableToggle = blocklists::toggleEnabled,
            onConfigure = blocklists::configure,
            onCopy = blocklists::copyStamp,
            onSearch = blocklists::search,
            onCheckUpdate = blocklists.downloader::checkForUpdate,
            onDownload = { state.downloadPrompt = LocalBlocklistDownload(redownload = false) },
            onRedownload = { state.downloadPrompt = LocalBlocklistDownload(redownload = true) },
            onDelete = blocklists::delete,
        )
    }
    state.downloadPrompt?.let { request ->
        DownloadConfirmDialog(
            request = request,
            localBlocklistTimestamp = persistentState.localBlocklistTimestamp,
            onConfirm = {
                state.downloadPrompt = null
                blocklists.downloader.download(request)
            },
            onDismiss = { state.downloadPrompt = null },
        )
    }
    state.lockdownPrompt?.let { request ->
        RethinkConfirmDialog(
            onDismissRequest = { state.lockdownPrompt = null },
            title = stringResource(R.string.lockdown_download_enable_inapp),
            message = stringResource(R.string.lockdown_download_message),
            confirmText = stringResource(R.string.lockdown_download_enable_inapp),
            dismissText = stringResource(R.string.lbl_cancel),
            // Switch to the in-app downloader, which works in lockdown.
            onConfirm = {
                state.lockdownPrompt = null
                persistentState.useCustomDownloadManager = true
                blocklists.downloader.download(request)
            },
            // Cancel keeps the system downloader and tries anyway.
            onDismiss = {
                state.lockdownPrompt = null
                blocklists.downloader.start(request)
            },
        )
    }
}

/** Feeds download progress into the sheet: the update check's status, and the download jobs. */
@Composable
internal fun LocalBlocklistDownloadEffects(
    downloader: LocalBlocklistDownloader,
    appDownloadManager: AppDownloadManager,
) {
    val status by appDownloadManager.downloadRequired
        .collectAsStateWithLifecycle(initialValue = DownloadManagerStatus.NOT_STARTED)
    LaunchedEffect(status) {
        Logger.i(LOG_TAG_UI, "Check for blocklist update, status: $status")
        if (status != DownloadManagerStatus.NOT_STARTED) downloader.onDownloadStatus(status)
    }
    LocalBlocklistWork.entries.forEach { work ->
        key(work) { WorkStateEffect(work, downloader) }
    }
}

@Composable
private fun WorkStateEffect(work: LocalBlocklistWork, downloader: LocalBlocklistDownloader) {
    val context = LocalContext.current
    val workInfos by remember(context, work) { WorkManager.getInstance(context).workInfosByTagFlow(work.tag) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    LaunchedEffect(workInfos) {
        val workInfo = workInfos.firstOrNull() ?: return@LaunchedEffect
        downloader.onWorkState(work, workInfo.state)
    }
}

@Composable
private fun DownloadConfirmDialog(
    request: LocalBlocklistDownload,
    localBlocklistTimestamp: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title: String
    val message: String
    if (request.redownload) {
        title = stringResource(R.string.local_blocklist_redownload)
        message = stringResource(
            R.string.local_blocklist_redownload_desc,
            convertLongToTime(localBlocklistTimestamp, Constants.TIME_FORMAT_2),
        )
    } else {
        title = stringResource(R.string.local_blocklist_download)
        message = stringResource(R.string.local_blocklist_download_desc)
    }
    RethinkConfirmDialog(
        onDismissRequest = onDismiss,
        title = title,
        message = message,
        confirmText = stringResource(R.string.settings_local_blocklist_dialog_positive),
        dismissText = stringResource(R.string.lbl_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun localBlocklistSheetState(
    state: LocalBlocklistUiState,
    persistentState: PersistentState,
): RethinkLocalBlocklistState {
    val inUse = state.inUse == true
    return RethinkLocalBlocklistState(
        heading = localBlocklistHeading(state.inUse, persistentState.numberOfLocalBlocklists),
        version = localBlocklistVersion(persistentState.localBlocklistTimestamp),
        canConfigure = inUse,
        canCopy = inUse,
        canSearch = inUse,
        showCheckUpdate = state.offer == LocalBlocklistOffer.CheckUpdate,
        showDownload = state.offer == LocalBlocklistOffer.Download,
        showRedownload = state.offer == LocalBlocklistOffer.Redownload,
        checking = state.checking,
        downloading = state.downloading,
        redownloading = state.redownloading,
        enabled = persistentState.blocklistEnabled,
    )
}

/** "N blocklists in use" while on; the plain heading while off; nothing until read (Play). */
@Composable
private fun localBlocklistHeading(inUse: Boolean?, count: Int): String = when (inUse) {
    true -> stringResource(R.string.settings_local_blocklist_in_use, count.toString())
    false -> stringResource(R.string.lbbs_heading)
    null -> ""
}

@Composable
private fun localBlocklistVersion(timestamp: Long): String =
    if (timestamp == INIT_TIME_MS) {
        ""
    } else {
        stringResource(
            R.string.settings_local_blocklist_version,
            convertLongToTime(timestamp, Constants.TIME_FORMAT_2),
        )
    }

@Composable
private fun localBlocklistStrings() = RethinkLocalBlocklistStrings(
    state = stringResource(R.string.lbbs_state_header),
    enable = stringResource(R.string.lbbs_toggle_on),
    disable = stringResource(R.string.lbbs_toggle_off),
    toggleDescription = stringResource(R.string.lbbs_toggle_desc),
    actions = stringResource(R.string.lbbs_actions_header),
    configure = stringResource(R.string.lbbs_configure),
    copy = stringResource(R.string.lbbs_copy),
    search = stringResource(R.string.lbbs_search),
    maintenance = stringResource(R.string.lbbs_maintenance_header),
    checkUpdate = stringResource(R.string.lbbs_update_check),
    download = stringResource(R.string.local_blocklist_download),
    redownload = stringResource(R.string.rt_re_download),
    delete = stringResource(R.string.lbl_delete),
    deleteMessage = stringResource(R.string.local_blocklist_delete_desc),
    cancel = stringResource(R.string.lbl_cancel),
)
