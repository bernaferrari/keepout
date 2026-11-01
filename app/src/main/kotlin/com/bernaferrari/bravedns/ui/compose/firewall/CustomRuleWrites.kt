/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.firewall

import Logger
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.CustomDomain
import com.bernaferrari.bravedns.database.EventSource
import com.bernaferrari.bravedns.database.EventType
import com.bernaferrari.bravedns.database.Severity
import com.bernaferrari.bravedns.service.DomainRulesManager
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.IpRulesManager
import com.bernaferrari.bravedns.util.Constants.Companion.UNSPECIFIED_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A tapped rule's Block · Trust · Delete, written through the managers by [applyRuleEdit], then an
 * Undo snackbar that restores the exact previous rule. A newer edit retires an older Undo.
 */
@Composable
internal fun rememberRuleEditAction(
    snackbar: SnackbarHostState,
    eventLogger: EventLogger,
): (RethinkFirewallRule, RethinkRuleEdit) -> Unit {
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.lbl_undo)
    val failed = stringResource(R.string.rules_edit_failed)
    val blocked = stringResource(R.string.rules_edit_blocked)
    val trusted = stringResource(R.string.rules_edit_trusted)
    val deleted = stringResource(R.string.rules_edit_deleted)
    var epoch by remember { mutableIntStateOf(0) }
    return { rule, edit ->
        val next = ++epoch
        snackbar.currentSnackbarData?.dismiss()
        val message = when (edit) {
            RethinkRuleEdit.Delete -> deleted
            is RethinkRuleEdit.SetAction -> if (edit.action == RethinkRuleAction.Trust) trusted else blocked
        }.format(rule.value)
        val log = "${rule.tab.name} ${rule.value}, ${rule.uid}: $edit"
        var undo: (suspend () -> Unit)? = null
        scope.launchReversibleRuleMutation(
            snackbarHostState = snackbar,
            editedMessage = message,
            undoLabel = undoLabel,
            epoch = next,
            currentEpoch = { epoch },
            setApplying = {},
            apply = {
                val result = runCatching {
                    withContext(Dispatchers.IO + NonCancellable) { applyRuleEdit(ManagerRuleEditStore, rule, edit) }
                }
                result.exceptionOrNull()?.let { error ->
                    Logger.e(Logger.LOG_TAG_UI, "rules: edit failed, $log", error as? Exception)
                    scope.launch { snackbar.showSnackbar(failed) }
                }
                undo = result.getOrNull()
                if (undo != null) logFirewallRuleChange(eventLogger, "Changed rule", log)
                undo != null
            },
            undo = {
                runCatching { withContext(Dispatchers.IO + NonCancellable) { undo?.invoke() } }
                    .onFailure { Logger.e(Logger.LOG_TAG_UI, "rules: undo failed, $log", it as? Exception) }
                logFirewallRuleChange(eventLogger, "Changed rule", "undo: $log")
            },
        )
    }
}

/** Deletes a rule from the Rules list through its manager (DB row and trie together), and logs it. */
internal suspend fun deleteRule(tab: RethinkRulesTab, rule: RethinkFirewallRule, eventLogger: EventLogger) {
    when (tab) {
        RethinkRulesTab.IP -> IpRulesManager.removeIpRule(rule.uid, rule.value, rule.port ?: UNSPECIFIED_PORT)
        RethinkRulesTab.DOMAIN -> DomainRulesManager.deleteDomain(
            CustomDomain().apply {
                domain = rule.value
                uid = rule.uid
            },
        )
    }
    val isIp = tab == RethinkRulesTab.IP
    eventLogger.log(
        EventType.FW_RULE_MODIFIED,
        Severity.LOW,
        if (isIp) "Removed IP rule" else "Removed domain rule",
        EventSource.UI,
        false,
        if (isIp) "IP: ${rule.value}" else "Domain: ${rule.value}",
    )
}
