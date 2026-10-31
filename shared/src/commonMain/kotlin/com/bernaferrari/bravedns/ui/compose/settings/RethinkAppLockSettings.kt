/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.settings

import com.bernaferrari.bravedns.ui.compose.theme.RethinkChoiceOption
import com.bernaferrari.bravedns.ui.compose.theme.RethinkConnectedChoiceGroup
import com.bernaferrari.bravedns.ui.compose.theme.RethinkFormSection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bernaferrari.bravedns.ui.compose.theme.RethinkContainedCard

/** When Keepout asks for the screen lock. Mirrors the app's stored `BioMetricType`. */
enum class RethinkAppLockTimeout { Off, EveryTime, FiveMinutes, FifteenMinutes }

data class RethinkAppLockOption(
    val timeout: RethinkAppLockTimeout,
    /** Short: one segment of a four-way group ("Off", "Always", "5 min", "15 min"). */
    val label: String,
)

data class RethinkAppLockSettingsStrings(
    /** What the lock does and what each choice means. */
    val explanation: String,
    /** Section header: "App lock". */
    val section: String,
    /** Shown above the choices when the device has no screen lock to ask for. */
    val unavailable: String,
)

/**
 * App lock as a section of Settings › General: one four-way group, Off · Always · 5 min · 15 min,
 * under the explanation. The host checks the screen lock before saving anything other than Off
 * ([onSelect] may refuse), so a mis-set lock can't shut the user out.
 */
@Composable
fun RethinkAppLockSection(
    selected: RethinkAppLockTimeout,
    options: List<RethinkAppLockOption>,
    strings: RethinkAppLockSettingsStrings,
    available: Boolean,
    onSelect: (RethinkAppLockTimeout) -> Unit,
    modifier: Modifier = Modifier,
) {
    RethinkFormSection(strings.section, description = strings.explanation, modifier = modifier) {
        if (!available) {
            RethinkContainedCard(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) { Text(strings.unavailable, style = MaterialTheme.typography.bodyMedium) }
        }
        RethinkConnectedChoiceGroup(
            options = options.map { RethinkChoiceOption(it.timeout, it.label) },
            selected = selected,
            onSelect = onSelect,
        )
    }
}
