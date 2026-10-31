/* Copyright 2026 RethinkDNS and its authors */
@file:OptIn(ExperimentalMaterial3Api::class)

package com.bernaferrari.bravedns.ui.compose.settings

import com.bernaferrari.bravedns.ui.compose.theme.RethinkFormSection
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.bernaferrari.bravedns.ui.compose.theme.CardPosition
import com.bernaferrari.bravedns.ui.compose.theme.RethinkListGroup
import com.bernaferrari.bravedns.ui.compose.theme.RethinkSwitchListItem

data class RethinkAdvancedSettingsStrings(
    val title: String,
    val subtitle: String,
    val experimentalTitle: String,
    val autoDialTitle: String,
    val autoDialDescription: String,
    val panicTitle: String,
    val panicDescription: String,
)

/** The debug-only engine toggles, as a section of Logs & diagnostics. Hosts keep persistence and gating. */
@Composable
fun RethinkDeveloperToggles(
    /** Developer tools (stats, database, flight recorder), listed above the toggles. */
    tools: List<RethinkSettingsLink>,
    strings: RethinkAdvancedSettingsStrings,
    experimentalEnabled: Boolean,
    autoDialEnabled: Boolean,
    panicEnabled: Boolean,
    onExperimentalChange: (Boolean) -> Unit,
    onAutoDialChange: (Boolean) -> Unit,
    onPanicChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    RethinkFormSection(strings.title, description = strings.subtitle, modifier = modifier) {
        RethinkSettingsLinkRows(tools)
        RethinkListGroup {
            AdvancedToggleRow(strings.experimentalTitle, null, MaterialSymbols.Filled.Science, experimentalEnabled, CardPosition.First, onExperimentalChange)
            AdvancedToggleRow(strings.autoDialTitle, strings.autoDialDescription, MaterialSymbols.Filled.Tune, autoDialEnabled, CardPosition.Middle, onAutoDialChange)
            AdvancedToggleRow(strings.panicTitle, strings.panicDescription, MaterialSymbols.Filled.Warning, panicEnabled, CardPosition.Last, onPanicChange, warning = true)
        }
    }
}

/** [warning] tints the icon error-red: the toggle itself is not destructive, only risky. */
@Composable
private fun AdvancedToggleRow(
    title: String,
    description: String?,
    icon: ImageVector,
    checked: Boolean,
    position: CardPosition,
    onCheckedChange: (Boolean) -> Unit,
    warning: Boolean = false,
) {
    RethinkSwitchListItem(
        headline = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        supporting = description,
        leadingIcon = icon,
        leadingIconTint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        position = position,
    )
}
