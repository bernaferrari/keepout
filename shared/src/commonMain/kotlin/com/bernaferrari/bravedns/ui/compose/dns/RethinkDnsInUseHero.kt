/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.dns

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.bravedns.ui.compose.theme.CardPosition
import com.bernaferrari.bravedns.ui.compose.theme.LocalRethinkMotion
import com.bernaferrari.bravedns.ui.compose.theme.RethinkContainedCard
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListGroup
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListItem
import com.bernaferrari.bravedns.ui.compose.theme.RethinkPill
import com.bernaferrari.bravedns.ui.compose.theme.RethinkPillDefaults
import com.bernaferrari.bravedns.ui.compose.theme.RethinkPillSize
import com.bernaferrari.bravedns.ui.compose.theme.RethinkStatusColors
import com.bernaferrari.bravedns.ui.compose.theme.RethinkStatusIndicator
import com.bernaferrari.bravedns.ui.compose.theme.RethinkStatusLabel
import com.bernaferrari.bravedns.ui.compose.theme.RethinkSwitchListItem
import com.bernaferrari.bravedns.ui.compose.theme.RethinkTopBarLazyColumnScreen
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.ui.compose.theme.rethinkSection
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols
import com.bernaferrari.bravedns.ui.icons.keepout.KeepoutIcons

/** DNS's In use card: the resolver, its protocol and address, and recent lookup speed. */
@Composable
internal fun DnsInUseHero(
    state: RethinkDnsSettingsState,
    strings: RethinkDnsSettingsStrings,
    onDetails: (() -> Unit)?,
    onTryAnother: () -> Unit,
) {
    val endpoint = connectedDnsEndpoint(state.connectedDnsName)
    val storedName = connectedDnsDisplayName(state.connectedDnsName).ifBlank { "--" }
    val choice = state.dnsChoice()
    // Rethink DNS is named as such; its blocklist ("Default", "Family") is the pill beside it.
    val displayName = if (choice == RethinkDnsChoice.Rethink) strings.rethinkDns else storedName
    val protocol = state.dnsType.protocol(endpoint)
    val shape = RoundedCornerShape(SharedDimensions.groupCornerRadius)
    val failing = state.isDnsFailing
    val (heroContainer, onHero) = dnsHeroColors(failing)
    Surface(
        shape = shape,
        color = heroContainer,
        contentColor = onHero,
        modifier = Modifier.fillMaxWidth().clip(shape).then(
            if (onDetails != null) Modifier.clickable(onClickLabel = strings.aboutItem(displayName), onClick = onDetails) else Modifier,
        ),
    ) {
        Column(Modifier.padding(SharedDimensions.spacingXl), verticalArrangement = Arrangement.spacedBy(SharedDimensions.spacingMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.inUse, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (onDetails != null) Icon(MaterialSymbols.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
            Text(
                displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DnsHeroFacts(protocol, endpoint, displayName, onHero)
            if (failing) DnsHeroFailing(strings, onTryAnother) else DnsHeroLatency(state, strings, onHero)
        }
    }
}

/**
 * The brand palette keeps primaryContainer at the seed's bright tone in dark theme too; the
 * secondary container is the same hue at a dark tone, so the hero dims with the rest of the UI.
 */
@Composable
private fun dnsHeroColors(failing: Boolean): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    return when {
        failing -> scheme.errorContainer to scheme.onErrorContainer
        dark -> scheme.secondaryContainer.copy(alpha = .78f) to scheme.onSecondaryContainer
        else -> scheme.primaryContainer.copy(alpha = .78f) to scheme.onPrimaryContainer
    }
}

/**
 * The protocol only when it says something (a bare "DNS", or one that repeats the name, doesn't),
 * then the address. The blocklist is named once, on the selected Rethink row below.
 */
@Composable
private fun DnsHeroFacts(protocol: String, endpoint: String, displayName: String, onHero: Color) {
    val showProtocol = protocol != GENERIC_PROTOCOL && !protocol.equals(displayName, ignoreCase = true)
    val showEndpoint = endpoint.isNotBlank()
    if (!showProtocol && !showEndpoint) return
    Row(horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSm), verticalAlignment = Alignment.CenterVertically) {
        if (showProtocol) RethinkPill(protocol, colors = RethinkPillDefaults.onContainer(onHero), size = RethinkPillSize.Large)
        if (showEndpoint) {
            RethinkPill(endpoint, Modifier.weight(1f, fill = false), colors = RethinkPillDefaults.onContainer(onHero), size = RethinkPillSize.Large)
        }
    }
}

@Composable
private fun DnsHeroFailing(strings: RethinkDnsSettingsStrings, onTryAnother: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingSm)) {
        Icon(MaterialSymbols.Filled.Warning, null, modifier = Modifier.size(SharedDimensions.iconSizeSm))
        Text(strings.heroFailing, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
    FilledTonalButton(onClick = onTryAnother) { Text(strings.tryAnotherServer) }
}

@Composable
private fun DnsHeroLatency(state: RethinkDnsSettingsState, strings: RethinkDnsSettingsStrings, onHero: Color) {
    val latency = dnsHeroLatency(state.recentLatencies, state.dnsLatency, LATENCY_BARS)
    val latencies = latency.samples
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(SharedDimensions.spacingLg)) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            Text(latency.label ?: "—", style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold)
            Text(if (latencies.isEmpty()) strings.noLookupsYet else strings.medianLookup, style = MaterialTheme.typography.bodySmall)
        }
        if (latencies.isNotEmpty()) LatencyBars(latencies.reversed(), onHero, Modifier.weight(1f).height(LatencyChartHeight))
    }
}

/**
 * One rounded bar per recent lookup, oldest to newest, in the hero's own on-colour: height says
 * the speed, and green/red bars on the coloured container clashed with it.
 */
@Composable
private fun LatencyBars(latencies: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val max = (latencies.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        latencies.forEach { ms ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight((ms / max).coerceIn(0.08f, 1f))
                    .background(color.copy(alpha = LATENCY_BAR_ALPHA), RoundedCornerShape(SharedDimensions.cornerRadiusFull)),
            )
        }
    }
}
