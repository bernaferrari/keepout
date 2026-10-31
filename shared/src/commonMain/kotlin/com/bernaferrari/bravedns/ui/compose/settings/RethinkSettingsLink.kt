/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.settings

import com.bernaferrari.bravedns.ui.compose.theme.RethinkFormSection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListGroup
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListItem
import com.bernaferrari.bravedns.ui.compose.theme.cardPositionFor

/** One row of [RethinkSettingsLinkGroup]. [opensScreen] adds the chevron; actions in place don't. */
data class RethinkSettingsLink(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val description: String? = null,
    val opensScreen: Boolean = true,
)

/**
 * A titled group of destinations and one-tap actions (the Troubleshooting and Developer tools of
 * Settings › Logs & diagnostics). Nothing at all when [links] is empty.
 */
@Composable
fun RethinkSettingsLinkGroup(
    title: String,
    links: List<RethinkSettingsLink>,
    modifier: Modifier = Modifier,
) {
    if (links.isEmpty()) return
    RethinkFormSection(title, modifier = modifier) { RethinkSettingsLinkRows(links) }
}

/** [links] as one grouped list (no header), for a section that holds more than links. */
@Composable
fun RethinkSettingsLinkRows(links: List<RethinkSettingsLink>) {
    if (links.isEmpty()) return
    RethinkListGroup {
        links.forEachIndexed { index, link ->
            RethinkListItem(
                headline = link.title,
                supporting = link.description,
                leadingIcon = link.icon,
                position = cardPositionFor(index, links.lastIndex),
                showTrailingChevron = link.opensScreen,
                onClick = link.onClick,
            )
        }
    }
}
