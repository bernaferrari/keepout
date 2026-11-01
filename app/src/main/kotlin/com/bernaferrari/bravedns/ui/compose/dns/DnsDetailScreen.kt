/*
 * Copyright 2020 RethinkDNS and its authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bernaferrari.bravedns.ui.compose.dns

import Logger
import Logger.LOG_TAG_DNS
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.data.AppConfig.Companion.DOH_INDEX
import com.bernaferrari.bravedns.data.AppConfig.Companion.DOT_INDEX
import com.bernaferrari.bravedns.database.EventSource
import com.bernaferrari.bravedns.database.EventType
import com.bernaferrari.bravedns.database.Severity
import com.bernaferrari.bravedns.download.AppDownloadManager
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.service.VpnController
import com.bernaferrari.bravedns.ui.compose.settings.RethinkSelectionDialog
import com.bernaferrari.bravedns.ui.compose.settings.RethinkSelectionOption
import com.bernaferrari.bravedns.ui.compose.theme.RethinkMultiActionDialog
import com.bernaferrari.bravedns.util.Constants
import com.bernaferrari.bravedns.util.ResourceRecordTypes
import com.bernaferrari.bravedns.util.UIUtils
import com.bernaferrari.bravedns.util.Utilities
import com.bernaferrari.bravedns.util.Utilities.tos
import com.celzero.firestack.backend.Backend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * The DNS screen, or DNS › Advanced, with the sheets and dialogs both open: record types,
 * fallback DNS, the resolver behind System and Smart DNS, and the on-device blocklists.
 *
 * @param viewModel The DnsSettingsViewModel for managing DNS settings state
 * @param persistentState The PersistentState for accessing app preferences
 * @param appDownloadManager The AppDownloadManager for handling blocklist downloads
 * @param onCustomDnsClick Callback when custom DNS is clicked (navigates to DNS list)
 * @param onRethinkPlusDnsClick Callback when Rethink Plus DNS is clicked
 * @param onLocalBlocklistConfigureClick Callback when local blocklist configure is clicked
 * @param onBackClick Optional callback for back navigation
 */
@Composable
fun DnsDetailScreen(
    viewModel: DnsSettingsViewModel,
    persistentState: PersistentState,
    appDownloadManager: AppDownloadManager,
    initialFocusKey: String? = null,
    onCustomDnsClick: () -> Unit,
    onRethinkPlusDnsClick: () -> Unit,
    onLocalBlocklistConfigureClick: () -> Unit,
    onBlockFreeDnsClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    /** Show DNS › Advanced instead of the DNS screen (same state, sheets and dialogs). */
    advanced: Boolean = false,
    onAdvancedClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val blocklists = rememberLocalBlocklistController(
        persistentState = persistentState,
        appDownloadManager = appDownloadManager,
        viewModel = viewModel,
        onConfigure = onLocalBlocklistConfigureClick,
    )
    var dialog by remember { mutableStateOf<DnsDetailDialog?>(null) }
    var resolverInfo by remember { mutableStateOf<ResolverInfo?>(null) }
    val smartDnsIntro = stringResource(R.string.smart_dns_desc)
    val bullet = stringResource(R.string.symbol_star)

    LaunchedEffect(Unit) { blocklists.refresh() }
    LocalBlocklistDownloadEffects(blocklists.downloader, appDownloadManager)
    DnsStatusEffects(viewModel)

    val navigation = RethinkDnsSettingsNavigation(
        onRefresh = { viewModel.refreshDns() },
        onSystemDns = { viewModel.enableSystemDns() },
        onSystemDnsInfo = {
            scope.launch {
                val systemDns = withContext(Dispatchers.IO) { VpnController.getSystemDns() }
                resolverInfo = ResolverInfo(R.string.network_dns, systemDns)
            }
        },
        onCustomDns = onCustomDnsClick,
        onRethinkDns = onRethinkPlusDnsClick,
        onSmartDns = { viewModel.enableSmartDns() },
        onSmartDnsInfo = {
            scope.launch {
                val addresses = withContext(Dispatchers.IO) { smartDnsAddresses() }
                resolverInfo = ResolverInfo(R.string.smart_dns, smartDnsMessage(smartDnsIntro, bullet, addresses))
            }
        },
        onLocalBlocklists = blocklists::open,
        onRecordTypes = { dialog = DnsDetailDialog.RecordTypes },
        onTrustedEndpoint = onBlockFreeDnsClick,
        onAdvanced = onAdvancedClick,
        // The sheet's own toggle: download when missing, turn on when present.
        onLocalBlocklistSetUp = blocklists::toggleEnabled,
        onLocalBlocklistChoose = blocklists::configure,
        onFallbackDns = { dialog = DnsDetailDialog.FallbackDns },
        onRethinkNetwork = { viewModel.setRethinkMax(it) },
    )
    if (advanced) {
        DnsAdvancedSettingsScreen(
            uiState = uiState,
            navigation = navigation,
            viewModel = viewModel,
            initialFocusKey = initialFocusKey,
            onBackClick = onBackClick,
        )
    } else {
        DnsSettingsScreen(
            uiState = uiState,
            navigation = navigation,
            initialFocusKey = initialFocusKey,
            onBackClick = onBackClick,
        )
    }

    when (dialog) {
        DnsDetailDialog.RecordTypes -> AndroidDnsRecordTypesSheet(
            persistentState = persistentState,
            onDismiss = { dialog = null },
        )
        DnsDetailDialog.FallbackDns -> FallbackDnsDialog(
            selectedUrl = persistentState.defaultDnsUrl,
            onDismiss = { dialog = null },
            onConfirm = { url -> viewModel.setDefaultDnsUrl(url) },
        )
        null -> Unit
    }
    resolverInfo?.let { info -> ResolverInfoDialog(info, onDismiss = { resolverInfo = null }) }
    LocalBlocklistSheetHost(blocklists, persistentState)
}

private const val DNS_STATUS_POLL_MS = 3_000L

private enum class DnsDetailDialog { RecordTypes, FallbackDns }

/** Which resolvers System or Smart DNS is using right now: [message] lists them. */
private data class ResolverInfo(@StringRes val title: Int, val message: String)

/** Refresh on resume; and poll, so the hero turns into an error while the resolver isn't answering, and back. */
@Composable
private fun DnsStatusEffects(viewModel: DnsSettingsViewModel) {
    LifecycleResumeEffect(Unit) {
        viewModel.updateUiState()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(DNS_STATUS_POLL_MS)
            viewModel.refreshDnsStatus()
        }
    }
}

/** The DoH and DoT addresses Smart DNS resolves through (other transports have none to show). */
private suspend fun smartDnsAddresses(): List<String> {
    val addresses = VpnController.getPlusResolvers().mapNotNull { id ->
        when (id.substringAfter(Backend.Plus).getOrNull(0)) {
            null -> {
                Logger.w(LOG_TAG_DNS, "smart(plus) dns resolver id is empty: $id")
                null
            }
            DOH_INDEX, DOT_INDEX -> VpnController.getPlusTransportById(id)?.addr?.tos()?.takeIf { it.isNotEmpty() }
            else -> {
                Logger.w(LOG_TAG_DNS, "smart(plus) dns resolver id is not doh or dot: $id")
                null
            }
        }
    }
    Logger.i(LOG_TAG_DNS, "smart(plus) dns list size: ${addresses.size}")
    return addresses
}

private fun smartDnsMessage(intro: String, bullet: String, addresses: List<String>): String =
    buildString {
        append(intro).append("\n\n")
        addresses.forEach { append("$bullet $it").append("\n") }
    }

/** The resolver addresses, with Copy. */
@Composable
private fun ResolverInfoDialog(info: ResolverInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardLabel = stringResource(R.string.copy_clipboard_label)
    val copiedToast = stringResource(R.string.info_dialog_url_copy_toast_msg)
    RethinkMultiActionDialog(
        onDismissRequest = onDismiss,
        title = stringResource(info.title),
        message = info.message,
        primaryText = stringResource(R.string.ada_noapp_dialog_positive),
        onPrimary = onDismiss,
        secondaryText = stringResource(R.string.dns_info_neutral),
        onSecondary = {
            UIUtils.clipboardCopy(context, info.message, clipboardLabel)
            Utilities.showToastUiCentered(context, copiedToast, Toast.LENGTH_SHORT)
            onDismiss()
        },
    )
}

/** Fallback DNS, moved here from Network › Advanced: the same list and preference. */
@Composable
private fun FallbackDnsDialog(
    selectedUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val eventLogger = koinInject<EventLogger>()
    val options = Constants.DEFAULT_DNS_LIST
    RethinkSelectionDialog(
        title = stringResource(R.string.settings_default_dns_heading),
        options = options.map { RethinkSelectionOption(it.url, it.name) },
        initialSelectedId = options.firstOrNull { it.url == selectedUrl }?.url ?: options.firstOrNull()?.url.orEmpty(),
        confirm = stringResource(R.string.fapps_info_dialog_positive_btn),
        cancel = stringResource(R.string.lbl_cancel),
        onDismiss = onDismiss,
        onConfirm = { option ->
            onConfirm(option.id)
            eventLogger.log(EventType.TUN_ESTABLISHED, Severity.LOW, "default dns changed", EventSource.UI, false, "Default DNS changed")
            onDismiss()
        },
    )
}

private fun getInitialRecordSelection(persistentState: PersistentState): List<String> {
    if (!persistentState.dnsRecordTypesAutoMode) {
        return persistentState.getAllowedDnsRecordTypes().toList()
    }
    val storedSelection = persistentState.allowedDnsRecordTypesString
    if (storedSelection.isNotEmpty()) {
        return storedSelection.split(",").filter { it.isNotEmpty() }
    }
    return listOf(
        ResourceRecordTypes.A.name,
        ResourceRecordTypes.AAAA.name,
        ResourceRecordTypes.CNAME.name,
        ResourceRecordTypes.HTTPS.name,
        ResourceRecordTypes.SVCB.name
    )
}

/** Android preference adapter for the shared DNS record-type sheet. */
@Composable
private fun AndroidDnsRecordTypesSheet(
    persistentState: PersistentState,
    onDismiss: () -> Unit,
) {
    var autoMode by remember { mutableStateOf(persistentState.dnsRecordTypesAutoMode) }
    val selected = remember {
        mutableStateListOf<String>().apply { addAll(getInitialRecordSelection(persistentState)) }
    }
    val types = remember {
        ResourceRecordTypes.entries
            .filter { it != ResourceRecordTypes.UNKNOWN }
            .sortedBy { it.name }
            .map { RethinkDnsRecordType(it.name, it.name, it.desc) }
    }
    val selectedLabel = stringResource(R.string.rt_filter_parent_selected)
    RethinkDnsRecordTypesSheet(
        types = types,
        autoMode = autoMode,
        selectedIds = selected.toSet(),
        strings = RethinkDnsRecordTypesStrings(
            title = stringResource(R.string.cd_allowed_dns_record_types_heading),
            description = stringResource(R.string.cd_allowed_dns_record_types_desc),
            auto = stringResource(R.string.settings_ip_text_ipv46),
            manual = stringResource(R.string.lbl_manual),
            selected = { count, total -> "$selectedLabel $count/$total" },
            allowed = stringResource(R.string.lbl_allowed),
        ),
        onAutoModeChange = { enabled ->
            autoMode = enabled
            persistentState.dnsRecordTypesAutoMode = enabled
        },
        onToggle = { id ->
            if (id in selected) selected.remove(id) else selected.add(id)
            persistentState.setAllowedDnsRecordTypes(selected.toSet())
        },
        onDismiss = onDismiss,
    )
}
