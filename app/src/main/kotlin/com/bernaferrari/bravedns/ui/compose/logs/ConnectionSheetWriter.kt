/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.logs

import Logger
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.CustomDomain
import com.bernaferrari.bravedns.database.CustomIp
import com.bernaferrari.bravedns.service.DomainRulesManager
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.IpRuleNormalizer
import com.bernaferrari.bravedns.service.IpRulesManager
import com.bernaferrari.bravedns.service.IpRulesManager.IpRuleStatus
import com.bernaferrari.bravedns.ui.compose.firewall.launchReversibleRuleMutation
import com.bernaferrari.bravedns.ui.compose.firewall.logFirewallRuleChange
import inet.ipaddr.IPAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A sheet write: performs the change and returns how to undo it. */
internal typealias SheetWrite = suspend () -> (suspend () -> Unit)

/**
 * The Connection sheet's write side: one write at a time, each followed by an Undo snackbar in
 * [snackbar]. A newer write retires an older snackbar's Undo. [applied] holds the fixes already
 * made (their buttons hide), and [revision] bumps after every write and undo so the host re-reads.
 */
@Stable
internal class ConnectionSheetWriter(
    private val scope: CoroutineScope,
    private val eventLogger: EventLogger,
    private val copy: State<WriterCopy>,
    private val onRulesChanged: State<() -> Unit>,
) {
    val snackbar = SnackbarHostState()

    /** A write or undo is in flight; the rule group disables itself and further writes are ignored. */
    var busy by mutableStateOf(false)
        private set
    var applied by mutableStateOf(emptySet<QuickFix>())
        private set
    var revision by mutableIntStateOf(0)
        private set
    private var epoch by mutableIntStateOf(0)

    /** Write, refresh, then offer Undo. [fix] (when the write is one) hides its button once made. */
    fun mutate(message: String, fix: QuickFix?, log: String, write: SheetWrite) {
        if (busy) return
        val next = ++epoch
        snackbar.currentSnackbarData?.dismiss()
        var undo: (suspend () -> Unit)? = null
        scope.launchReversibleRuleMutation(
            snackbarHostState = snackbar,
            editedMessage = message,
            undoLabel = copy.value.undo,
            epoch = next,
            currentEpoch = { epoch },
            setApplying = { busy = it },
            apply = {
                undo = apply(fix, log, write)
                undo != null
            },
            undo = { revert(fix, log, undo) },
        )
    }

    private suspend fun apply(fix: QuickFix?, log: String, write: SheetWrite): (suspend () -> Unit)? {
        val result = runCatching { serialised { write() } }
        result.exceptionOrNull()?.let { error ->
            Logger.e(Logger.LOG_TAG_UI, "connection sheet: write failed, $log", error as? Exception)
            scope.launch { snackbar.showSnackbar(copy.value.failed) }
        }
        val undo = result.getOrNull() ?: return null
        logFirewallRuleChange(eventLogger, LOG_TITLE, log)
        fix?.let { applied = applied + it }
        changed()
        return undo
    }

    private suspend fun revert(fix: QuickFix?, log: String, undo: (suspend () -> Unit)?) {
        runCatching { serialised { undo?.invoke() } }
            .onFailure { Logger.e(Logger.LOG_TAG_UI, "connection sheet: undo failed, $log", it as? Exception) }
        logFirewallRuleChange(eventLogger, LOG_TITLE, "undo: $log")
        fix?.let { applied = applied - it }
        changed()
    }

    private fun changed() {
        revision++
        onRulesChanged.value()
    }

    private suspend fun <T> serialised(block: suspend () -> T): T =
        withContext(Dispatchers.IO + NonCancellable) { sheetWrites.withLock { block() } }

    private companion object {
        const val LOG_TITLE = "Connection sheet"
    }
}

/** The writer's own copy, resolved in composition. */
internal data class WriterCopy(val undo: String, val failed: String)

@Composable
internal fun rememberConnectionSheetWriter(eventLogger: EventLogger, onRulesChanged: () -> Unit): ConnectionSheetWriter {
    val scope = rememberCoroutineScope()
    val copy = rememberUpdatedState(
        WriterCopy(undo = stringResource(R.string.lbl_undo), failed = stringResource(R.string.connection_save_failed)),
    )
    val latestOnRulesChanged = rememberUpdatedState(onRulesChanged)
    return remember(scope, eventLogger) { ConnectionSheetWriter(scope, eventLogger, copy, latestOnRulesChanged) }
}

/** Serialises sheet writes: the managers' DB rows and native tries must change together. */
private val sheetWrites = Mutex()

/**
 * Set the exact domain rule for [uid] to [status] and return its undo. An existing rule changes in
 * place (keeping its proxy settings); a rule created here is deleted again on undo.
 */
internal suspend fun writeDomainRule(domain: String, uid: Int, ips: String, status: DomainRulesManager.Status): suspend () -> Unit {
    val existing = DomainRulesManager.getObj(uid, domain)
    if (existing == null) {
        DomainRulesManager.changeStatus(domain, uid, ips, DomainRulesManager.DomainType.DOMAIN, status)
        return { DomainRulesManager.getObj(uid, domain)?.let { DomainRulesManager.deleteDomain(it) } }
    }
    val previous = DomainRulesManager.Status.getStatus(existing.status)
    existing.applyStatus(status)
    return { DomainRulesManager.getObj(uid, domain)?.applyStatus(previous) }
}

private suspend fun CustomDomain.applyStatus(status: DomainRulesManager.Status) = when (status) {
    DomainRulesManager.Status.TRUST -> DomainRulesManager.trust(this)
    DomainRulesManager.Status.BLOCK -> DomainRulesManager.block(this)
    DomainRulesManager.Status.NONE -> DomainRulesManager.noRule(this)
}

/** As [writeDomainRule], for the IP rule at [port] (0: any port). */
internal suspend fun writeIpRule(uid: Int, address: IPAddress, status: IpRuleStatus, port: Int = 0): suspend () -> Unit {
    val storage = IpRuleNormalizer.storageText(address)
    val existing = IpRulesManager.getObj(uid, storage, port)
    if (existing == null) {
        IpRulesManager.addIpRule(uid, address, port.takeIf { it != 0 }, status, proxyId = "", proxyCC = "")
        return { IpRulesManager.removeIpRule(uid, storage, port) }
    }
    val previous = IpRuleStatus.getStatus(existing.status)
    existing.applyStatus(status)
    return { IpRulesManager.getObj(uid, storage, port)?.applyStatus(previous) }
}

private suspend fun CustomIp.applyStatus(status: IpRuleStatus) = when (status) {
    IpRuleStatus.BLOCK -> IpRulesManager.updateBlock(this)
    IpRuleStatus.TRUST -> IpRulesManager.updateTrust(this)
    IpRuleStatus.BYPASS_UNIVERSAL -> IpRulesManager.updateBypass(this)
    IpRuleStatus.NONE -> IpRulesManager.updateNoRule(this)
}
