package com.bernaferrari.bravedns.ui.compose.statistics

import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import com.bernaferrari.bravedns.ui.compose.theme.RethinkMetric
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bernaferrari.bravedns.ui.compose.theme.SectionHeader
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.ui.compose.theme.rethinkConnectedRowShape

data class RethinkUsageOverview(
    val download: String,
    val upload: String,
    val connections: String,
)

/**
 * Usage totals as one connected tile group (download first), matching the grouped-list surface
 * language instead of a bordered card.
 */
@Composable
fun RethinkUsageOverviewCard(
    overview: RethinkUsageOverview,
    overallLabel: String,
    downloadLabel: String,
    uploadLabel: String,
    connectionsLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        SectionHeader(title = overallLabel)
        // IntrinsicSize.Min + fillMaxHeight keeps the three tiles equal height even when one
        // value or label wraps (large font scales, long translations).
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // One value size for the row: the largest that fits the widest value, so "602.9 MB"
            // never sits smaller than "13.5 MB" beside it.
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val base = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
            val minSize = MaterialTheme.typography.titleSmall.fontSize
            val values = listOf(overview.download, overview.upload, overview.connections)
            val tileText = (maxWidth - SharedDimensions.spacingGridTile * 2) / 3 - SharedDimensions.cardPaddingSm * 2
            val widest = values.maxOf { with(density) { measurer.measure(it, base, maxLines = 1).size.width.toDp() } }
            val valueSize = if (widest <= tileText) base.fontSize else (base.fontSize.value * (tileText / widest)).coerceAtLeast(minSize.value).sp
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingGridTile),
            ) {
                // Neutral values: in this app red and green mean blocked and allowed, not direction.
                UsageTile(downloadLabel, overview.download, valueSize, 0, Modifier.weight(1f).fillMaxHeight())
                UsageTile(uploadLabel, overview.upload, valueSize, 1, Modifier.weight(1f).fillMaxHeight())
                UsageTile(connectionsLabel, overview.connections, valueSize, 2, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun UsageTile(label: String, value: String, valueSize: TextUnit, index: Int, modifier: Modifier) {
    Surface(modifier = modifier, shape = rethinkConnectedRowShape(index, count = 3), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        RethinkMetric(
            value = value,
            label = label,
            valueFontSize = valueSize,
            modifier = Modifier.padding(horizontal = SharedDimensions.cardPaddingSm, vertical = SharedDimensions.cardPadding),
        )
    }
}
