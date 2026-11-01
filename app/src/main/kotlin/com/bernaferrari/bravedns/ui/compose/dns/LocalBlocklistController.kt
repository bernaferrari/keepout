/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.dns

import Logger
import Logger.LOG_TAG_UI
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.customdownloader.LocalBlocklistCoordinator
import com.bernaferrari.bravedns.download.AppDownloadManager
import com.bernaferrari.bravedns.download.AppDownloadManager.DownloadManagerStatus
import com.bernaferrari.bravedns.download.DownloadConstants
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.service.RethinkBlocklistManager
import com.bernaferrari.bravedns.service.VpnController
import com.bernaferrari.bravedns.util.Constants
import com.bernaferrari.bravedns.util.Constants.Companion.INIT_TIME_MS
import com.bernaferrari.bravedns.util.Constants.Companion.LOCAL_BLOCKLIST_DOWNLOAD_FOLDER_NAME
import com.bernaferrari.bravedns.util.Constants.Companion.RETHINK_SEARCH_URL
import com.bernaferrari.bravedns.util.UIUtils
import com.bernaferrari.bravedns.util.Utilities
import com.bernaferrari.bravedns.util.Utilities.blocklistCanonicalPath
import com.bernaferrari.bravedns.util.Utilities.deleteRecursive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** The one maintenance action the local-blocklist sheet offers at a time. */
internal enum class LocalBlocklistOffer { CheckUpdate, Download, Redownload }

/** A download the user asked for, waiting on a confirmation. */
internal data class LocalBlocklistDownload(val redownload: Boolean)

/** What the local-blocklist sheet and its confirmations show. */
@Stable
internal class LocalBlocklistUiState {
    var sheetVisible by mutableStateOf(false)
    /** Whether the blocklists are in use, as last read; null on Play, which never reads it. */
    var inUse by mutableStateOf<Boolean?>(null)
        private set
    var offer by mutableStateOf(LocalBlocklistOffer.CheckUpdate)
        private set
    var checking by mutableStateOf(false)
    var downloading by mutableStateOf(false)
    var redownloading by mutableStateOf(false)
    var downloadPrompt by mutableStateOf<LocalBlocklistDownload?>(null)
    /** The VPN is in lockdown and the system downloader can't reach the network: ask first. */
    var lockdownPrompt by mutableStateOf<LocalBlocklistDownload?>(null)

    /** Offer [next], with nothing in progress. */
    fun offer(next: LocalBlocklistOffer) {
        offer = next
        settle()
    }

    /** Nothing is checking or downloading any more. */
    fun settle() {
        checking = false
        downloading = false
        redownloading = false
    }

    fun refreshUsage(prefs: PersistentState) {
        if (Utilities.isPlayStoreFlavour()) return
        inUse = prefs.blocklistEnabled
    }
}

/** Download an update when a newer one is known; otherwise offer to check. */
private fun initialOffer(prefs: PersistentState): LocalBlocklistOffer = when {
    prefs.localBlocklistTimestamp == INIT_TIME_MS -> LocalBlocklistOffer.CheckUpdate
    prefs.newestRemoteBlocklistTimestamp == INIT_TIME_MS -> LocalBlocklistOffer.CheckUpdate
    prefs.newestLocalBlocklistTimestamp > prefs.localBlocklistTimestamp -> LocalBlocklistOffer.Download
    else -> LocalBlocklistOffer.CheckUpdate
}

/** The toasts and clipboard label the local-blocklist actions use. */
internal data class LocalBlocklistMessages(
    val updateFailed: String,
    val updateNotRequired: String,
    val notAvailable: String,
    val deleted: String,
    val startVpnFirst: String,
    val clipboardLabel: String,
    val stampCopied: String,
)

/**
 * The WorkManager jobs a blocklist download runs as. The in-app downloader is one job; the
 * system downloader is a download job (progress) followed by a file job (completion).
 */
internal enum class LocalBlocklistWork(
    val tag: String,
    val reportsProgress: Boolean,
    val reportsCompletion: Boolean,
    val cancelOnFailure: List<String>,
) {
    InApp(
        LocalBlocklistCoordinator.CUSTOM_DOWNLOAD,
        reportsProgress = true,
        reportsCompletion = true,
        cancelOnFailure = listOf(LocalBlocklistCoordinator.CUSTOM_DOWNLOAD),
    ),
    SystemDownload(
        DownloadConstants.DOWNLOAD_TAG,
        reportsProgress = true,
        reportsCompletion = false,
        cancelOnFailure = listOf(DownloadConstants.DOWNLOAD_TAG, DownloadConstants.FILE_TAG),
    ),
    SystemFile(
        DownloadConstants.FILE_TAG,
        reportsProgress = false,
        reportsCompletion = true,
        cancelOnFailure = listOf(DownloadConstants.FILE_TAG),
    ),
}

/** Checks for, and downloads, blocklist updates; reports their progress into [state]. */
internal class LocalBlocklistDownloader(
    private val state: LocalBlocklistUiState,
    private val context: Context,
    private val scope: CoroutineScope,
    private val persistentState: PersistentState,
    private val appDownloadManager: AppDownloadManager,
    private val messages: LocalBlocklistMessages,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun checkForUpdate() {
        state.checking = true
        scope.launch(Dispatchers.IO) {
            appDownloadManager.isDownloadRequired(RethinkBlocklistManager.DownloadType.LOCAL)
        }
    }

    /** Download now, unless lockdown means the system downloader needs the user's say first. */
    fun download(request: LocalBlocklistDownload) {
        if (VpnController.isVpnLockdown() && !persistentState.useCustomDownloadManager) {
            state.lockdownPrompt = request
            return
        }
        start(request)
    }

    /** Download without asking about lockdown again. */
    fun start(request: LocalBlocklistDownload) {
        scope.launch(Dispatchers.Main) {
            state.downloading = !request.redownload
            state.redownloading = request.redownload
            val currentTs = persistentState.localBlocklistTimestamp
            val status = withContext(Dispatchers.IO) {
                appDownloadManager.downloadLocalBlocklist(currentTs, request.redownload)
            }
            onDownloadStatus(status)
        }
    }

    fun onDownloadStatus(status: DownloadManagerStatus) {
        when (status) {
            DownloadManagerStatus.IN_PROGRESS, DownloadManagerStatus.STARTED -> state.checking = true
            DownloadManagerStatus.NOT_STARTED -> Unit
            DownloadManagerStatus.SUCCESS -> {
                state.offer(LocalBlocklistOffer.Download)
                acknowledge()
            }
            DownloadManagerStatus.FAILURE -> {
                state.settle()
                context.toast(messages.updateFailed)
                acknowledge()
            }
            DownloadManagerStatus.NOT_REQUIRED -> {
                state.offer(LocalBlocklistOffer.Redownload)
                context.toast(messages.updateNotRequired)
                acknowledge()
            }
            DownloadManagerStatus.NOT_AVAILABLE -> context.toast(messages.notAvailable)
        }
    }

    fun onWorkState(work: LocalBlocklistWork, workState: WorkInfo.State) {
        Logger.i(LOG_TAG_UI, "WorkManager state: $workState for ${work.tag}")
        when (workState) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING ->
                if (work.reportsProgress) state.downloading = true
            WorkInfo.State.SUCCEEDED -> if (work.reportsCompletion) {
                state.offer(LocalBlocklistOffer.Download)
                workManager.pruneWork()
            }
            WorkInfo.State.CANCELLED, WorkInfo.State.FAILED -> {
                state.downloading = false
                context.toast(messages.updateFailed)
                workManager.pruneWork()
                work.cancelOnFailure.forEach(workManager::cancelAllWorkByTag)
            }
            WorkInfo.State.BLOCKED -> Unit
        }
    }

    /** The status was handled: reset it so the same one isn't handled again. */
    private fun acknowledge() {
        appDownloadManager.downloadRequired.value = DownloadManagerStatus.NOT_STARTED
    }
}

/**
 * The on-device blocklists behind DNS: open the sheet, turn them on or off, pick them, copy or
 * search the stamp, and delete them. [downloader] keeps them up to date.
 */
@Stable
internal class LocalBlocklistController(
    val state: LocalBlocklistUiState,
    val downloader: LocalBlocklistDownloader,
    private val context: Context,
    private val scope: CoroutineScope,
    private val persistentState: PersistentState,
    private val viewModel: DnsSettingsViewModel,
    private val messages: LocalBlocklistMessages,
    private val onConfigure: () -> Unit,
) {
    /** Re-read whether the blocklists are in use and which maintenance action fits. */
    fun refresh() {
        state.refreshUsage(persistentState)
        state.offer(initialOffer(persistentState))
    }

    fun open() {
        refresh()
        state.sheetVisible = true
    }

    fun dismiss() {
        state.sheetVisible = false
        viewModel.updateUiState()
    }

    /** Off when on. Otherwise on when blocklists are downloaded and chosen, else go choose them. */
    fun toggleEnabled() {
        if (persistentState.blocklistEnabled) {
            setEnabled(false)
            state.refreshUsage(persistentState)
            return
        }
        if (!VpnController.hasTunnel()) {
            context.toast(messages.startVpnFirst)
            return
        }
        scope.launch(Dispatchers.Main) {
            val downloaded = withContext(Dispatchers.Default) {
                Utilities.hasLocalBlocklists(context, persistentState.localBlocklistTimestamp)
            }
            if (!downloaded) {
                leaveToConfigure()
                return@launch
            }
            setEnabled(true)
            // Downloaded but none chosen yet: on, and off to choose them.
            if (persistentState.localBlocklistStamp.isNotEmpty()) {
                state.refreshUsage(persistentState)
            } else {
                leaveToConfigure()
            }
        }
    }

    /** Choose which blocklists to use; that needs the tunnel up. */
    fun configure() {
        if (!VpnController.hasTunnel()) {
            context.toast(messages.startVpnFirst)
            return
        }
        leaveToConfigure()
    }

    fun copyStamp() {
        val url = Constants.RETHINK_BASE_URL_MAX + persistentState.localBlocklistStamp
        UIUtils.clipboardCopy(context, url, messages.clipboardLabel)
        context.toast(messages.stampCopied)
    }

    fun search() {
        dismiss()
        UIUtils.openUrl(context, RETHINK_SEARCH_URL + Uri.encode(persistentState.localBlocklistStamp))
    }

    fun delete() {
        scope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                deleteRecursive(File(blocklistCanonicalPath(context, LOCAL_BLOCKLIST_DOWNLOAD_FOLDER_NAME)))
                persistentState.localBlocklistTimestamp = INIT_TIME_MS
                persistentState.localBlocklistStamp = ""
                persistentState.newestLocalBlocklistTimestamp = INIT_TIME_MS
            }
            state.refreshUsage(persistentState)
            state.offer(LocalBlocklistOffer.CheckUpdate)
            viewModel.updateUiState()
            context.toast(messages.deleted)
        }
    }

    private fun setEnabled(enabled: Boolean) {
        persistentState.blocklistEnabled = enabled
        viewModel.updateUiState()
    }

    private fun leaveToConfigure() {
        dismiss()
        onConfigure()
    }
}

@Composable
internal fun rememberLocalBlocklistController(
    persistentState: PersistentState,
    appDownloadManager: AppDownloadManager,
    viewModel: DnsSettingsViewModel,
    onConfigure: () -> Unit,
): LocalBlocklistController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnConfigure by rememberUpdatedState(onConfigure)
    val messages = LocalBlocklistMessages(
        updateFailed = stringResource(R.string.blocklist_update_check_failure),
        updateNotRequired = stringResource(R.string.blocklist_update_check_not_required),
        notAvailable = stringResource(R.string.blocklist_not_available_toast),
        deleted = stringResource(R.string.config_add_success_toast),
        startVpnFirst = stringResource(R.string.ssv_toast_start_rethink),
        clipboardLabel = stringResource(R.string.copy_clipboard_label),
        stampCopied = stringResource(R.string.info_dialog_rethink_toast_msg),
    )
    return remember(context, persistentState, appDownloadManager, viewModel, messages) {
        val state = LocalBlocklistUiState()
        LocalBlocklistController(
            state = state,
            downloader = LocalBlocklistDownloader(state, context, scope, persistentState, appDownloadManager, messages),
            context = context,
            scope = scope,
            persistentState = persistentState,
            viewModel = viewModel,
            messages = messages,
            onConfigure = { currentOnConfigure() },
        )
    }
}

private fun Context.toast(message: String) =
    Utilities.showToastUiCentered(this, message, Toast.LENGTH_SHORT)
