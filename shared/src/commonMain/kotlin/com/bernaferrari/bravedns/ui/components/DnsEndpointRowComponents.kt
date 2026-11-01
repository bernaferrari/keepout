/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.components

import com.bernaferrari.bravedns.ui.compose.theme.rethinkGroupedRowTopGap
import com.bernaferrari.bravedns.ui.compose.theme.rethinkGroupedListShape
import com.bernaferrari.bravedns.ui.compose.theme.CardPosition
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bernaferrari.bravedns.ui.compose.theme.RethinkPill
import com.bernaferrari.bravedns.ui.compose.theme.RethinkPillSize
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions

/** The host chooses the action behavior; commonMain owns the expressive endpoint row itself. */
enum class DnsRowAction { Info, Edit, Delete }

enum class DnsRowSelection { Radio, Checkbox }

/**
 * Set by a list that groups rows under a provider header: [position] joins the rows into one
 * grouped list, and a title starting with [provider] ("Adguard Family" under "AdGuard") shows only
 * its variant ("Family"), or [standard] when nothing is left.
 */
data class DnsRowContext(val position: CardPosition, val provider: String, val standard: String)

val LocalDnsRowContext = staticCompositionLocalOf<DnsRowContext?> { null }

/** [title] without the provider its group header already names. */
fun dnsRowTitle(title: String, context: DnsRowContext?): String {
    if (context == null || !title.startsWith(context.provider, ignoreCase = true)) return title
    val rest = title.substring(context.provider.length).trim(' ', '-', '·', '_')
    return rest.ifBlank { context.standard }.replaceFirstChar { it.uppercaseChar() }
}

@Composable
fun DnsEndpointRow(
    title: String,
    supporting: String?,
    selected: Boolean,
    action: DnsRowAction,
    selection: DnsRowSelection = DnsRowSelection.Radio,
    onActionClick: () -> Unit,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    /** A short fact beside the title, e.g. the protocol on a provider list ("DoH"). */
    badge: String? = null,
    /** What the server does, above the live status in [supporting]. */
    description: String? = null,
    /** Accessibility label for the trailing action (info, edit or delete). */
    actionLabel: String? = null,
) {
    val context = LocalDnsRowContext.current
    val shape = context?.let { rethinkGroupedListShape(it.position) } ?: RoundedCornerShape(SharedDimensions.groupCornerRadius)
    val outer = if (context != null) Modifier.padding(top = rethinkGroupedRowTopGap(context.position)) else Modifier.padding(vertical = 4.dp)
    val shownTitle = dnsRowTitle(title, context)
    Surface(
        onClick = { onSelectionChange(if (selection == DnsRowSelection.Radio) true else !selected) },
        modifier = modifier.fillMaxWidth().then(outer).clip(shape),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f) else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            // 16dp in, like every list row; the trailing controls keep their own touch padding.
            modifier = Modifier.fillMaxWidth().padding(start = SharedDimensions.spacingLg, end = SharedDimensions.spacingXs, top = SharedDimensions.spacingSmMd, bottom = SharedDimensions.spacingSmMd),
            horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSmMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingGridTile)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(shownTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
                    if (!badge.isNullOrEmpty()) RethinkPill(badge, size = RethinkPillSize.Small)
                }
                if (!description.isNullOrEmpty()) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!supporting.isNullOrEmpty()) {
                    // The live status of the chosen server, distinct from its description.
                    Text(
                        supporting,
                        style = if (description.isNullOrEmpty()) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelMedium,
                        color = if (description.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                // The action and the selection read as one cluster: their 48dp targets touch, no gap.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DnsEndpointActionButton(action, actionLabel, onActionClick)
                    when (selection) {
                        DnsRowSelection.Radio -> RadioButton(selected, onClick = { onSelectionChange(true) })
                        DnsRowSelection.Checkbox -> Checkbox(selected, onCheckedChange = onSelectionChange)
                    }
                }
            }
        }
    }
}

/** A bare icon button: a different tinted plate per action read as three kinds of row. */
@Composable
private fun DnsEndpointActionButton(action: DnsRowAction, label: String?, onClick: () -> Unit) {
    val contentColor = if (action == DnsRowAction.Delete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = when (action) {
        DnsRowAction.Delete -> MaterialSymbols.Filled.DeleteOutline
        DnsRowAction.Edit -> MaterialSymbols.Filled.Edit
        DnsRowAction.Info -> MaterialSymbols.Filled.MoreHoriz
    }
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor),
        modifier = Modifier.size(SharedDimensions.touchTargetMin),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(SharedDimensions.iconSizeMd))
    }
}
