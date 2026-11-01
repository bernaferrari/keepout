/*
 * Copyright 2024 RethinkDNS and its authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bernaferrari.bravedns.ui.compose.navigation

import com.bernaferrari.bravedns.data.SummaryStatisticsType
import com.bernaferrari.bravedns.ui.compose.dns.ConfigureRethinkScreenType
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRulesMode
import com.bernaferrari.bravedns.ui.compose.firewall.RethinkRulesTab
import com.bernaferrari.bravedns.ui.compose.logs.ConnectionSheetNavigation
import com.bernaferrari.bravedns.ui.compose.logs.DomainConnectionsInputType
import com.bernaferrari.bravedns.ui.compose.wireguard.WgType
import com.bernaferrari.bravedns.util.Constants.Companion.UID_EVERYBODY
import com.bernaferrari.bravedns.viewmodel.DomainConnectionsViewModel
import com.bernaferrari.bravedns.viewmodel.SummaryStatisticsViewModel
import kotlinx.serialization.Serializable

@Serializable
enum class CustomRulesTab(val value: Int) {
    IP(0),
    DOMAIN(1);

    companion object {
        fun fromValue(value: Int): CustomRulesTab = entries.firstOrNull { it.value == value } ?: IP
    }
}

@Serializable
enum class CustomRulesMode(val value: Int) {
    ALL_RULES(0),
    APP_SPECIFIC(1);

    companion object {
        fun fromValue(value: Int): CustomRulesMode = entries.firstOrNull { it.value == value } ?: APP_SPECIFIC
    }
}

/** App-side enum payloads -> the typed [RethinkRoute] graph (which stores primitives). */
fun detailedStatsRoute(
    type: SummaryStatisticsType,
    timeCategory: SummaryStatisticsViewModel.TimeCategory,
): RethinkRoute = RethinkRoute.DetailedStatistics(type.tid, timeCategory.value)

fun customRulesRoute(
    uid: Int = UID_EVERYBODY,
    tab: CustomRulesTab = CustomRulesTab.IP,
    mode: CustomRulesMode = CustomRulesMode.APP_SPECIFIC,
): RethinkRoute = RethinkRoute.CustomRules(uid, tab.value, mode.value)

fun wireGuardDetailRoute(configId: Int, wgType: WgType): RethinkRoute =
    RethinkRoute.WireGuardDetail(configId, wgType.value)

fun wireGuardEditorRoute(configId: Int, wgType: WgType): RethinkRoute =
    RethinkRoute.WireGuardEditor(configId, wgType.value)

fun domainConnectionsRoute(
    type: DomainConnectionsInputType,
    flag: String,
    domain: String,
    asn: String,
    ip: String,
    isBlocked: Boolean,
    timeCategory: DomainConnectionsViewModel.TimeCategory,
): RethinkRoute = RethinkRoute.DomainConnections(
    typeId = type.type,
    flag = flag,
    domain = domain,
    asn = asn,
    ip = ip,
    isBlocked = isBlocked,
    timeCategory = timeCategory.value,
)

fun configureRethinkBasicRoute(
    screenType: ConfigureRethinkScreenType,
    remoteName: String = "",
    remoteUrl: String = "",
    uid: Int = -1,
): RethinkRoute = RethinkRoute.ConfigureRethinkBasic(
    screenTypeOrdinal = screenType.ordinal,
    remoteName = remoteName,
    remoteUrl = remoteUrl,
    uid = uid,
)

/**
 * Where the Connection sheet's links lead, pushed on the stack that opened the sheet (so the tab
 * stays highlighted). WS2+ screens that open the sheet (App detail's Recent activity) use this.
 *
 * - Universal rules: the Firewall screen, scrolled to the deciding rule's row (its id is the focus key).
 * - Rules: the app's (or all apps') rule list on the domain or IP tab, searched for the address.
 */
fun connectionSheetNavigation(nav: RethinkNavOps) = ConnectionSheetNavigation(
    onOpenUniversalRules = { focusKey -> nav.push(RethinkRoute.FirewallSettings(focusKey)) },
    onOpenProxySettings = { nav.push(RethinkRoute.ProxySettings()) },
    onOpenRules = { uid, tab, query ->
        nav.push(RethinkRoute.CustomRules(uid = uid, tab = tab.value, mode = RethinkRulesMode.SELECTED_UID.value, query = query))
    },
    onOpenApp = { uid -> nav.push(RethinkRoute.AppInfo(uid)) },
)

/** The Firewall screen's Rules entry: every rule, domains first. */
internal val allRulesRoute = RethinkRoute.CustomRules(
    uid = UID_EVERYBODY,
    tab = RethinkRulesTab.DOMAIN.value,
    mode = RethinkRulesMode.ALL_APPS.value,
)

/** Shared route keys that Android renders through a richer screen of its own. */
internal fun RethinkRoute.forAndroid(): RethinkRoute = when (this) {
    RethinkRoute.DnsSettings -> RethinkRoute.DnsDetail()
    RethinkRoute.Blocklists -> RethinkRoute.ConfigureRethinkBasic(
        screenTypeOrdinal = ConfigureRethinkScreenType.REMOTE.ordinal,
    )
    is RethinkRoute.RpnServerDetails -> RethinkRoute.CountryProxy(key)
    else -> this
}

/** A route's focus key as a screen's initial focus: blank means "no focus". */
internal fun String.asFocusKey(): String? = takeIf { it.isNotBlank() }
