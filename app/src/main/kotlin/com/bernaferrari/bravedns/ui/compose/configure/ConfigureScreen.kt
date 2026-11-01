/* Copyright 2024 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.configure

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.ui.compose.database.BackupRestoreSheet
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols
import com.bernaferrari.bravedns.ui.icons.keepout.KeepoutIcons

/**
 * The Settings tab: Appearance inline at the top, Protection (DNS, Firewall, Proxy & VPN) and
 * Advanced (Network, Censorship, Logs & diagnostics) as colored tiles, then General (start on boot,
 * app lock, backup, notifications) and About as rows. Apps and the log are tabs.
 *
 * [onOpen] receives every destination; Backup & restore opens its sheet here, over the hub, and
 * Appearance scrolls the hub to its inline section.
 */
@Composable
fun ConfigureScreen(
    isDebug: Boolean,
    onOpen: (SettingsSearchDestination) -> Unit,
    appearance: @Composable () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var showBackupSheet by rememberSaveable { mutableStateOf(false) }
    var revealAppearance by remember { mutableIntStateOf(0) }
    BackHandler(enabled = searchOpen) { searchOpen = false; query = "" }
    fun open(destination: SettingsSearchDestination) {
        when (destination) {
            SettingsSearchDestination.Backup -> showBackupSheet = true
            SettingsSearchDestination.Appearance -> revealAppearance++
            else -> onOpen(destination)
        }
    }
    fun selectSearch(destination: SettingsSearchDestination) {
        searchOpen = false
        query = ""
        open(destination)
    }
    fun entry(
        id: String,
        title: String,
        icon: ImageVector,
        destination: SettingsSearchDestination,
        accent: Color,
        keywords: List<String> = emptyList(),
        click: () -> Unit = { open(destination) },
    ) = RethinkConfigureEntry(
        id = id,
        title = title,
        icon = { Icon(icon, null) },
        onClick = click,
        keywords = keywords,
        accent = accent,
    )

    val appName = stringResource(R.string.app_name)
    val sections = listOf(
        // Protection: what Keepout does to traffic. Three tiles; the last one spans the row.
        RethinkConfigureSection(stringResource(R.string.lbl_protection), listOf(
            entry("dns", stringResource(R.string.lbl_dns), MaterialSymbols.Filled.Dns, SettingsSearchDestination.Dns(), ConfigureAccents.dns, listOf("dns", "resolver", "blocklist")),
            entry("firewall", stringResource(R.string.lbl_firewall), KeepoutIcons.Firewall, SettingsSearchDestination.Firewall(), ConfigureAccents.firewall, listOf("firewall", "rules", "allow", "block")),
            entry("proxy", stringResource(R.string.settings_proxy_vpn), MaterialSymbols.Filled.ConversionPath, SettingsSearchDestination.Proxy(), ConfigureAccents.proxy, listOf("proxy", "socks5", "orbot", "wireguard", "vpn")),
        ), RethinkConfigureLayout.GridFour),
        // Advanced: engine knobs and troubleshooting, the same three-tile shape as Protection.
        RethinkConfigureSection(stringResource(R.string.lbl_advanced), listOf(
            entry("network", stringResource(R.string.lbl_network), MaterialSymbols.Filled.VpnLock, SettingsSearchDestination.Network(), ConfigureAccents.network, listOf("network", "vpn", "tunnel")),
            entry("anti-censorship", stringResource(R.string.settings_tile_censorship), MaterialSymbols.Filled.DominoMask, SettingsSearchDestination.AntiCensorship, ConfigureAccents.antiCensorship, listOf("anti censorship", "dpi")),
            entry("logging", stringResource(R.string.settings_logs_diagnostics_title), MaterialSymbols.Filled.History, SettingsSearchDestination.Logging(), ConfigureAccents.logs, listOf("logs", "logging", "diagnostics", "event logs", "ping")),
        ), RethinkConfigureLayout.GridFour),
        // After the grids: two plain rows (Appearance sits inline above everything; see appearanceAfter).
        RethinkConfigureSection("", listOf(
            entry("general", stringResource(R.string.settings_general_header).replaceFirstChar { it.titlecase() }, MaterialSymbols.Filled.Tune, SettingsSearchDestination.General(), ConfigureAccents.settings, listOf("general", "boot", "updates", "lock", "backup", "notifications"))
                .copy(subtitle = stringResource(R.string.settings_general_summary)),
            entry("about", stringResource(R.string.settings_about_keepout, appName), MaterialSymbols.Filled.Info, SettingsSearchDestination.About, ConfigureAccents.about, listOf("about", "version")),
        ), RethinkConfigureLayout.List),
    )
    val deepSearch = buildSettingsSearchIndex(isDebug).map { source ->
        RethinkConfigureSearchEntry(
            RethinkConfigureEntry(
                id = source.id,
                title = source.title,
                icon = { Icon(source.icon, null) },
                onClick = { selectSearch(source.destination) },
                subtitle = source.subtitle,
                keywords = source.keywords,
                accent = source.destination.configureAccent(),
            ),
            source.path,
        )
    }
    val topSearch = sections.flatMap { section ->
        section.entries.map { entry ->
            RethinkConfigureSearchEntry(
                entry.copy(onClick = { searchOpen = false; query = ""; entry.onClick() }),
                if (section.title.isBlank()) entry.title else "${section.title} > ${entry.title}",
            )
        }
    }
    RethinkConfigureScreen(
        sections = sections,
        searchEntries = (deepSearch + topSearch).distinctBy(::rethinkConfigureSearchDedupKey),
        strings = RethinkConfigureStrings(
            title = stringResource(R.string.title_settings), searchHint = stringResource(R.string.configure_search_hint),
            openSearch = stringResource(R.string.configure_search_open), closeSearch = stringResource(R.string.configure_search_close),
            clearSearch = stringResource(R.string.cd_clear_search), noResultsTitle = stringResource(R.string.configure_search_empty_title), noResultsSubtitle = stringResource(R.string.configure_search_empty_subtitle),
        ),
        searchOpen = searchOpen, query = query,
        onSearchOpenChange = { searchOpen = it; if (!it) query = "" }, onQueryChange = { query = it },
        appearanceContent = appearance,
        // Appearance leads: the one thing most people change here, and it shows at a glance.
        appearanceAfter = 0,
        revealAppearance = revealAppearance,
    )
    if (showBackupSheet) BackupRestoreSheet(onDismiss = { showBackupSheet = false })
}

/**
 * The tile hue a search result wears. A screen keeps its hue whatever section the focus key
 * points at; a place handled in place (or a section folded into a screen) has its own.
 */
private fun SettingsSearchDestination.configureAccent(): Color = when (this) {
    is SettingsSearchDestination.Dns -> ConfigureAccents.dns
    is SettingsSearchDestination.Firewall -> ConfigureAccents.firewall
    is SettingsSearchDestination.Proxy -> ConfigureAccents.proxy
    is SettingsSearchDestination.General -> ConfigureAccents.settings
    is SettingsSearchDestination.Network -> ConfigureAccents.network
    is SettingsSearchDestination.Logging -> ConfigureAccents.logs
    else -> placeAccents[this] ?: ConfigureAccents.settings
}

/** The destinations without a focus key: one value each, so they key a plain lookup. */
private val placeAccents: Map<SettingsSearchDestination, Color> = mapOf(
    SettingsSearchDestination.Appearance to ConfigureAccents.appearance,
    SettingsSearchDestination.AppLock to ConfigureAccents.appLock,
    SettingsSearchDestination.Notifications to ConfigureAccents.notifications,
    SettingsSearchDestination.Backup to ConfigureAccents.backup,
    SettingsSearchDestination.AntiCensorship to ConfigureAccents.antiCensorship,
    SettingsSearchDestination.Diagnostics to ConfigureAccents.diagnostics,
    SettingsSearchDestination.Developer to ConfigureAccents.advanced,
    SettingsSearchDestination.About to ConfigureAccents.about,
)
