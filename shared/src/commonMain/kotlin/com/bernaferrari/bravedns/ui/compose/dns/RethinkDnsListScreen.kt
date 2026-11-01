/* Copyright 2026 RethinkDNS and its authors */

package com.bernaferrari.bravedns.ui.compose.dns

import com.bernaferrari.bravedns.ui.components.LocalDnsRowContext
import com.bernaferrari.bravedns.ui.components.DnsRowContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.bernaferrari.bravedns.ui.compose.common.RethinkLoadingState
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListGroup
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListItem
import com.bernaferrari.bravedns.ui.compose.theme.RethinkModalBottomSheet
import com.bernaferrari.bravedns.ui.compose.theme.RethinkSheetHeader
import com.bernaferrari.bravedns.ui.compose.theme.RethinkTopBarLazyColumnScreen
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.ui.compose.theme.cardPositionFor
import com.bernaferrari.bravedns.ui.compose.theme.rethinkSection
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

/** Kept for fixtures that still describe protocols by capability tags. */
enum class RethinkDnsCapability { Fast, Private, Secure, Anonymous }

/** A DNS protocol, as a detail: what "Add server" asks and what "Servers by protocol" lists. */
data class RethinkDnsProtocol(
    val id: Int,
    /** "DNS-over-HTTPS". */
    val label: String,
    /** What it means for the user: "Encrypted over HTTPS. Most providers offer it." */
    val description: String,
    val icon: ImageVector = MaterialSymbols.Filled.Dns,
) {
    constructor(
        id: Int,
        label: String,
        title: String,
        @Suppress("UNUSED_PARAMETER") capabilities: List<RethinkDnsCapability>,
        icon: ImageVector = MaterialSymbols.Filled.Dns,
    ) : this(id, label, title, icon)
}

/** One provider (Cloudflare, Quad9…) and its servers, in the order they should be shown. */
data class RethinkDnsProviderGroup<K>(val title: String, val keys: List<K>)

data class RethinkDnsListStrings(
    val title: String,
    val subtitle: String,
    val add: String = "Add server",
    val addTitle: String = "Add a server",
    val addDescription: String = "How should this phone talk to it?",
    val byProtocol: String = "Servers by protocol",
    val byProtocolDescription: String = "Every server of one kind, and DNSCrypt relays.",
    val loading: String = "Loading servers…",
    /** A provider's plain server, when its name is only the provider ("Cloudflare" → "Standard"). */
    val standard: String = "Standard",
)

/**
 * "Another provider": servers grouped by who runs them, each row showing its protocol as a pill.
 * Picking a row is the host's (the same selection calls as the per-protocol lists). [groups] must
 * keep their order while the screen is open (see [rememberStableOrder]) so a picked row never
 * jumps. "Add server" asks for the protocol; "Servers by protocol" keeps the per-protocol lists
 * (and DNSCrypt relays) one tap away.
 */
@Composable
fun <K : Any> RethinkDnsListScreen(
    groups: List<RethinkDnsProviderGroup<K>>,
    protocols: List<RethinkDnsProtocol>,
    strings: RethinkDnsListStrings,
    onProtocolClick: (RethinkDnsProtocol) -> Unit,
    onAddProtocol: (RethinkDnsProtocol) -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    itemContent: @Composable (K) -> Unit,
) {
    var showAddSheet by remember { mutableStateOf(false) }
    RethinkTopBarLazyColumnScreen(
        title = strings.title,
        subtitle = strings.subtitle,
        onBackClick = onBackClick,
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                elevation = FloatingActionButtonDefaults.loweredElevation(),
            ) {
                Icon(MaterialSymbols.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(SharedDimensions.spacingSm))
                Text(strings.add)
            }
        },
    ) {
        if (loading) {
            item(key = "loading") { RethinkLoadingState(strings.loading, Modifier.fillParentMaxHeight(CENTERED_STATE_HEIGHT)) }
        } else {
            groups.forEach { group ->
                rethinkSection(group.title, key = "group:${group.title}") {
                    // One grouped list per provider; each row names only its variant.
                    Column {
                        group.keys.forEachIndexed { index, key ->
                            val context = DnsRowContext(cardPositionFor(index, group.keys.lastIndex), group.title, strings.standard)
                            CompositionLocalProvider(LocalDnsRowContext provides context) { itemContent(key) }
                        }
                    }
                }
            }
        }
        rethinkSection(strings.byProtocol, strings.byProtocolDescription, key = "protocols") {
            RethinkListGroup {
                protocols.forEachIndexed { index, protocol ->
                    RethinkListItem(
                        headline = protocol.label,
                        supporting = protocol.description,
                        leadingIcon = protocol.icon,
                        position = cardPositionFor(index, protocols.lastIndex),
                        showTrailingChevron = true,
                        onClick = { onProtocolClick(protocol) },
                    )
                }
            }
        }
        // Clear the extended FAB so no row is ever hidden under it.
        item(key = "fab-clearance") { Spacer(Modifier.height(SharedDimensions.fabClearance)) }
    }
    if (showAddSheet) {
        RethinkModalBottomSheet(onDismissRequest = { showAddSheet = false }) { _ ->
            RethinkSheetHeader(title = strings.addTitle, subtitle = strings.addDescription)
            RethinkListGroup {
                protocols.forEachIndexed { index, protocol ->
                    RethinkListItem(
                        headline = protocol.label,
                        supporting = protocol.description,
                        leadingIcon = protocol.icon,
                        position = cardPositionFor(index, protocols.lastIndex),
                        onClick = {
                            showAddSheet = false
                            onAddProtocol(protocol)
                        },
                    )
                }
            }
        }
    }
}

/** The share of the list a lone state centers in, leaving the sections below it in view. */
private const val CENTERED_STATE_HEIGHT = 0.7f
