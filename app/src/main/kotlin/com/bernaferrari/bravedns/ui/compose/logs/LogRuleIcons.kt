/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.bernaferrari.bravedns.service.FirewallRuleset
import com.bernaferrari.bravedns.ui.compose.theme.SharedDimensions
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols
import com.bernaferrari.bravedns.ui.icons.keepout.KeepoutIcons

/** A firewall rule's glyph at list-option size, for the Reason filter. */
@Composable
internal fun PlatformLogRuleIcon(ruleId: String, tint: Color) {
    Icon(
        imageVector = firewallRuleIcon(ruleId),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(SharedDimensions.iconSizeSm),
    )
}

/** Why a connection was allowed or blocked: the rule's subject, in the same glyphs the settings use. */
private val firewallRuleIcons: Map<String, () -> ImageVector> by lazy {
    // Keep each vector lazy: initializing the dispatch table must not inflate every glyph.
    mapOf(
        FirewallRuleset.RULE0.id to { MaterialSymbols.Filled.CheckCircle },
        FirewallRuleset.RULE1.id to { MaterialSymbols.Filled.DoNotDisturbOn },
        FirewallRuleset.RULE1C.id to { MaterialSymbols.Filled.DoNotDisturbOn },
        FirewallRuleset.RULE1B.id to { MaterialSymbols.Filled.NewReleases },
        FirewallRuleset.RULE1D.id to { MaterialSymbols.Filled.WifiOff },
        FirewallRuleset.RULE1E.id to { MaterialSymbols.Filled.MobiledataOff },
        FirewallRuleset.RULE1F.id to { MaterialSymbols.Filled.CellTower },
        FirewallRuleset.RULE1G.id to { MaterialSymbols.Filled.Verified },
        FirewallRuleset.RULE2B.id to { MaterialSymbols.Filled.Verified },
        FirewallRuleset.RULE2F.id to { MaterialSymbols.Filled.Verified },
        FirewallRuleset.RULE2I.id to { MaterialSymbols.Filled.Verified },
        FirewallRuleset.RULE1H.id to { KeepoutIcons.FirewallOff },
        FirewallRuleset.RULE2.id to { MaterialSymbols.Filled.Pin },
        FirewallRuleset.RULE2C.id to { MaterialSymbols.Filled.PublicOff },
        FirewallRuleset.RULE8.id to { MaterialSymbols.Filled.PublicOff },
        FirewallRuleset.RULE2D.id to { MaterialSymbols.Filled.Public },
        FirewallRuleset.RULE2H.id to { MaterialSymbols.Filled.Public },
        FirewallRuleset.RULE2E.id to { MaterialSymbols.Filled.Link },
        FirewallRuleset.RULE3.id to { MaterialSymbols.Filled.ScreenLockPortrait },
        FirewallRuleset.RULE4.id to { MaterialSymbols.Filled.VisibilityOff },
        FirewallRuleset.RULE5.id to { MaterialSymbols.AutoMirrored.Filled.HelpOutline },
        FirewallRuleset.RULE6.id to { MaterialSymbols.Filled.SettingsEthernet },
        FirewallRuleset.RULE7.id to { MaterialSymbols.Filled.Shield },
        FirewallRuleset.RULE9B.id to { MaterialSymbols.Filled.ConversionPath },
        FirewallRuleset.RULE12.id to { MaterialSymbols.Filled.ConversionPath },
        FirewallRuleset.RULE15.id to { MaterialSymbols.Filled.ConversionPath },
        FirewallRuleset.RULE10.id to { MaterialSymbols.Filled.Http },
        FirewallRuleset.RULE11.id to { MaterialSymbols.Filled.Lock },
        FirewallRuleset.RULE17.id to { MaterialSymbols.Filled.Lock },
        FirewallRuleset.RULE13.id to { MaterialSymbols.Filled.Warning },
        FirewallRuleset.RULE18.id to { MaterialSymbols.Filled.Warning },
        FirewallRuleset.RULE16.id to { MaterialSymbols.Filled.PauseCircle },
        FirewallRuleset.RULE19.id to { MaterialSymbols.Filled.Timer },
    )
}

private fun firewallRuleIcon(ruleId: String?): ImageVector =
    firewallRuleIcons[ruleId]?.invoke() ?: MaterialSymbols.Filled.Dns
