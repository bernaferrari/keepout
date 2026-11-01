/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bernaferrari.bravedns.ui.compose.common.RethinkAdaptiveWidth
import com.bernaferrari.bravedns.ui.compose.common.RethinkEmptyState
import com.bernaferrari.bravedns.ui.compose.common.RethinkLoadingState
import com.bernaferrari.bravedns.ui.compose.common.isExpanded
import com.bernaferrari.bravedns.ui.compose.theme.CardPosition
import com.bernaferrari.bravedns.ui.compose.theme.CompactEmptyState
import com.bernaferrari.bravedns.ui.compose.theme.LocalRethinkMotion
import com.bernaferrari.bravedns.ui.compose.theme.RethinkFormActionRow
import com.bernaferrari.bravedns.ui.compose.theme.RethinkFormSection
import com.bernaferrari.bravedns.ui.compose.theme.RethinkLargeTopBar
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListGroup
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListItem
import com.bernaferrari.bravedns.ui.compose.theme.RethinkModalBottomSheet
import com.bernaferrari.bravedns.ui.compose.theme.RethinkRadioListItem
import com.bernaferrari.bravedns.ui.compose.theme.RethinkSheetHeader
import com.bernaferrari.bravedns.ui.compose.theme.RethinkStatusColors
import com.bernaferrari.bravedns.ui.compose.theme.RethinkSwitchListItem
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.ui.compose.theme.cardPositionFor
import com.bernaferrari.bravedns.ui.compose.theme.rethinkListItemColors
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols
import com.bernaferrari.bravedns.ui.icons.keepout.KeepoutIcons

/** Two-line action row height (Material 72dp). */
private val AppInfoActionMinHeight = 72.dp

/**
 * How Keepout treats an app beyond its Wi‑Fi / mobile data flags. Exactly one applies; [Normal]
 * follows every rule. Hosts map their stored firewall status onto these.
 */
enum class RethinkAppMode { Normal, TrustedOnly, SkipAllFiltering, SkipUniversalRules, Excluded }

/** The tone of App detail's one status line. */
enum class RethinkAppStatusTone { Allowed, Partial, Blocked, Temporary }

/** One destination in Recent activity: the newest connection to it, blocked rows first. */
@Immutable
data class RethinkAppInfoActivityItem(
    val id: String,
    /** The domain, else the IP. */
    val title: String,
    /** "Blocked · 09:41 · 3 times", formatted by the host. */
    val subtitle: String?,
    val blocked: Boolean,
)

@Immutable
data class RethinkAppInfoState(
    /** True until the app's first lookup completes: neither the controls nor "missing" show yet. */
    val loading: Boolean,
    /** False only after loading finished and the app wasn't found. */
    val appAvailable: Boolean,
    val title: String,
    val subtitle: String? = null,
    /** The single status sentence: "Can't use the internet", "Allowed for 12 more minutes"… */
    val status: String,
    val statusTone: RethinkAppStatusTone,
    /** A quiet line under the status (proxy routing), when there is one. */
    val statusDetail: String? = null,
    val wifiBlocked: Boolean,
    val mobileBlocked: Boolean,
    val mode: RethinkAppMode,
    /** Minutes of temporary access left, or null when the app isn't temporarily allowed. */
    val tempAllowMinutesLeft: Int? = null,
    val proxyExcluded: Boolean,
    /** "Bypass all proxies" only matters when a proxy is set up (or the app already bypasses). */
    val showProxyExclude: Boolean = true,
    /** Null while loading. Blocked destinations first. */
    val activity: List<RethinkAppInfoActivityItem>? = null,
    val domainRuleCount: Int? = null,
    val ipRuleCount: Int? = null,
    /** False while a write is in flight, so rapid taps can't interleave. */
    val enabled: Boolean = true,
)

@Immutable
data class RethinkAppInfoStrings(
    val unavailable: String,
    val loading: String,
    val internetAccess: String,
    val wifi: String,
    val mobile: String,
    val allowed: String,
    val blocked: String,
    val temporaryAllow: String,
    /** "Stop temporary access · 12 min left" */
    val stopTemporaryAccess: (minutesLeft: Int) -> String,
    val recentActivity: String,
    val seeAllActivity: String,
    val noRecentActivity: String,
    val rules: String,
    /** "Domain and IP rules": the one row that opens this app's Rules screen. */
    val domainAndIpRules: String = "Domain and IP rules",
    /** Under it: "2 domains · 1 IP", or "None yet"; null counts are still loading. */
    val rulesSummary: (domains: Int?, ips: Int?) -> String? = { d, i -> if (d == null || i == null) null else "$d domains · $i IPs" },
    /** The sheet's title: "How Keepout treats this app". */
    val modeTitle: String,
    val normal: String,
    val normalDescription: String,
    val trustedOnly: String,
    val trustedOnlyDescription: String,
    val skipAllFiltering: String,
    val skipAllFilteringDescription: String,
    val skipUniversalRules: String,
    val skipUniversalRulesDescription: String,
    val exclude: String,
    val excludeDescription: String,
    /** Shown in the sheet while Exclude or Skip all filtering is the pending choice. */
    val excludeWarning: String,
    val skipAllFilteringWarning: String,
    val apply: String,
    val cancel: String,
    val more: String,
    val proxyExclude: String,
    val proxyExcludeDescription: String,
    val systemAppInfo: String,
    val wifiState: (blocked: Boolean) -> String = { if (it) "$wifi: $blocked" else "$wifi: $allowed" },
    val mobileState: (blocked: Boolean) -> String = { if (it) "$mobile: $blocked" else "$mobile: $allowed" },
)

/** Everything App detail can do. Hosts own the writes, undo and navigation. */
@Immutable
data class RethinkAppInfoActions(
    val onBackClick: () -> Unit,
    val onWifiClick: () -> Unit,
    val onMobileClick: () -> Unit,
    /** Choose a handling mode; called only after the user confirms it in the sheet. */
    val onModeChange: (RethinkAppMode) -> Unit,
    val onTempAllowChange: (Boolean) -> Unit,
    val onProxyExcludedChange: (Boolean) -> Unit,
    val onSystemAppInfo: () -> Unit,
    val onIpRules: () -> Unit,
    val onDomainRules: () -> Unit,
    val onActivityClick: ((RethinkAppInfoActivityItem) -> Unit)? = null,
    val onSeeAllActivity: (() -> Unit)? = null,
)

/**
 * Shared App detail, built around "can it use the internet?": one status line, the Wi‑Fi and
 * mobile data tiles (with temporary access when anything is blocked), recent activity, the app's
 * rules, its special handling and the rest. Android keeps lookup, icons, writes and log queries.
 */
@Composable
fun RethinkAppInfoScreen(
    state: RethinkAppInfoState,
    strings: RethinkAppInfoStrings,
    actions: RethinkAppInfoActions,
    modifier: Modifier = Modifier,
    titleLeading: (@Composable () -> Unit)? = null,
    appNotes: (@Composable () -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showModes by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            RethinkLargeTopBar(
                title = state.title,
                subtitle = state.subtitle,
                onBackClick = actions.onBackClick,
                scrollBehavior = scrollBehavior,
                titleLeading = titleLeading,
                // The package reads as the name's second line, not a caption under the icon.
                subtitleIndent = packageIndent(titleLeading != null),
            )
        },
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
    ) { padding ->
        val access: LazyListScope.() -> Unit = {
            item(key = "status") { RethinkAppStatusLine(state) }
            item(key = "access") { RethinkAppAccessSection(state, strings, actions) }
        }
        val activity: LazyListScope.() -> Unit = {
            item(key = "activity") { RethinkAppActivitySection(state, strings, actions) }
        }
        val rules: LazyListScope.() -> Unit = {
            // One group for how the firewall treats this app: its rules (one screen, Domains · IPs)
            // and its handling mode.
            item(key = "rules") {
                RethinkFormSection(strings.rules) {
                    RethinkListGroup {
                        val onlyIps = state.domainRuleCount == 0 && (state.ipRuleCount ?: 0) > 0
                        RethinkListItem(
                            headline = strings.domainAndIpRules,
                            supporting = strings.rulesSummary(state.domainRuleCount, state.ipRuleCount),
                            leadingIcon = MaterialSymbols.Filled.Rule,
                            position = CardPosition.First,
                            showTrailingChevron = true,
                            onClick = if (onlyIps) actions.onIpRules else actions.onDomainRules,
                        )
                        RethinkAppHandlingRow(state, strings, CardPosition.Last, onOpen = { showModes = true })
                    }
                }
            }
        }
        val more: LazyListScope.() -> Unit = {
            item(key = "more") {
                RethinkFormSection(strings.more) {
                    RethinkListGroup {
                        if (state.showProxyExclude) {
                            RethinkSwitchListItem(
                                headline = strings.proxyExclude,
                                checked = state.proxyExcluded,
                                onCheckedChange = actions.onProxyExcludedChange,
                                supporting = strings.proxyExcludeDescription,
                                leadingIcon = MaterialSymbols.Filled.ConversionPath,
                                position = CardPosition.First,
                            )
                        }
                        RethinkListItem(
                            headline = strings.systemAppInfo,
                            leadingIcon = MaterialSymbols.Filled.PhoneAndroid,
                            position = if (state.showProxyExclude) CardPosition.Last else CardPosition.Single,
                            showTrailingChevron = true,
                            onClick = actions.onSystemAppInfo,
                        )
                    }
                }
            }
            if (appNotes != null) item(key = "notes") { appNotes() }
            item(key = "end") { Spacer(Modifier.height(SharedDimensions.spacingSm)) }
        }
        RethinkAdaptiveWidth(Modifier.fillMaxSize().padding(padding)) { widthClass ->
            when {
                // The lookup is a quick local read: show nothing rather than flash a spinner or "missing".
                state.loading -> Spacer(Modifier.fillMaxSize())
                // Only after the lookup finished: centered in the screen; the top bar already offers back.
                !state.appAvailable -> Box(Modifier.fillMaxSize().padding(AppInfoPadding), contentAlignment = Alignment.Center) {
                    RethinkEmptyState(icon = MaterialSymbols.Filled.Apps, title = strings.unavailable)
                }
                widthClass.isExpanded -> Row(
                    // Wide windows: access and handling in one column, traffic and rules in the other.
                    Modifier.fillMaxSize().padding(horizontal = SharedDimensions.spacingSm),
                    horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSm),
                ) {
                    LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = AppInfoPadding, verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingMd)) {
                        access()
                        more()
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = AppInfoPadding, verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingMd)) {
                        activity()
                        rules()
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = AppInfoPadding,
                    verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingMd),
                ) {
                    access()
                    activity()
                    rules()
                    more()
                }
            }
        }
    }
    if (showModes && state.appAvailable) {
        RethinkAppModeSheet(
            current = state.mode,
            strings = strings,
            onDismiss = { showModes = false },
            onApply = actions.onModeChange,
        )
    }
}

private val AppInfoPadding = PaddingValues(
    start = SharedDimensions.screenPaddingHorizontal,
    end = SharedDimensions.screenPaddingHorizontal,
    top = SharedDimensions.spacingSm,
    bottom = SharedDimensions.spacing3xl,
)

@Composable
private fun RethinkAppStatusTone.color(): Color = when (this) {
    RethinkAppStatusTone.Allowed, RethinkAppStatusTone.Temporary -> RethinkStatusColors.allowed
    RethinkAppStatusTone.Partial -> RethinkStatusColors.mixed
    RethinkAppStatusTone.Blocked -> RethinkStatusColors.blocked
}

private fun RethinkAppInfoState.statusIcon(): ImageVector = when (statusTone) {
    RethinkAppStatusTone.Allowed -> MaterialSymbols.Filled.CheckCircle
    RethinkAppStatusTone.Temporary -> MaterialSymbols.Filled.Timer
    RethinkAppStatusTone.Blocked -> MaterialSymbols.Filled.DoNotDisturbOn
    RethinkAppStatusTone.Partial -> when {
        // Show what is actually limited, most specific first.
        mode == RethinkAppMode.Excluded -> MaterialSymbols.Filled.RemoveModerator
        mode == RethinkAppMode.SkipAllFiltering -> KeepoutIcons.FirewallOff
        mode == RethinkAppMode.SkipUniversalRules -> MaterialSymbols.Filled.PublicOff
        wifiBlocked -> MaterialSymbols.Filled.WifiOff
        mobileBlocked -> MaterialSymbols.Filled.MobiledataOff
        else -> MaterialSymbols.Filled.PublicOff
    }
}

/**
 * The one answer to "can it use the internet?": a pill in the status colour on a soft wash of it,
 * then any detail as plain text. Announced politely when it changes.
 */
@Composable
private fun RethinkAppStatusLine(state: RethinkAppInfoState) {
    val color by animateColorAsState(state.statusTone.color(), label = "app_status_color")
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSm),
    ) {
        Surface(shape = RoundedCornerShape(SharedDimensions.cornerRadiusFull), color = color.copy(alpha = STATUS_WASH_ALPHA)) {
            Row(
                Modifier.padding(start = SharedDimensions.spacingMd, end = SharedDimensions.spacingLg, top = SharedDimensions.spacingSm, bottom = SharedDimensions.spacingSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSm),
            ) {
                Icon(state.statusIcon(), null, tint = color, modifier = Modifier.size(SharedDimensions.iconSizeSm))
                Text(state.status, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
            }
        }
        state.statusDetail?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** How strongly the status pill's background tints toward its status colour. */
private const val STATUS_WASH_ALPHA = 0.14f

/**
 * Wi‑Fi, mobile data and, when something is blocked, temporary access: one compact group 4dp
 * apart, like a quick-settings panel, so the three read as one control.
 */
@Composable
private fun RethinkAppAccessSection(state: RethinkAppInfoState, strings: RethinkAppInfoStrings, actions: RethinkAppInfoActions) {
    RethinkFormSection(strings.internetAccess) {
        Column {
            RethinkAppNetworkPair(state, strings, actions.onWifiClick, actions.onMobileClick)
            val minutesLeft = state.tempAllowMinutesLeft
            val somethingBlocked = state.wifiBlocked || state.mobileBlocked || state.mode == RethinkAppMode.TrustedOnly
            AnimatedVisibility(
                visible = minutesLeft != null || somethingBlocked,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                FilledTonalButton(
                    onClick = { actions.onTempAllowChange(minutesLeft == null) },
                    enabled = state.enabled,
                    // 4dp under the tiles, inside the animation, so no gap is left once it hides.
                    modifier = Modifier
                        .padding(top = SharedDimensions.spacingXs)
                        .fillMaxWidth()
                        .heightIn(min = SharedDimensions.buttonHeight),
                ) {
                    Icon(
                        if (minutesLeft == null) MaterialSymbols.Filled.Timer else MaterialSymbols.Filled.TimerOff,
                        null,
                        Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(if (minutesLeft == null) strings.temporaryAllow else strings.stopTemporaryAccess(minutesLeft))
                }
            }
        }
    }
}

/** Starts the package line where the name starts, past the icon and its gap. */
private fun packageIndent(hasIcon: Boolean): Dp =
    if (hasIcon) SharedDimensions.iconContainerLg + SharedDimensions.spacingSmMd else 0.dp

@Composable
private fun RethinkAppNetworkPair(
    state: RethinkAppInfoState,
    strings: RethinkAppInfoStrings,
    onWifiClick: () -> Unit,
    onMobileClick: () -> Unit,
) {
    // Two tiles 4dp apart, the same gap as the temporary-access button below: one tight group.
    // IntrinsicSize.Min keeps both tiles the same height should a label still need two lines.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    // One label size for both tiles, the largest that keeps "Mobile data" on one line, so a
    // narrow phone or a large font doesn't stack it into a tall, bulky pill.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val textRoom = (maxWidth - SharedDimensions.spacingXs) / 2 - NetworkTileChrome
    val widest = listOf(strings.wifi, strings.mobile).maxOf { with(density) { measurer.measure(it, base, maxLines = 1).size.width.toDp() } }
    val labelSize = if (widest <= textRoom) base.fontSize else
        (base.fontSize.value * (textRoom / widest)).coerceAtLeast(MaterialTheme.typography.titleSmall.fontSize.value).sp
    Row(
        horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingXs),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
    ) {
        RethinkAppNetworkTile(
            label = strings.wifi,
            blocked = state.wifiBlocked,
            allowedLabel = strings.allowed,
            blockedLabel = strings.blocked,
            stateDescription = strings.wifiState(state.wifiBlocked),
            allowedIcon = MaterialSymbols.Filled.Wifi,
            blockedIcon = MaterialSymbols.Filled.WifiOff,
            enabled = state.enabled,
            labelSize = labelSize,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = onWifiClick,
        )
        RethinkAppNetworkTile(
            label = strings.mobile,
            blocked = state.mobileBlocked,
            allowedLabel = strings.allowed,
            blockedLabel = strings.blocked,
            stateDescription = strings.mobileState(state.mobileBlocked),
            allowedIcon = MaterialSymbols.Filled.MobiledataArrows,
            blockedIcon = MaterialSymbols.Filled.MobiledataOff,
            enabled = state.enabled,
            labelSize = labelSize,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onClick = onMobileClick,
        )
    }
    }
}

/** Everything in a network tile beside its label: 16dp start, 24dp icon, 12dp gap, 16dp end. */
private val NetworkTileChrome = SharedDimensions.spacingLg + SharedDimensions.iconSizeMd +
    SharedDimensions.spacingMd + SharedDimensions.spacingLg

/**
 * Compact allow/block network card: at least list-item-large tall, the icon on the segment itself, and
 * Allowed/Blocked status text. The title may take two lines so "Mobile data" never truncates.
 */
@Composable
private fun RethinkAppNetworkTile(
    label: String,
    blocked: Boolean,
    allowedLabel: String,
    blockedLabel: String,
    stateDescription: String,
    allowedIcon: ImageVector,
    blockedIcon: ImageVector,
    enabled: Boolean,
    labelSize: TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val motion = LocalRethinkMotion.current
    val haptics = LocalHapticFeedback.current
    val colorSpec: AnimationSpec<Color> = tween(motion.durationMedium, easing = FastOutSlowInEasing)
    // Allowed sits at the group radius; blocked morphs to a full pill, matching the
    // temporary-access button beneath it. The pill radius is half the measured height (a two-line
    // label makes the tile taller), never an oversized clamp, so the morph stays linear.
    val density = LocalDensity.current
    var tileHeight by remember { mutableStateOf(AppInfoActionMinHeight) }
    val corner by animateDpAsState(
        targetValue = if (blocked) tileHeight / 2 else SharedDimensions.groupCornerRadius,
        animationSpec = tween(motion.durationMedium, easing = FastOutSlowInEasing),
        label = "app_network_tile_corner",
    )
    val containerColor by animateColorAsState(
        targetValue = if (blocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = colorSpec,
        label = "app_network_tile_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (blocked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = colorSpec,
        label = "app_network_tile_title",
    )
    val secondaryColor by animateColorAsState(
        targetValue = if (blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = colorSpec,
        label = "app_network_tile_status",
    )
    // Same rule as the list's network segments: the icon sits directly on its segment, no
    // inner circle; allowed is tonal, blocked takes the error container's on-colour.
    val iconBlockedTint = if (blocked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val iconAllowedTint = if (blocked) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.44f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = {
            haptics.performHapticFeedback(if (blocked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onClick()
        },
        enabled = enabled,
        // One stop for TalkBack: "Wi‑Fi: blocked", a switch whose double-tap allows it.
        modifier = modifier
            .heightIn(min = AppInfoActionMinHeight)
            .onSizeChanged { tileHeight = with(density) { it.height.toDp() } }
            .semantics(mergeDescendants = true) {
            role = Role.Switch
            this.stateDescription = stateDescription
        },
        shape = RoundedCornerShape(corner),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                // Even rhythm: 16dp at both edges, 12dp top and bottom and from icon to text.
                .padding(horizontal = SharedDimensions.spacingLg, vertical = SharedDimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingMd),
        ) {
            DiagonalWipeIcon(
                blocked = blocked,
                allowedIcon = allowedIcon,
                blockedIcon = blockedIcon,
                allowedTint = iconAllowedTint,
                blockedTint = iconBlockedTint,
                contentDescription = null,
                modifier = Modifier.size(SharedDimensions.iconSizeMd),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    // Line height follows the label size, so a wrapped label stays one tight block over its status.
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = labelSize, lineHeight = 1.2.em),
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedContent(
                    targetState = blocked,
                    transitionSpec = {
                        if (motion.reducedMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (slideInVertically { height -> if (targetState) height else -height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> if (targetState) -height else height } + fadeOut())
                        }
                    },
                    label = "app_network_tile_status_text",
                ) { isBlocked ->
                    Text(
                        text = if (isBlocked) blockedLabel else allowedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Blocked destinations first; each opens the Connection sheet, then "See all activity". */
@Composable
private fun RethinkAppActivitySection(state: RethinkAppInfoState, strings: RethinkAppInfoStrings, actions: RethinkAppInfoActions) {
    val items = state.activity
    RethinkFormSection(strings.recentActivity) {
        when {
            items == null -> RethinkLoadingState(label = strings.loading)
            items.isEmpty() -> Surface(
                shape = RoundedCornerShape(SharedDimensions.groupCornerRadius),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CompactEmptyState(message = strings.noRecentActivity, icon = MaterialSymbols.Filled.Info)
            }
            else -> RethinkListGroup {
                val seeAll = actions.onSeeAllActivity
                val last = if (seeAll != null) items.size else items.lastIndex
                items.forEachIndexed { index, item ->
                    RethinkListItem(
                        headline = item.title,
                        supporting = item.subtitle,
                        singleLineHeadline = true,
                        leadingIcon = if (item.blocked) MaterialSymbols.Filled.DoNotDisturbOn else MaterialSymbols.Filled.CheckCircle,
                        colors = rethinkListItemColors(
                            leadingIconTint = if (item.blocked) RethinkStatusColors.blocked else RethinkStatusColors.allowed,
                        ),
                        position = cardPositionFor(index, last),
                        showTrailingChevron = actions.onActivityClick != null,
                        onClick = actions.onActivityClick?.let { open -> { open(item) } },
                    )
                }
                if (seeAll != null) {
                    RethinkListItem(
                        headline = strings.seeAllActivity,
                        leadingIcon = MaterialSymbols.Filled.History,
                        position = cardPositionFor(last, last),
                        showTrailingChevron = true,
                        onClick = seeAll,
                    )
                }
            }
        }
    }
}

private fun RethinkAppMode.icon(): ImageVector = when (this) {
    RethinkAppMode.Normal -> MaterialSymbols.Filled.GppGood
    RethinkAppMode.TrustedOnly -> MaterialSymbols.Filled.Verified
    RethinkAppMode.SkipAllFiltering -> KeepoutIcons.FirewallOff
    RethinkAppMode.SkipUniversalRules -> MaterialSymbols.Filled.PublicOff
    RethinkAppMode.Excluded -> MaterialSymbols.Filled.RemoveModerator
}

private fun RethinkAppInfoStrings.label(mode: RethinkAppMode): String = when (mode) {
    RethinkAppMode.Normal -> normal
    RethinkAppMode.TrustedOnly -> trustedOnly
    RethinkAppMode.SkipAllFiltering -> skipAllFiltering
    RethinkAppMode.SkipUniversalRules -> skipUniversalRules
    RethinkAppMode.Excluded -> exclude
}

private fun RethinkAppInfoStrings.description(mode: RethinkAppMode): String = when (mode) {
    RethinkAppMode.Normal -> normalDescription
    RethinkAppMode.TrustedOnly -> trustedOnlyDescription
    RethinkAppMode.SkipAllFiltering -> skipAllFilteringDescription
    RethinkAppMode.SkipUniversalRules -> skipUniversalRulesDescription
    RethinkAppMode.Excluded -> excludeDescription
}

/**
 * The current mode as a row of the Rules group (highlighted when it isn't Normal). Tapping it opens
 * the five modes as one radio group; nothing changes until Apply.
 */
@Composable
private fun RethinkAppHandlingRow(state: RethinkAppInfoState, strings: RethinkAppInfoStrings, position: CardPosition, onOpen: () -> Unit) {
    RethinkListItem(
        headline = strings.label(state.mode),
        supporting = strings.description(state.mode),
        leadingIcon = state.mode.icon(),
        position = position,
        highlighted = state.mode != RethinkAppMode.Normal,
        colors = rethinkListItemColors(highlightContainerColor = MaterialTheme.colorScheme.secondaryContainer),
        showTrailingChevron = true,
        enabled = state.enabled,
        onClick = onOpen,
    )
}

/**
 * The modes are mutually exclusive, so they read as one radio group with "Normal" first. The
 * choice is pending until Apply; Exclude and Skip all filtering say what they give up.
 */
@Composable
private fun RethinkAppModeSheet(
    current: RethinkAppMode,
    strings: RethinkAppInfoStrings,
    onDismiss: () -> Unit,
    onApply: (RethinkAppMode) -> Unit,
) {
    var pending by rememberSaveable(current) { mutableStateOf(current) }
    RethinkModalBottomSheet(
        onDismissRequest = onDismiss,
        contentPadding = PaddingValues(SharedDimensions.spacingLg),
        verticalSpacing = SharedDimensions.spacingMd,
        expandOnShow = true,
    ) { dismiss ->
        Column(
            Modifier.fillMaxWidth().heightIn(max = SharedDimensions.sheetContentMaxHeight).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingMd),
        ) {
            RethinkSheetHeader(title = strings.modeTitle)
            RethinkListGroup(Modifier.selectableGroup()) {
                RethinkAppMode.entries.forEachIndexed { index, mode ->
                    RethinkRadioListItem(
                        headline = strings.label(mode),
                        selected = pending == mode,
                        onSelect = { pending = mode },
                        supporting = strings.description(mode),
                        leadingIcon = mode.icon(),
                        position = cardPositionFor(index, RethinkAppMode.entries.lastIndex),
                    )
                }
            }
            val warning = when (pending) {
                RethinkAppMode.Excluded -> strings.excludeWarning
                RethinkAppMode.SkipAllFiltering -> strings.skipAllFilteringWarning
                else -> null
            }
            AnimatedVisibility(visible = warning != null && pending != current) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = SharedDimensions.spacingXs),
                    horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSm),
                ) {
                    Icon(MaterialSymbols.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(SharedDimensions.iconSizeSm))
                    Text(warning.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        RethinkFormActionRow(
            confirmLabel = strings.apply,
            confirmEnabled = pending != current,
            onConfirm = {
                val chosen = pending
                dismiss()
                onApply(chosen)
            },
            dismissLabel = strings.cancel,
            onDismiss = dismiss,
        )
    }
}
