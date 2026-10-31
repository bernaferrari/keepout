/* Copyright 2026 RethinkDNS and its authors */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.bernaferrari.bravedns.ui.compose.logs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.bernaferrari.bravedns.ui.compose.theme.LocalRethinkMotion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bernaferrari.bravedns.ui.compose.navigation.RethinkActivityView
import com.bernaferrari.bravedns.ui.compose.theme.RethinkChoiceOption
import com.bernaferrari.bravedns.ui.compose.theme.RethinkConnectedChoiceGroup
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

/**
 * The Activity tab's **Log · Insights** switch, set as the top bar's title: the bottom bar
 * already says "Activity", so the header spends its one row on the choice instead.
 */
@Composable
fun RethinkActivityViewSwitch(
    selected: RethinkActivityView,
    onSelect: (RethinkActivityView) -> Unit,
    logLabel: String,
    insightsLabel: String,
    modifier: Modifier = Modifier,
) = RethinkTitleSwitch(
    options = listOf(RethinkTitleSwitchOption(RethinkActivityView.Log, logLabel), RethinkTitleSwitchOption(RethinkActivityView.Insights, insightsLabel)),
    selected = selected,
    onSelect = onSelect,
    modifier = modifier,
)

/** One word of a [RethinkTitleSwitch]; [count] reads after it, small and quiet ("Domains 12"). */
data class RethinkTitleSwitchOption<T>(val value: T, val label: String, val count: Int? = null)

/**
 * Two or three views of one screen as its title (Log · Insights, Domains · IPs): the chosen one in
 * full ink, the others muted, all bold so switching only shifts ink and nothing changes width.
 * Each word is a tab to TalkBack.
 */
@Composable
fun <T> RethinkTitleSwitch(
    options: List<RethinkTitleSwitchOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalRethinkMotion.current
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val color by animateColorAsState(
                // Muted = the same ink at reduced alpha, so the gap holds in light and dark.
                targetValue = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 1f else MUTED_VIEW_ALPHA),
                animationSpec = if (motion.reducedMotion) snap() else tween(motion.durationMedium),
                label = "title_switch_color",
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(SharedDimensions.cornerRadiusMd))
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(option.value) })
                    .heightIn(min = SharedDimensions.touchTargetMin)
                    .padding(horizontal = SharedDimensions.spacingXs),
                contentAlignment = Alignment.CenterStart,
            ) {
                // The count sits on the word's baseline, like a footnote figure, not mid-height.
                Row(horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingXs)) {
                    Text(
                        option.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color, maxLines = 1,
                        modifier = Modifier.alignByBaseline(),
                    )
                    option.count?.let {
                        Text(
                            it.toString(),
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = color.copy(alpha = color.alpha * COUNT_ALPHA),
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
        }
    }
}

private const val COUNT_ALPHA = 0.6f
private const val MUTED_VIEW_ALPHA = 0.42f

/**
 * The header of an Activity view: one row with the [viewSwitch] as its title and the view's
 * [actions]. It keeps the surface colour while the list scrolls under it and never changes
 * between views, so switching only swaps what is below.
 */
@Composable
fun RethinkActivityTopBar(
    viewSwitch: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        // Pull the first label's padding back so its ink lines up with the content gutter.
        title = { Box(Modifier.offset(x = -SharedDimensions.spacingXs)) { viewSwitch() } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * Which log the Activity tab lists: connections or DNS lookups. Labelled on both halves (the old
 * top-bar switch showed the inactive half as a bare icon).
 */
@Composable
fun RethinkLogTypeSwitch(
    selected: RethinkLogTab,
    onSelect: (RethinkLogTab) -> Unit,
    connectionsLabel: String,
    lookupsLabel: String,
    modifier: Modifier = Modifier,
) {
    RethinkConnectedChoiceGroup(
        options = listOf(
            RethinkChoiceOption(RethinkLogTab.Network, connectionsLabel, MaterialSymbols.Filled.SyncAlt),
            RethinkChoiceOption(RethinkLogTab.Dns, lookupsLabel, MaterialSymbols.Filled.Dns),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SharedDimensions.screenPaddingHorizontal)
            .padding(top = SharedDimensions.spacingXs),
    )
}
