/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.configure

import androidx.compose.ui.unit.Dp
import com.bernaferrari.bravedns.ui.compose.theme.LocalRethinkMotion
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.BoxWithConstraints
import com.bernaferrari.bravedns.ui.compose.theme.rethinkListItemColors

import com.bernaferrari.bravedns.ui.compose.theme.RethinkSearchField
import com.bernaferrari.bravedns.ui.compose.theme.RethinkIconWell
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import com.bernaferrari.bravedns.ui.compose.common.RethinkAdaptiveWidth
import com.bernaferrari.bravedns.ui.compose.common.RethinkEmptyState
import com.bernaferrari.bravedns.ui.compose.common.WindowWidthClass
import com.bernaferrari.bravedns.ui.compose.common.isExpanded
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.bernaferrari.bravedns.ui.compose.theme.CardPosition
import com.bernaferrari.bravedns.ui.compose.theme.RethinkLargeTopBar
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListItem
import com.bernaferrari.bravedns.ui.compose.theme.SectionHeader
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.ui.compose.theme.cardPositionFor
import com.bernaferrari.bravedns.ui.compose.theme.rethinkGroupedListPairShape
import com.bernaferrari.bravedns.ui.compose.theme.rethinkGroupedGridCellShape

enum class RethinkConfigureLayout { GridFour, GridTriad, List }

data class RethinkConfigureEntry(
    val id: String,
    val title: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val keywords: List<String> = emptyList(),
    val accent: Color = Color.Unspecified,
)

data class RethinkConfigureSection(
    val title: String,
    val entries: List<RethinkConfigureEntry>,
    val layout: RethinkConfigureLayout,
    val subtitle: String? = null,
)

data class RethinkConfigureSearchEntry(
    val entry: RethinkConfigureEntry,
    val path: String,
)

data class RethinkConfigureStrings(
    val title: String,
    val searchHint: String,
    val openSearch: String,
    val closeSearch: String,
    val clearSearch: String,
    val noResultsTitle: String,
    val noResultsSubtitle: String,
)

@Composable
fun RethinkConfigureScreen(
    sections: List<RethinkConfigureSection>,
    searchEntries: List<RethinkConfigureSearchEntry>,
    strings: RethinkConfigureStrings,
    searchOpen: Boolean,
    query: String,
    onSearchOpenChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Optional appearance block (theme mode + color swatches), shown inline on the hub. */
    appearanceContent: (@Composable () -> Unit)? = null,
    /** How many [sections] come before [appearanceContent]; the rest follow it. */
    appearanceAfter: Int = sections.size,
    /** Bump to scroll the hub to [appearanceContent] (a search result for theme or color). */
    revealAppearance: Int = 0,
) {
    val searchScroll = TopAppBarDefaults.pinnedScrollBehavior()
    val browseScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehavior = if (searchOpen) searchScroll else browseScroll
    val focusRequester = remember { FocusRequester() }
    val showBrowse = rethinkConfigureShowsBrowse(searchOpen, query)
    val results = rethinkConfigureFilterResults(searchEntries, query)
    fun setSearchOpen(open: Boolean) {
        rethinkConfigureOnSearchOpenChange(open, onSearchOpenChange, onQueryChange)
    }
    LaunchedEffect(searchOpen) {
        if (searchOpen) focusRequester.requestFocus()
    }

    // Wide windows: one centered column (top bar included, so the title aligns with the
    // content) wide enough for a 4-up grid, but rows never stretch edge to edge.
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            // Paint the full window; only the column is width-limited.
            .background(MaterialTheme.colorScheme.surface)
            .wrapContentWidth()
            .widthIn(max = ConfigureMaxWidth)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (searchOpen) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { setSearchOpen(false) }) {
                            Icon(MaterialSymbols.AutoMirrored.Filled.ArrowBack, strings.closeSearch)
                        }
                    },
                    title = {
                        RethinkSearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            placeholder = strings.searchHint,
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            clearQueryContentDescription = strings.clearSearch,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            // The back arrow already says "search mode"; no second glyph.
                            leadingContent = null,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            } else {
                RethinkLargeTopBar(
                    title = strings.title,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = { setSearchOpen(true) }) {
                            Icon(MaterialSymbols.Filled.Search, strings.openSearch)
                        }
                    },
                )
            }
        },
    ) { padding ->
        RethinkAdaptiveWidth(Modifier.fillMaxSize().padding(padding)) { widthClass ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val titleSize = rememberGridTitleSize(sections, widthClass.isExpanded, maxWidth)
            CompositionLocalProvider(LocalConfigureTitleSize provides titleSize) {
            val listState = rememberLazyListState()
            val reducedMotion = LocalRethinkMotion.current.reducedMotion
            LaunchedEffect(revealAppearance) {
                if (revealAppearance > 0 && appearanceContent != null) {
                    val index = appearanceAfter.coerceIn(0, sections.size)
                    if (reducedMotion) listState.scrollToItem(index) else listState.animateScrollToItem(index)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // The root navigation owns the system/navigation-bar inset. Keep a deliberate tail here
                // as well: the final expressive tile is taller than a list row and needs room to scroll
                // fully above the persistent navigation bar.
                contentPadding = PaddingValues(
                    start = SharedDimensions.screenPaddingHorizontal,
                    top = SharedDimensions.spacingMd,
                    end = SharedDimensions.screenPaddingHorizontal,
                    bottom = SharedDimensions.spacing3xl + SharedDimensions.spacingSm,
                ),
                verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingLg),
            ) {
                if (showBrowse) {
                    // Protection controls lead; theming is a one-time preference and sits low.
                    val split = appearanceAfter.coerceIn(0, sections.size)
                    sections.take(split).forEach { section -> item { ConfigureSection(section, widthClass) } }
                    appearanceContent?.let { content ->
                        item(key = "appearance") { content() }
                    }
                    sections.drop(split).forEach { section -> item { ConfigureSection(section, widthClass) } }
                    item { Spacer(Modifier.height(SharedDimensions.spacingSm)) }
                } else {
                    item {
                        if (results.isEmpty()) NoResults(strings) else SearchResults(results, query)
                    }
                }
            }
            }
            }
        }
    }
}

@Composable
private fun ConfigureSection(section: RethinkConfigureSection, widthClass: WindowWidthClass) {
    Column {
        // A section without a title (About, last on the hub) is one row standing on its own.
        if (section.title.isNotBlank()) SectionHeader(section.title)
        section.subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = SharedDimensions.spacingLg, top = SharedDimensions.spacingGridTile),
            )
        }
        when (section.layout) {
            RethinkConfigureLayout.GridFour -> FourGrid(section.entries, widthClass)
            RethinkConfigureLayout.GridTriad -> TriadGrid(section.entries, widthClass)
            RethinkConfigureLayout.List -> EntryList(section.entries)
        }
    }
}

@Composable private fun FourGrid(entries: List<RethinkConfigureEntry>, widthClass: WindowWidthClass) {
    // A host that adds or hides an entry must not make the whole section vanish.
    if (entries.size != 4) {
        AdaptiveTileGrid(entries, columns = if (widthClass.isExpanded) expandedColumns(entries.size) else 2)
        return
    }
    if (widthClass.isExpanded) {
        AdaptiveTileGrid(entries, columns = expandedColumns(entries.size))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingGridTile)) {
        GridRow(entries[0], entries[1], rethinkGroupedListPairShape(true, CardPosition.First), rethinkGroupedListPairShape(false, CardPosition.First), 0)
        GridRow(entries[2], entries[3], rethinkGroupedListPairShape(true, CardPosition.Last), rethinkGroupedListPairShape(false, CardPosition.Last), 2)
    }
}
@Composable private fun TriadGrid(entries: List<RethinkConfigureEntry>, widthClass: WindowWidthClass) {
    if (entries.size < 2) return
    if (widthClass.isExpanded) {
        AdaptiveTileGrid(entries, columns = expandedColumns(entries.size))
        return
    }
    Column {
        GridRow(entries[0], entries[1], rethinkGroupedListPairShape(true, CardPosition.First), rethinkGroupedListPairShape(false, CardPosition.First), 0)
        // Full-width entries are list rows, so they line up exactly with the list sections below
        // (same inset, type and spacing) instead of being wide grid tiles.
        val rest = entries.drop(2)
        rest.forEachIndexed { index, entry ->
            ConfigureListRow(entry, if (index == rest.lastIndex) CardPosition.Last else CardPosition.Middle)
        }
    }
}

@Composable
private fun AdaptiveTileGrid(entries: List<RethinkConfigureEntry>, columns: Int) {
    val rows = entries.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingGridTile)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingGridTile),
            ) {
                // A short last row stretches its tiles across, so an odd count never leaves a hole.
                row.forEachIndexed { column, entry ->
                    GridTile(
                        entry,
                        rethinkGroupedGridCellShape(column, row.size, cardPositionFor(rowIndex, rows.lastIndex)),
                        Modifier.weight(1f).fillMaxHeight(),
                        rowIndex * columns + column,
                    )
                }
            }
        }
    }
}

@Composable private fun GridRow(
    first: RethinkConfigureEntry,
    second: RethinkConfigureEntry,
    firstShape: Shape,
    secondShape: Shape,
    startIndex: Int,
) {
    // IntrinsicSize.Min + fillMaxHeight: the two tiles of a row are always one height.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingGridTile)) {
        GridTile(first, firstShape, Modifier.weight(1f).fillMaxHeight(), startIndex)
        GridTile(second, secondShape, Modifier.weight(1f).fillMaxHeight(), startIndex + 1)
    }
}
@Composable private fun GridTile(entry: RethinkConfigureEntry, shape: Shape, modifier: Modifier, index: Int) {
    val colors = configureTileColors(entry.accent)
    Surface(
        onClick = entry.onClick,
        shape = shape,
        color = colors.fill,
        modifier = modifier.clip(shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 16dp in, like the list rows (General, About) on the same screen, so every icon lines up.
                .padding(horizontal = SharedDimensions.spacingLg, vertical = SharedDimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSmMd),
        ) {
            ConfigureIconWell(colors) { entry.icon() }
            Column(modifier = Modifier.weight(1f)) {
                // Same type role as RethinkListItem, at the screen's shared size: one line, always.
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = LocalConfigureTitleSize.current),
                    fontWeight = FontWeight.Medium,
                    color = colors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                entry.subtitle?.let { subtitle ->
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * One title size for every grid tile: the largest that keeps the longest half-width title on one
 * line beside its icon, so a narrow phone or large font never hyphenates a title or makes one tile
 * taller than its neighbour. Wide windows keep the full size.
 */
@Composable
private fun rememberGridTitleSize(sections: List<RethinkConfigureSection>, expanded: Boolean, width: Dp): TextUnit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
    val halfTile = (width - SharedDimensions.screenPaddingHorizontal * 2 - SharedDimensions.spacingGridTile) / 2
    val besideIcon = halfTile - SharedDimensions.spacingLg * 2 - SharedDimensions.iconContainerMd - SharedDimensions.spacingSmMd
    val widest = sections
        .filter { it.layout != RethinkConfigureLayout.List }
        .flatMap { section -> if (section.layout == RethinkConfigureLayout.GridTriad) section.entries.take(2) else section.entries }
        .maxOfOrNull { with(density) { measurer.measure(it.title, titleStyle, maxLines = 1).size.width.toDp() } }
    if (expanded || widest == null || widest <= besideIcon) return titleStyle.fontSize
    return (titleStyle.fontSize.value * (besideIcon / widest)).coerceAtLeast(MaterialTheme.typography.labelLarge.fontSize.value).sp
}

/** The one title size every grid tile on this screen uses (decided once, from the longest title). */
private val LocalConfigureTitleSize = staticCompositionLocalOf { TextUnit.Unspecified }

@Composable private fun EntryList(entries: List<RethinkConfigureEntry>) = Column {
    entries.forEachIndexed { index, entry ->
        ConfigureListRow(entry, cardPositionFor(index, entries.lastIndex))
    }
}

@Composable
private fun ConfigureListRow(entry: RethinkConfigureEntry, position: CardPosition) {
    val colors = configureTileColors(entry.accent)
    RethinkListItem(
        headline = entry.title,
        supporting = entry.subtitle,
        position = position,
        onClick = entry.onClick,
        colors = rethinkListItemColors(defaultContainerColor = colors.fill),
        leadingContent = { ConfigureIconWell(colors) { entry.icon() } },
    )
}

@Composable
private fun ConfigureIconWell(colors: ConfigureTileColors, icon: @Composable () -> Unit) = RethinkIconWell(
    containerColor = colors.well,
    contentColor = colors.icon,
    shape = RoundedCornerShape(SharedDimensions.iconContainerRadius),
    content = icon,
)
@Composable private fun NoResults(strings: RethinkConfigureStrings) =
    RethinkEmptyState(icon = MaterialSymbols.Filled.Search, title = strings.noResultsTitle, message = strings.noResultsSubtitle, contained = true)
@Composable
private fun SearchResults(results: List<RethinkConfigureSearchEntry>, query: String) {
    val matchStyle = SpanStyle(color = MaterialTheme.colorScheme.primary)
    Column {
        results.forEachIndexed { index, result ->
            val colors = configureTileColors(result.entry.accent)
            val supporting = rethinkConfigureSearchSupporting(result, query)
            RethinkListItem(
                headline = result.entry.title,
                headlineAnnotated = highlightConfigureQuery(result.entry.title, query, matchStyle),
                supporting = supporting,
                supportingAnnotated = highlightConfigureQuery(supporting, query, matchStyle),
                position = cardPositionFor(index, results.lastIndex),
                onClick = result.entry.onClick,
                colors = rethinkListItemColors(defaultContainerColor = colors.fill),
                showTrailingChevron = true,
                leadingContent = { ConfigureIconWell(colors) { result.entry.icon() } },
            )
        }
    }
}

private val ConfigureMaxWidth = 960.dp

/** One row for up to four tiles, else balanced rows of at most three: never a lone orphan. */
private fun expandedColumns(count: Int): Int = when {
    count <= 4 -> count.coerceAtLeast(1)
    count % 3 == 1 -> 4
    else -> 3
}
