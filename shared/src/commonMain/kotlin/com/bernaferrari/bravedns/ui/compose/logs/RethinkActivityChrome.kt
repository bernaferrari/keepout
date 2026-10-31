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
import androidx.compose.foundation.layout.wrapContentHeight
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
 * already says "Activity", so the header spends its one row on the choice instead. The chosen
 * view reads in full ink, the other muted; both stay labelled and are tabs to TalkBack.
 */
@Composable
fun RethinkActivityViewSwitch(
    selected: RethinkActivityView,
    onSelect: (RethinkActivityView) -> Unit,
    logLabel: String,
    insightsLabel: String,
    modifier: Modifier = Modifier,
) {
    val motion = LocalRethinkMotion.current
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(RethinkActivityView.Log to logLabel, RethinkActivityView.Insights to insightsLabel).forEach { (view, label) ->
            val isSelected = view == selected
            val color by animateColorAsState(
                // Muted = the same ink at reduced alpha, so the gap holds in light and dark.
                targetValue = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 1f else MUTED_VIEW_ALPHA),
                animationSpec = if (motion.reducedMotion) snap() else tween(motion.durationMedium),
                label = "activity_view_color",
            )
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                // Both bold, so switching only shifts ink and the words never change width.
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(SharedDimensions.cornerRadiusMd))
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(view) })
                    .heightIn(min = SharedDimensions.touchTargetMin)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = SharedDimensions.spacingXs),
            )
        }
    }
}

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
