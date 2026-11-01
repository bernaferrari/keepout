/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.ConnectionTracker
import com.bernaferrari.bravedns.database.ConnectionTrackerRepository
import com.bernaferrari.bravedns.database.DnsLog
import com.bernaferrari.bravedns.database.DnsLogRepository
import com.bernaferrari.bravedns.ui.compose.common.AppIcon
import com.bernaferrari.bravedns.ui.compose.dns.dnsLogModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Wide layout: the selected connection, and where its app connects. Its fixes open in the sheet. */
@Composable
internal fun ConnectionLogPane(
    selected: ConnectionTracker?,
    repository: ConnectionTrackerRepository,
    onOpenSheet: (ConnectionTracker) -> Unit,
    modifier: Modifier,
) {
    var groups by remember { mutableStateOf<List<RethinkLogGroup>>(emptyList()) }
    LaunchedEffect(selected?.uid) {
        val uid = selected?.uid ?: return@LaunchedEffect
        groups = withContext(Dispatchers.IO) { repository.getDestinationsForUid(uid) }
            .map { RethinkLogGroup(it.label, it.count, it.blocked, monospace = true) }
    }
    RethinkLogDetailPane(
        model = selected?.let { connectionLogModel(it) },
        groups = groups,
        strings = logPaneStrings(R.string.log_pane_network_hosts),
        modifier = modifier,
        onOpenSheet = selected?.let { ct -> { onOpenSheet(ct) } },
    )
}

/** Wide layout: the selected query, and the apps that looked its domain up. */
@Composable
internal fun DnsLogPane(
    selected: DnsLog?,
    repository: DnsLogRepository,
    loadFavIcon: Boolean,
    onOpenSheet: (DnsLog) -> Unit,
    modifier: Modifier,
) {
    var groups by remember { mutableStateOf<List<RethinkLogGroup>>(emptyList()) }
    LaunchedEffect(selected?.queryStr) {
        val domain = selected?.queryStr ?: return@LaunchedEffect
        groups = withContext(Dispatchers.IO) { repository.getAppsQueryingDomain(domain) }
            .map { group -> RethinkLogGroup(group.label, group.count, group.blocked, icon = { AppIcon(group.packageName, appName = group.label, size = Dp.Unspecified) }) }
    }
    RethinkLogDetailPane(
        model = selected?.let { dnsLogModel(it, loadFavIcon, false, onOpenSheet) },
        groups = groups,
        strings = logPaneStrings(R.string.log_pane_dns_apps),
        modifier = modifier,
        onOpenSheet = selected?.let { log -> { onOpenSheet(log) } },
    )
}

@Composable
private fun logPaneStrings(groupsTitle: Int) = RethinkLogPaneStrings(
    groupsTitle = stringResource(groupsTitle),
    emptySelection = stringResource(R.string.log_pane_empty),
    detailsTitle = stringResource(R.string.log_pane_details),
    openSheet = stringResource(R.string.log_pane_fix),
)
