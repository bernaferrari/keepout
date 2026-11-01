/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.firewall

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.service.FirewallRuleset
import com.bernaferrari.bravedns.service.PersistentState

/**
 * One universal rule's switch: the [PersistentState] flag it reads and writes (the firewall reads
 * the same flag), its copy and glyph, and [logName], how the event log names a change.
 */
internal class UniversalFirewallSwitch(
    val rule: FirewallRuleset,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val icon: RethinkUniversalFirewallIcon,
    val logName: String,
    val read: (PersistentState) -> Boolean,
    val write: (PersistentState, Boolean) -> Unit,
)

/** The universal rules by section, in screen order. */
internal object UniversalFirewallSwitches {
    val everyday = listOf(
        UniversalFirewallSwitch(
            FirewallRuleset.RULE1B, R.string.univ_rule_new_apps, R.string.univ_rule_new_apps_desc,
            RethinkUniversalFirewallIcon.NewApp, "New apps",
            PersistentState::getBlockNewlyInstalledApp, PersistentState::setBlockNewlyInstalledApp,
        ),
        UniversalFirewallSwitch(
            FirewallRuleset.RULE3, R.string.univ_rule_screen_locked, R.string.univ_rule_screen_locked_desc,
            RethinkUniversalFirewallIcon.DeviceLock, "Device lock",
            PersistentState::getBlockWhenDeviceLocked, PersistentState::setBlockWhenDeviceLocked,
        ),
        // Needs the accessibility service to see which app is in use (see [background]).
        UniversalFirewallSwitch(
            FirewallRuleset.RULE4, R.string.univ_firewall_rule_2, R.string.univ_rule_not_in_use_desc,
            RethinkUniversalFirewallIcon.Background, "Background mode",
            PersistentState::getBlockAppWhenBackground, PersistentState::setBlockAppWhenBackground,
        ),
    )

    val strict = listOf(
        UniversalFirewallSwitch(
            FirewallRuleset.RULE6, R.string.univ_firewall_rule_4, R.string.univ_rule_udp_desc,
            RethinkUniversalFirewallIcon.Udp, "UDP",
            PersistentState::getUdpBlocked, PersistentState::setUdpBlocked,
        ),
        UniversalFirewallSwitch(
            FirewallRuleset.RULE10, R.string.univ_rule_http, R.string.univ_rule_http_desc,
            RethinkUniversalFirewallIcon.Http, "HTTP",
            PersistentState::getBlockHttpConnections, PersistentState::setBlockHttpConnections,
        ),
        UniversalFirewallSwitch(
            FirewallRuleset.RULE5, R.string.univ_rule_unknown, R.string.univ_rule_unknown_desc,
            RethinkUniversalFirewallIcon.Unknown, "Unknown connections",
            PersistentState::getBlockUnknownConnections, PersistentState::setBlockUnknownConnections,
        ),
        UniversalFirewallSwitch(
            FirewallRuleset.RULE7, R.string.univ_firewall_rule_5, R.string.univ_rule_dns_bypass_desc,
            RethinkUniversalFirewallIcon.Dns, "DNS bypass",
            PersistentState::getDisallowDnsBypass, PersistentState::setDisallowDnsBypass,
        ),
        UniversalFirewallSwitch(
            FirewallRuleset.RULE1F, R.string.univ_firewall_rule_9, R.string.univ_rule_mobile_data_desc,
            RethinkUniversalFirewallIcon.Metered, "Metered connections",
            PersistentState::getBlockMeteredConnections, PersistentState::setBlockMeteredConnections,
        ),
    )

    val lockdown = UniversalFirewallSwitch(
        FirewallRuleset.RULE11, R.string.univ_rule_lockdown, R.string.univ_rule_lockdown_desc,
        RethinkUniversalFirewallIcon.Lockdown, "Lockdown",
        PersistentState::getUniversalLockdown, PersistentState::setUniversalLockdown,
    )

    /** Blocking apps in the background, which only takes effect with the accessibility service on. */
    val background: UniversalFirewallSwitch = everyday.first { it.rule == FirewallRuleset.RULE4 }

    val all: List<UniversalFirewallSwitch> = everyday + strict + lockdown

    private val byId: Map<String, UniversalFirewallSwitch> = all.associateBy { it.rule.id }

    fun byId(id: String): UniversalFirewallSwitch? = byId[id]
}

/**
 * The switches' on/off state, read once from [prefs] and written through to it, so the screen
 * redraws a change without re-reading preferences.
 */
@Stable
internal class UniversalFirewallSwitchStates(private val prefs: PersistentState) {
    private val checked = mutableStateMapOf<String, Boolean>().apply {
        UniversalFirewallSwitches.all.forEach { put(it.rule.id, it.read(prefs)) }
    }

    fun isChecked(switch: UniversalFirewallSwitch): Boolean = checked[switch.rule.id] == true

    fun set(switch: UniversalFirewallSwitch, enabled: Boolean) {
        checked[switch.rule.id] = enabled
        switch.write(prefs, enabled)
    }
}
