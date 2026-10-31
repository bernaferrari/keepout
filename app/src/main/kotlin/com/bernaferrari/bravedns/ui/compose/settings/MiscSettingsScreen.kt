/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.settings

import com.bernaferrari.bravedns.ui.compose.database.BackupRestoreSheet
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols
import com.bernaferrari.bravedns.ui.compose.theme.CardPosition
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListItem
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListGroup
import com.bernaferrari.bravedns.ui.compose.theme.RethinkFormSection
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.EventSource
import com.bernaferrari.bravedns.database.EventType
import com.bernaferrari.bravedns.database.Severity
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.ui.compose.settings.RethinkMiscSettingIcon.AppUpdates
import com.bernaferrari.bravedns.ui.compose.settings.RethinkMiscSettingIcon.AutoStart
import com.bernaferrari.bravedns.ui.compose.settings.RethinkMiscSettingIcon.CrashReports
import com.bernaferrari.bravedns.ui.compose.settings.RethinkMiscSettingIcon.FirewallBubble
import com.bernaferrari.bravedns.ui.compose.settings.RethinkMiscSettingIcon.IpInfo
import com.bernaferrari.bravedns.ui.compose.settings.RethinkMiscSettingIcon.Logs
import com.bernaferrari.bravedns.ui.compose.settings.RethinkMiscSettingIcon.Tombstone
import com.bernaferrari.bravedns.util.Utilities.isAtleastQ
import com.bernaferrari.bravedns.util.Utilities.isFdroidFlavour

/**
 * Settings › General: everything about Keepout itself on one screen — how it behaves (start on
 * boot, uninstalled apps, update checks, crash reports), App lock, then Backup & restore and the
 * system notification settings. Logging lives on Logs & diagnostics ([LoggingSettingsScreen]).
 */
@Composable
fun MiscSettingsScreen(
    persistentState: PersistentState,
    eventLogger: EventLogger,
    onNotificationsClick: () -> Unit,
    initialFocusKey: String? = null,
    onBackClick: (() -> Unit)? = null,
) {
    var showBackupSheet by rememberSaveable { mutableStateOf(false) }
    var autoStartEnabled by remember { mutableStateOf(persistentState.prefAutoStartBootUp) }
    var tombstoneEnabled by remember { mutableStateOf(persistentState.tombstoneApps) }
    var checkUpdatesEnabled by remember { mutableStateOf(persistentState.checkForAppUpdate) }
    var firebaseEnabled by remember { mutableStateOf(persistentState.firebaseErrorReportingEnabled) }
    val isFdroid = isFdroidFlavour()
    val toggles = listOfNotNull(
        RethinkMiscToggle("general_autostart", stringResource(R.string.settings_autostart_bootup_heading), stringResource(R.string.settings_autostart_bootup_desc), autoStartEnabled, AutoStart),
        RethinkMiscToggle("general_tombstone", stringResource(R.string.tombstone_app_title), stringResource(R.string.tombstone_app_desc), tombstoneEnabled, Tombstone),
        if (!isFdroid) RethinkMiscToggle("general_app_updates", stringResource(R.string.settings_check_update_heading), stringResource(R.string.settings_check_update_desc), checkUpdatesEnabled, AppUpdates) else null,
        if (!isFdroid) RethinkMiscToggle("general_crash_reports", stringResource(R.string.settings_firebase_error_reporting_heading), stringResource(R.string.settings_firebase_error_reporting_desc), firebaseEnabled, CrashReports) else null,
    )
    RethinkMiscSettingsScreen(
        strings = settingsListStrings(
            title = stringResource(R.string.settings_general_header).titlecaseFirst(),
            section = stringResource(R.string.settings_misc_behavior_section),
        ),
        toggles = toggles,
        onToggleChange = { id, enabled ->
            when (id) {
                "general_autostart" -> { autoStartEnabled = enabled; persistentState.prefAutoStartBootUp = enabled }
                "general_tombstone" -> { tombstoneEnabled = enabled; persistentState.tombstoneApps = enabled }
                "general_app_updates" -> { checkUpdatesEnabled = enabled; persistentState.checkForAppUpdate = enabled }
                "general_crash_reports" -> { firebaseEnabled = enabled; persistentState.firebaseErrorReportingEnabled = enabled; com.bernaferrari.bravedns.util.FirebaseErrorReporting.setEnabled(enabled) }
            }
            logSettingChange(eventLogger, "General setting", id, enabled)
        },
        additionalSettings = {
            Column(verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingLg)) {
                AppLockSettingSection(persistentState, eventLogger)
                RethinkFormSection(stringResource(R.string.settings_general_more_section)) {
                    RethinkListGroup {
                        // Opens a sheet in place; chevrons are reserved for navigation.
                        RethinkListItem(
                            headline = stringResource(R.string.brbs_title),
                            supporting = stringResource(R.string.settings_import_export_desc),
                            leadingIcon = MaterialSymbols.Filled.Backup,
                            position = CardPosition.First,
                            onClick = { showBackupSheet = true },
                        )
                        RethinkListItem(
                            headline = stringResource(R.string.settings_notifications),
                            supporting = stringResource(R.string.settings_notifications_desc),
                            leadingIcon = MaterialSymbols.Filled.Notifications,
                            position = CardPosition.Last,
                            onClick = onNotificationsClick,
                        )
                    }
                }
            }
        },
        onBackClick = onBackClick,
        focusedSettingId = initialFocusKey?.takeIf { it.isNotBlank() },
    )
    if (showBackupSheet) BackupRestoreSheet(onDismiss = { showBackupSheet = false })
}

/**
 * Settings › Advanced › Logs & diagnostics: what Keepout records on this device (connection and DNS
 * logs, how long they're kept, provider details, packet capture, the firewall bubble), then the
 * troubleshooting tools (event logs, app logs, connectivity checks) and, in debug builds, the
 * developer tools and engine toggles.
 */
@Composable
fun LoggingSettingsScreen(
    persistentState: PersistentState,
    eventLogger: EventLogger,
    diagnostics: DiagnosticsActions,
    initialFocusKey: String? = null,
    onBackClick: (() -> Unit)? = null,
) {
    var logsEnabled by remember { mutableStateOf(persistentState.logsEnabled) }
    var firewallBubbleEnabled by remember { mutableStateOf(persistentState.firewallBubbleEnabled) }
    var ipInfoEnabled by remember { mutableStateOf(persistentState.downloadIpInfo) }
    val toggles = listOfNotNull(
        RethinkMiscToggle("general_logs", stringResource(R.string.settings_enable_logs), stringResource(R.string.settings_enable_logs_desc), logsEnabled, Logs),
        RethinkMiscToggle("general_ip_info", stringResource(R.string.download_ip_info_title), stringResource(R.string.download_ip_info_desc, stringResource(R.string.lbl_ipinfo_inc)).replace("..", "."), ipInfoEnabled, IpInfo),
        if (isAtleastQ()) RethinkMiscToggle("general_firewall_bubble", stringResource(R.string.firewall_bubble_title), stringResource(R.string.firewall_bubble_desc), firewallBubbleEnabled, FirewallBubble) else null,
    )
    RethinkMiscSettingsScreen(
        strings = settingsListStrings(
            title = stringResource(R.string.settings_logs_diagnostics_title),
            section = stringResource(R.string.settings_logging_section),
        ),
        toggles = toggles,
        onToggleChange = { id, enabled ->
            when (id) {
                "general_logs" -> { logsEnabled = enabled; persistentState.logsEnabled = enabled }
                "general_firewall_bubble" -> { firewallBubbleEnabled = enabled; persistentState.firewallBubbleEnabled = enabled }
                "general_ip_info" -> { ipInfoEnabled = enabled; persistentState.downloadIpInfo = enabled }
            }
            logSettingChange(eventLogger, "Logging setting", id, enabled)
        },
        additionalSettings = {
            Column(verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingLg)) {
                RethinkFormSection(stringResource(R.string.settings_logging_keep_section)) {
                    RethinkListGroup {
                        LogRetentionSetting(persistentState, CardPosition.First)
                        PcapCaptureSetting(CardPosition.Last)
                    }
                }
                RethinkSettingsLinkGroup(
                    stringResource(R.string.diagnostics_section_troubleshoot),
                    listOf(
                        RethinkSettingsLink("events", stringResource(R.string.event_logs_title), MaterialSymbols.Filled.History, diagnostics.onEventLogs, stringResource(R.string.diagnostics_event_logs_desc)),
                        RethinkSettingsLink("console", stringResource(R.string.console_log_title), MaterialSymbols.Filled.Terminal, diagnostics.onAppLogs, stringResource(R.string.diagnostics_app_logs_desc)),
                        RethinkSettingsLink("ping", stringResource(R.string.diagnostics_connectivity_check), MaterialSymbols.Filled.NetworkCheck, diagnostics.onConnectivityChecks, stringResource(R.string.settings_connectivity_checks_desc)),
                    ),
                )
                if (diagnostics.isDebug) {
                    DeveloperSettingsSection(
                        persistentState,
                        tools = listOf(
                            // Stats opens a dialog and the recorder starts at once: actions in place, no chevron.
                            RethinkSettingsLink("stats", stringResource(R.string.title_statistics), MaterialSymbols.Filled.InsertChart, diagnostics.onNetworkStats, opensScreen = false),
                            RethinkSettingsLink("database", stringResource(R.string.title_database_dump), MaterialSymbols.Filled.Database, diagnostics.onDatabase),
                            RethinkSettingsLink("flight", stringResource(R.string.about_flight_recorder), MaterialSymbols.Filled.Report, diagnostics.onFlightRecorder, opensScreen = false),
                        ),
                    )
                }
            }
        },
        onBackClick = onBackClick,
        focusedSettingId = initialFocusKey?.takeIf { it.isNotBlank() },
    )
}

/** Where Logs & diagnostics' troubleshooting rows go; the host owns navigation and dialogs. */
class DiagnosticsActions(
    val isDebug: Boolean,
    val onEventLogs: () -> Unit,
    val onAppLogs: () -> Unit,
    val onConnectivityChecks: () -> Unit,
    val onNetworkStats: () -> Unit,
    val onDatabase: () -> Unit,
    val onFlightRecorder: () -> Unit,
)

/** Only title and section are shown when the backup and about rows are off. */
private fun settingsListStrings(title: String, section: String) = RethinkMiscSettingsStrings(
    title = title,
    backupSection = "",
    backupTitle = "",
    backupDescription = "",
    generalSection = section,
    aboutSection = "",
    websiteTitle = "",
    websiteDescription = "",
    aboutTitle = "",
    aboutDescription = "",
)

private fun logSettingChange(eventLogger: EventLogger, message: String, id: String, enabled: Boolean) =
    eventLogger.log(EventType.UI_SETTING_CHANGED, Severity.LOW, message, EventSource.UI, true, "$id set to $enabled")

private fun String.titlecaseFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
