/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.ActivityLogEntry
import com.bernaferrari.bravedns.database.ConnectionTrackerRepository
import com.bernaferrari.bravedns.database.DnsLogRepository
import com.bernaferrari.bravedns.ui.compose.common.RethinkAdaptiveWidth
import com.bernaferrari.bravedns.ui.compose.common.isExpanded
import com.bernaferrari.bravedns.ui.compose.dns.DnsLogRow
import com.bernaferrari.bravedns.ui.compose.theme.RethinkToolbarIconToggle
import com.bernaferrari.bravedns.ui.compose.theme.RethinkToolbarRow
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

/**
 * The controls over the log, and the entries: rows open the sheet, or at Expanded width select
 * into the pane beside the list (the newest entry until one is picked).
 */
@Composable
internal fun ActivityLogPanes(
    items: LazyPagingItems<ActivityLogEntry>,
    filters: LogFilters,
    emptyMessage: String,
    onOpenFilters: () -> Unit,
    connections: ConnectionTrackerRepository,
    lookups: DnsLogRepository,
    loadFavIcon: Boolean,
    onOpenSheet: (LogSheetTarget) -> Unit,
) {
    val listState = rememberLazyListState()
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    LogsAdaptiveLayout(
        controls = {
            val active = activeFilters(filters)
            LogControls(
                verdict = filters.verdict,
                onVerdict = filters::selectVerdict,
                filtersActive = active.isNotEmpty(),
                onFilters = onOpenFilters,
                active = active,
            )
        },
        list = { listModifier, twoPane ->
            LogsPagedListContent(
                items = items,
                listState = listState,
                emptyMessage = emptyMessage,
                modifier = listModifier
            ) { entry, index, itemCount ->
                val selected = twoPane && entry.key == (selectedKey ?: items.firstLoadedId { it.key })
                val onSelect = if (twoPane) ({ selectedKey = entry.key }) else ({ onOpenSheet(entry.sheetTarget()) })
                ActivityLogRow(entry, index, itemCount, selected, loadFavIcon, onSelect, onOpenSheet)
            }
        },
        pane = { paneModifier ->
            val selected = items.loadedOrFirst(selectedKey) { it.key }
            val lookup = selected?.lookup
            if (lookup != null) {
                DnsLogPane(lookup, lookups, loadFavIcon, { onOpenSheet(LogSheetTarget.Lookup(it)) }, paneModifier)
            } else {
                ConnectionLogPane(selected?.connection, connections, { onOpenSheet(LogSheetTarget.Connection(it)) }, paneModifier)
            }
        },
    )
}

/** One timeline entry: a connection row, or a lookup row. */
@Composable
private fun ActivityLogRow(
    entry: ActivityLogEntry,
    index: Int,
    itemCount: Int,
    selected: Boolean,
    loadFavIcon: Boolean,
    onSelect: () -> Unit,
    onOpenSheet: (LogSheetTarget) -> Unit,
) {
    entry.connection?.let { ct ->
        ConnectionRow(ct = ct, index = index, itemCount = itemCount, selected = selected, onSelect = onSelect)
    }
    entry.lookup?.let { log ->
        DnsLogRow(
            log = log,
            loadFavIcon = loadFavIcon,
            isRethinkDns = false,
            onShowBlocklist = { onOpenSheet(LogSheetTarget.Lookup(it)) },
            index = index,
            itemCount = itemCount,
            selected = selected,
            onSelect = onSelect,
        )
    }
}

private fun ActivityLogEntry.sheetTarget(): LogSheetTarget =
    connection?.let { LogSheetTarget.Connection(it) } ?: LogSheetTarget.Lookup(checkNotNull(lookup))

/**
 * One control row: the verdict group (icons, the selected one labelled) and the Filters button,
 * dotted while any filter is set; under it, the active filters as removable chips.
 */
@Composable
private fun LogControls(
    verdict: RethinkLogFilter,
    onVerdict: (RethinkLogFilter) -> Unit,
    filtersActive: Boolean,
    onFilters: () -> Unit,
    active: List<RethinkActiveLogFilter>,
) {
    RethinkLogsControlsDeck {
        RethinkToolbarRow {
            RethinkLogFilterRow(
                selected = verdict,
                strings = logListStrings(),
                onSelected = onVerdict,
                modifier = Modifier.weight(1f)
            )
            RethinkToolbarIconToggle(
                icon = MaterialSymbols.Filled.FilterList,
                // Same 40dp as the compact verdict group beside it.
                size = SharedDimensions.buttonHeightSm,
                contentDescription = stringResource(R.string.activity_filters),
                selected = filtersActive,
                showSelectedDot = true,
                onClick = onFilters,
            )
        }
    }
    val remove = stringResource(R.string.activity_filter_remove)
    RethinkActiveLogFilters(filters = active, removeDescription = { remove.format(it) })
}

@Composable
private fun <T : Any> LogsPagedListContent(
    items: LazyPagingItems<T>,
    listState: LazyListState,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    rowContent: @Composable (item: T, index: Int, itemCount: Int) -> Unit
) {
    val refreshState = items.loadState.refresh
    val itemCount = items.itemCount
    val isLoading = refreshState is LoadState.Loading && itemCount == 0
    val isEmpty = refreshState is LoadState.NotLoading && itemCount == 0
    val hasLoadError = refreshState is LoadState.Error && itemCount == 0
    val density = LocalDensity.current
    val navBarBottomInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    val listContentState = when {
        isLoading -> RethinkPagedLogListState.Loading
        isEmpty -> RethinkPagedLogListState.Empty
        hasLoadError -> RethinkPagedLogListState.Error { items.retry() }
        else -> RethinkPagedLogListState.Content
    }
    RethinkPagedLogList(
        itemCount = itemCount,
        state = listContentState,
        strings = logListStrings().copy(empty = emptyMessage),
        listState = listState,
        modifier = modifier,
        bottomPadding = SharedDimensions.screenPaddingHorizontal + navBarBottomInset,
    ) { index, total ->
        val item = items[index] ?: return@RethinkPagedLogList
        rowContent(item, index, total)
    }
}

/**
 * Compact and medium widths: filters over one list whose rows expand in place. Expanded width:
 * the list on the left, the selected entry's details in a pane on the right.
 */
@Composable
private fun LogsAdaptiveLayout(
    controls: @Composable () -> Unit,
    list: @Composable (modifier: Modifier, twoPane: Boolean) -> Unit,
    pane: @Composable (modifier: Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val navBarBottomInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    RethinkAdaptiveWidth(Modifier.fillMaxSize()) { widthClass ->
        if (widthClass.isExpanded) {
            Row(Modifier.fillMaxSize().padding(end = SharedDimensions.screenPaddingHorizontal)) {
                Column(Modifier.weight(LIST_PANE_WEIGHT).fillMaxHeight()) {
                    controls()
                    list(Modifier.weight(1f), true)
                }
                pane(
                    Modifier
                        .weight(1f)
                        .padding(
                            top = SharedDimensions.spacingXs,
                            bottom = SharedDimensions.screenPaddingHorizontal + navBarBottomInset,
                        )
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                controls()
                list(Modifier.weight(1f), false)
            }
        }
    }
}

private const val LIST_PANE_WEIGHT = 1.15f

/** The selected entry if it is loaded, else the newest one, so the pane is never empty by default. */
private fun <T : Any, K> LazyPagingItems<T>.loadedOrFirst(selectedId: K?, id: (T) -> K): T? {
    val loaded = itemSnapshotList.items
    return loaded.firstOrNull { selectedId != null && id(it) == selectedId } ?: loaded.firstOrNull()
}

private fun <T : Any, K> LazyPagingItems<T>.firstLoadedId(id: (T) -> K): K? =
    itemSnapshotList.items.firstOrNull()?.let(id)
