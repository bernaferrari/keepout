/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.firewall

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.service.DomainRulesManager
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.IpRulesManager
import com.bernaferrari.bravedns.util.Constants.Companion.UID_EVERYBODY
import com.bernaferrari.bravedns.viewmodel.CustomDomainViewModel
import com.bernaferrari.bravedns.viewmodel.CustomIpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Android adapter for the Rules screen. [uid] "everybody" opens the full screen with its scope chip
 * ([initialMode]: every rule, or the all-apps ones); an app uid keeps the list to that app and names
 * it in the title. Adds go through [AddRuleSheetHost] (Block or Trust, for all apps or one), deletes
 * through [IpRulesManager] / [DomainRulesManager]. [initialQuery] prefills the opened tab's search.
 */
@Composable
fun CustomRulesScreen(
    uid: Int = UID_EVERYBODY,
    initialTab: RethinkRulesTab = RethinkRulesTab.IP,
    initialMode: RethinkRulesMode = RethinkRulesMode.SELECTED_UID,
    domainViewModel: CustomDomainViewModel,
    ipViewModel: CustomIpViewModel,
    eventLogger: EventLogger,
    initialQuery: String = "",
    onBackClick: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val lockedToApp = uid != UID_EVERYBODY
    var selectedTab by rememberSaveable(uid, initialTab) { mutableStateOf(initialTab) }
    val search = rememberSaveable(uid, saver = RuleSearch.Saver) { RuleSearch.opening(initialTab, initialQuery) }
    val lockedAppName = rememberAppName(uid, load = lockedToApp)
    var chosenScope by remember(uid, initialMode) { mutableStateOf(initialRulesScope(uid, initialMode)) }
    // An app's own screen stays on that app, titled with its name once loaded.
    val rulesScope = (chosenScope as? RethinkRulesScope.App)?.takeIf { lockedToApp }?.copy(label = lockedAppName) ?: chosenScope
    val scopedApp = rulesScope as? RethinkRulesScope.App
    val everyRule = rulesScope is RethinkRulesScope.All
    val listUid = scopedApp?.uid ?: UID_EVERYBODY
    val query = search[selectedTab]
    val ownerNames = rememberRuleOwnerNames()
    val picker = rememberRuleAppPicker()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val editRule = rememberRuleEditAction(snackbar, eventLogger)

    LaunchedEffect(listUid) {
        ipViewModel.setUid(listUid)
        domainViewModel.setUid(listUid)
    }
    LaunchedEffect(selectedTab, rulesScope, query) {
        delay(250)
        if (selectedTab == RethinkRulesTab.IP) ipViewModel.setFilter(query) else domainViewModel.setFilter(query)
    }

    RethinkCustomRulesScreen(
        selectedTab = selectedTab,
        scope = rulesScope,
        query = query,
        rules = rulesFeed(selectedTab, everyRule, ipViewModel, domainViewModel, ownerNames.names),
        strings = androidCustomRulesStrings(),
        domainCount = rememberRuleCount(everyRule, listUid, domainViewModel::everyDomainRuleCount, domainViewModel::domainRulesCount),
        ipCount = rememberRuleCount(everyRule, listUid, ipViewModel::everyIpRuleCount, ipViewModel::ipRulesCount),
        // Import writes to one owner: the app on its own screen, otherwise the all-apps rules.
        importAction = {
            RulesImportAction(listUid, selectedTab)
            if (!everyRule) RulesExportAction(listUid, selectedTab)
        },
        onTabChange = { selectedTab = it },
        onScopeChange = if (lockedToApp) null else ({ chosenScope = it }),
        onChooseApp = if (lockedToApp) null else ({ picker.open { app -> chosenScope = RethinkRulesScope.App(app.uid, app.label) } }),
        onQueryChange = { value -> search[selectedTab] = value },
        onAddRule = { showAddSheet = true },
        onDeleteRule = { tab, rule -> scope.launch(Dispatchers.IO) { deleteRule(tab, rule, eventLogger) } },
        onRuleOwnerVisible = ownerNames::load,
        onBackClick = onBackClick,
        onEditRule = editRule,
        snackbarHostState = snackbar,
    )

    if (showAddSheet) {
        AddRuleSheet(
            target = selectedTab,
            app = scopedApp,
            eventLogger = eventLogger,
            onDismiss = { showAddSheet = false },
            onChooseApp = if (lockedToApp) null else picker::open,
        )
    }
    RuleAppPickerHost(picker, selectedUid = scopedApp?.uid)
}

/** An app's own screen opens on that app; the full screen on every rule or the all-apps ones. */
private fun initialRulesScope(uid: Int, mode: RethinkRulesMode): RethinkRulesScope = when {
    uid != UID_EVERYBODY -> RethinkRulesScope.App(uid, "")
    mode == RethinkRulesMode.ALL_APPS -> RethinkRulesScope.All
    else -> RethinkRulesScope.AllApps
}

/** A tab's rule count: every rule's when [everyRule], else the rules owned by [uid]. */
@Composable
private fun rememberRuleCount(
    everyRule: Boolean,
    uid: Int,
    every: () -> Flow<Int>,
    ownedBy: (Int) -> Flow<Int>,
): Int? {
    val count by remember(everyRule, uid) { if (everyRule) every() else ownedBy(uid) }.collectAsState(initial = null)
    return count
}

/** The Add rule sheet, starting on the scope being looked at; All rules starts on all apps. */
@Composable
private fun AddRuleSheet(
    target: RethinkRulesTab,
    app: RethinkRulesScope.App?,
    eventLogger: EventLogger,
    onDismiss: () -> Unit,
    onChooseApp: ((onPicked: (RethinkAddRuleScope.App) -> Unit) -> Unit)?,
) {
    val thisApp = stringResource(R.string.rules_this_app)
    AddRuleSheetHost(
        target = target,
        initialScope = app?.let { RethinkAddRuleScope.App(it.uid, it.label.ifBlank { thisApp }) } ?: RethinkAddRuleScope.AllApps,
        eventLogger = eventLogger,
        onDismiss = onDismiss,
        onChooseApp = onChooseApp,
    )
}

/** The Rules screen's two searches, one per tab: each tab keeps its own while the other shows. */
@Stable
private class RuleSearch(ip: String, domain: String) {
    private var ip by mutableStateOf(ip)
    private var domain by mutableStateOf(domain)

    operator fun get(tab: RethinkRulesTab): String = if (tab == RethinkRulesTab.IP) ip else domain

    operator fun set(tab: RethinkRulesTab, value: String) {
        if (tab == RethinkRulesTab.IP) ip = value else domain = value
    }

    companion object {
        /** [query] prefills the tab the screen opens on. */
        fun opening(tab: RethinkRulesTab, query: String) = RuleSearch(
            ip = if (tab == RethinkRulesTab.IP) query else "",
            domain = if (tab == RethinkRulesTab.DOMAIN) query else "",
        )

        val Saver = listSaver<RuleSearch, String>(
            save = { listOf(it.ip, it.domain) },
            restore = { RuleSearch(ip = it[0], domain = it[1]) },
        )
    }
}
