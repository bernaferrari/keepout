/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.firewall

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.ConnectionTrackerRepository
import com.bernaferrari.bravedns.database.CustomDomainDAO
import com.bernaferrari.bravedns.database.CustomIpDao
import com.bernaferrari.bravedns.database.EventSource
import com.bernaferrari.bravedns.database.EventType
import com.bernaferrari.bravedns.database.Severity
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.util.BackgroundAccessibilityService
import com.bernaferrari.bravedns.util.Utilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Android adapter for [RethinkFirewallSettingsScreen]: the universal rules read and write the same
 * [PersistentState] flags as before (the firewall reads them unchanged), the Rules entry counts every
 * IP and domain rule, and each row's blocked count comes from the connection log.
 */
@Composable
fun FirewallSettingsScreen(
    persistentState: PersistentState,
    eventLogger: EventLogger,
    connTrackerRepository: ConnectionTrackerRepository,
    onRulesClick: () -> Unit,
    onNavigateToLogs: (String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    initialFocusKey: String? = null,
    onBackClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val switches = remember { UniversalFirewallSwitchStates(persistentState) }
    val rulesCount = rememberEveryRuleCount()
    // Null until the connection log has been counted.
    val blockedCounts by produceState<Map<String, Int>?>(null) {
        val rows = withContext(Dispatchers.IO) { connTrackerRepository.getBlockedUniversalRulesCount() }
        value = rows.associate { it.blockedByRule to it.count }
    }

    fun change(id: String, enabled: Boolean): RethinkUniversalFirewallChange {
        val switch = UniversalFirewallSwitches.byId(id) ?: return RethinkUniversalFirewallChange.Applied
        val log = { detail: String ->
            eventLogger.log(EventType.FW_RULE_MODIFIED, Severity.LOW, "Univ firewall setting", EventSource.UI, false, detail)
        }
        if (switch == UniversalFirewallSwitches.background && enabled && !isBackgroundServiceEnabled(context)) {
            switches.set(switch, false)
            log("Background mode requires accessibility")
            return RethinkUniversalFirewallChange.RequiresAccessibility
        }
        switches.set(switch, enabled)
        log("${switch.logName}: $enabled")
        return RethinkUniversalFirewallChange.Applied
    }

    val counts = blockedCounts.orEmpty()
    RethinkFirewallSettingsScreen(
        strings = firewallSettingsStrings(),
        everyday = UniversalFirewallSwitches.everyday.map { it.setting(switches, counts) },
        strict = UniversalFirewallSwitches.strict.map { it.setting(switches, counts) },
        lockdown = UniversalFirewallSwitches.lockdown.setting(switches, counts),
        rulesCount = rulesCount,
        isLoadingStats = blockedCounts == null,
        onSettingChange = ::change,
        onRulesClick = onRulesClick,
        onLogsClick = onNavigateToLogs,
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        initialFocusKey = initialFocusKey,
        onBackClick = onBackClick,
    )
}

/** Every IP and domain rule, for all apps and each app; null until counted. */
@Composable
private fun rememberEveryRuleCount(): Int? {
    val ipDao = koinInject<CustomIpDao>()
    val domainDao = koinInject<CustomDomainDAO>()
    val count by remember(ipDao, domainDao) {
        combine(ipDao.observeEveryIpRuleCount(), domainDao.observeEveryDomainRuleCount()) { ips, domains -> ips + domains }
    }.collectAsState(initial = null)
    return count
}

@Composable
private fun UniversalFirewallSwitch.setting(states: UniversalFirewallSwitchStates, blockedCounts: Map<String, Int>) =
    RethinkUniversalFirewallSetting(
        rule.id,
        stringResource(label),
        states.isChecked(this),
        icon,
        blockedCounts[rule.id] ?: 0,
        stringResource(description),
    )

/** Background blocking sees the foreground app only through the accessibility service. */
private fun isBackgroundServiceEnabled(context: Context): Boolean =
    Utilities.isAccessibilityServiceEnabled(context, BackgroundAccessibilityService::class.java) &&
        Utilities.isAccessibilityServiceEnabledViaSettingsSecure(context, BackgroundAccessibilityService::class.java)

@Composable
private fun firewallSettingsStrings(): RethinkFirewallSettingsStrings {
    val rulesTitle = stringResource(R.string.lbl_rules)
    val rulesFormat = stringResource(R.string.firewall_rules_count)
    return RethinkFirewallSettingsStrings(
        title = stringResource(R.string.lbl_firewall),
        explanation = stringResource(R.string.universal_firewall_explanation),
        rulesTitle = rulesTitle,
        rulesDescription = stringResource(R.string.univ_view_blocked_ip_desc),
        rulesWithCount = { count -> if (count == 0) rulesTitle else rulesFormat.format(count) },
        everydaySection = stringResource(R.string.firewall_section_everyday),
        strictSection = stringResource(R.string.firewall_section_strict),
        lockdownSection = stringResource(R.string.firewall_section_lockdown),
        blocked = { count -> stringResource(R.string.two_argument_colon, stringResource(R.string.lbl_blocked), count.toString()) },
        lockdownConfirmTitle = stringResource(R.string.firewall_lockdown_confirm_title),
        lockdownConfirmMessage = stringResource(R.string.firewall_lockdown_confirm_message),
        lockdownConfirm = stringResource(R.string.firewall_lockdown_confirm),
        cancel = stringResource(R.string.lbl_cancel),
        accessibilityTitle = stringResource(R.string.alert_permission_accessibility),
        accessibilityDescription = stringResource(R.string.alert_firewall_accessibility_explanation),
        accessibilityConfirm = stringResource(R.string.univ_accessibility_dialog_positive),
        accessibilityDismiss = stringResource(R.string.univ_accessibility_dialog_negative),
    )
}
