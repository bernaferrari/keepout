/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.service.DomainRulesManager
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.FirewallManager
import com.bernaferrari.bravedns.service.IpRulesManager
import com.bernaferrari.bravedns.ui.compose.common.AppIcon
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRuleAction
import com.bernaferrari.bravedns.ui.compose.firewall.domainStatusFor
import com.bernaferrari.bravedns.ui.compose.firewall.ipStatusFor
import com.bernaferrari.bravedns.util.UIUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android adapter for [RethinkConnectionSheet]: reads the app's flags and the domain/IP rules,
 * maps [BlockReason] fixes to buttons, and performs every write through [FirewallManager],
 * [DomainRulesManager] and [IpRulesManager] with an Undo snackbar.
 *
 * Fixes change only the rule that decided [request] ([BlockReason]); the rule group edits the
 * *exact* rule for the target and scope, preserving an existing rule's proxy settings, and an
 * Undo of a rule the sheet created deletes it again rather than leaving a "No rule" row.
 * [onRulesChanged] runs after every write and undo, for hosts that cache rule state.
 *
 * The pieces: [ConnectionSheetTarget] (what can be acted on), [loadConnectionSheetState] (the
 * rules as they stand), [ConnectionSheetFixes] (the buttons) and [ConnectionSheetWriter] (writes
 * with Undo).
 */
@Composable
fun ConnectionSheetHost(
    request: ConnectionSheetRequest,
    eventLogger: EventLogger,
    onDismiss: () -> Unit,
    navigation: ConnectionSheetNavigation = ConnectionSheetNavigation(),
    onRulesChanged: () -> Unit = {},
) {
    val target = remember(request) { ConnectionSheetTarget.of(request) }
    var ruleScope by remember { mutableStateOf(target.defaultScope) }
    val writer = rememberConnectionSheetWriter(eventLogger, onRulesChanged)
    val state by produceState<ConnectionSheetState?>(null, request, ruleScope, writer.revision) {
        value = withContext(Dispatchers.IO) { loadConnectionSheetState(request, target, ruleScope) }
    }

    // The rule group is part of the sheet's height: open once it's read (a few ms), so the
    // sheet appears at its final size instead of growing under the user's finger.
    if (target.kind != null && state == null) return

    val appName = request.appName.ifBlank { stringResource(R.string.network_log_app_name_unknown) }
    val title = target.domain ?: request.domain ?: request.ipAddress ?: appName
    val links = ConnectionSheetLinks(navigation, onDismiss)
    val fixes = ConnectionSheetFixes(request, target, state, writer, links, appName)
    val reason = target.reason
    val primary = fixes.actionFor(reason.primaryFix)
    // Allowing the app outright makes temporary access moot.
    val secondary = fixes.actionFor(reason.secondaryFix).takeUnless { reason.primaryFix in writer.applied }
    val savedMessage = stringResource(R.string.connection_done_rule_saved)

    RethinkConnectionSheet(
        model = RethinkConnectionSheetModel(
            title = title,
            subtitle = listOfNotNull(appName, request.timeLabel, request.protocolLabel).filter { it.isNotBlank() }.joinToString(" · "),
            isBlocked = request.isBlocked,
            verdict = stringResource(reason.sentence),
            verdictCaption = blocklistsCaption(reason.blocklists),
            primaryAction = primary,
            secondaryAction = secondary,
            rule = ruleGroup(request, target, state, ruleScope, enabled = !writer.busy),
            details = request.details,
        ),
        strings = connectionSheetStrings(appName),
        onDismiss = onDismiss,
        onRuleActionChange = { action -> writer.setGroupRule(request, target, target.ruleUid(ruleScope), action, savedMessage) },
        onRuleScopeChange = { if (target.allows(it)) ruleScope = it },
        appIcon = { ConnectionAppIcon(request, target.appUid) },
        onCopyAddress = rememberCopyAddress(title, writer.snackbar),
        onOpenApp = links.openApp(target.appUid),
        snackbarHostState = writer.snackbar,
    )
}

/** The rule group for the target in [scope]; null hides it (no target, or rules not read yet). */
@Composable
private fun ruleGroup(
    request: ConnectionSheetRequest,
    target: ConnectionSheetTarget,
    state: ConnectionSheetState?,
    scope: RethinkConnectionRuleScope,
    enabled: Boolean,
): RethinkConnectionRule? {
    val kind = target.kind ?: return null
    val current = state ?: return null
    return RethinkConnectionRule(
        kind = kind,
        value = if (kind == RethinkConnectionTargetKind.Domain) target.domain.orEmpty() else request.ipAddress.orEmpty(),
        action = current.groupAction,
        scope = scope,
        note = if (current.groupInherited) stringResource(R.string.connection_wider_rule) else null,
        enabled = enabled,
    )
}

/**
 * The rule group's Block · Trust · No rule: writes the exact rule for [uid] on the domain when
 * there is an exact one, else on the IP.
 */
private fun ConnectionSheetWriter.setGroupRule(
    request: ConnectionSheetRequest,
    target: ConnectionSheetTarget,
    uid: Int,
    action: RethinkRuleAction,
    message: String,
) {
    val domain = target.domain
    val address = target.ip
    when {
        domain != null -> mutate(message, fix = null, log = "domain rule $domain, $uid -> $action") {
            writeDomainRule(domain, uid, request.ipAddress.orEmpty(), domainStatusFor(action))
        }
        address != null -> mutate(message, fix = null, log = "ip rule $address, $uid -> $action") {
            writeIpRule(uid, address, ipStatusFor(action, uid))
        }
    }
}

@Composable
private fun blocklistsCaption(blocklists: List<String>): String? =
    if (blocklists.isEmpty()) null else stringResource(R.string.connection_blocked_by_lists, blocklists.joinToString(", "))

@Composable
private fun ConnectionAppIcon(request: ConnectionSheetRequest, appUid: Int?) {
    val packageName = request.packageName
    when {
        packageName != null -> AppIcon(packageName, appName = request.appName)
        appUid != null -> AppIcon(appUid)
    }
}

/** Copies the sheet's address and confirms it in the sheet's own snackbar. */
@Composable
private fun rememberCopyAddress(address: String, snackbar: SnackbarHostState): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.connection_copy_address)
    val copied = stringResource(R.string.copied_clipboard)
    return {
        UIUtils.clipboardCopy(context, address, label)
        scope.launch { snackbar.showSnackbar(copied) }
    }
}

@Composable
private fun connectionSheetStrings(appName: String) = RethinkConnectionSheetStrings(
    thisDomain = stringResource(R.string.connection_this_domain),
    thisIp = stringResource(R.string.connection_this_ip),
    block = stringResource(R.string.ci_block),
    trust = stringResource(R.string.ci_trust_rule),
    noRule = stringResource(R.string.ci_no_rule),
    onlyApp = stringResource(R.string.connection_only_app, appName),
    allApps = stringResource(R.string.lbl_all_apps),
    details = stringResource(R.string.log_pane_details),
    copyAddress = stringResource(R.string.connection_copy_address),
    openApp = stringResource(R.string.connection_open_app, appName),
)
