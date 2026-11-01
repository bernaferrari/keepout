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

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.bravedns.ui.compose.dns.BlockFreeDnsScreen
import com.bernaferrari.bravedns.ui.compose.dns.ConfigureOtherDnsScreen
import com.bernaferrari.bravedns.ui.compose.dns.ConfigureRethinkBasicScreen
import com.bernaferrari.bravedns.ui.compose.dns.ConfigureRethinkScreenType
import com.bernaferrari.bravedns.ui.compose.dns.DnsDetailScreen
import com.bernaferrari.bravedns.ui.compose.dns.DnsListScreen
import com.bernaferrari.bravedns.ui.compose.dns.DnsScreenType
import com.bernaferrari.bravedns.ui.compose.dns.isDnsAdvancedFocusKey

/** DNS: the resolver screen and its Advanced page, blocklists, and the other-DNS endpoint lists. */
internal fun EntryProviderScope<NavKey>.dnsEntries(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    dns: HomeDnsDependencies,
) {
    entry<RethinkRoute.DnsDetail> { args ->
        DnsEntry(
            nav, core, dns,
            focusKey = args.focusKey.asFocusKey(),
            // A search result for an expert setting opens the screen that shows it.
            advanced = isDnsAdvancedFocusKey(args.focusKey),
        )
    }
    entry<RethinkRoute.DnsSettings> {
        DnsEntry(nav, core, dns)
    }
    entry<RethinkRoute.DnsAdvanced> { args ->
        DnsEntry(nav, core, dns, focusKey = args.focusKey.asFocusKey(), advanced = true)
    }
    entry<RethinkRoute.BlockFreeDns> {
        BlockFreeDnsScreen(dns.blockFreeDnsViewModel, core.persistentState) { nav.popBackStack() }
    }
    entry<RethinkRoute.Blocklists> {
        BlocklistsEntry(nav, core, dns, screenType = ConfigureRethinkScreenType.REMOTE)
    }
    entry<RethinkRoute.ConfigureRethinkBasic> { args ->
        BlocklistsEntry(
            nav, core, dns,
            screenType = ConfigureRethinkScreenType.entries.getOrElse(args.screenTypeOrdinal) {
                ConfigureRethinkScreenType.REMOTE
            },
            remoteName = args.remoteName,
            remoteUrl = args.remoteUrl,
            uid = args.uid,
        )
    }
    entry<RethinkRoute.DnsList> {
        val endpoints = dns.endpoints
        DnsListScreen(
            appConfig = core.appConfig,
            persistentState = core.persistentState,
            dohViewModel = endpoints.doh,
            dotViewModel = endpoints.dot,
            dnsProxyViewModel = endpoints.dnsProxy,
            dnsCryptViewModel = endpoints.dnsCrypt,
            oDohViewModel = endpoints.oDoh,
            onConfigureOtherDns = dns.actions.onConfigureOtherDns,
            onBackClick = { nav.popBackStack() }
        )
    }
    entry<RethinkRoute.ConfigureOtherDns> { args ->
        val endpoints = dns.endpoints
        ConfigureOtherDnsScreen(
            dnsType = DnsScreenType.fromIndex(args.dnsType),
            appConfig = core.appConfig,
            persistentState = core.persistentState,
            dohViewModel = endpoints.doh,
            dotViewModel = endpoints.dot,
            dnsProxyViewModel = endpoints.dnsProxy,
            dnsCryptViewModel = endpoints.dnsCrypt,
            dnsCryptRelayViewModel = endpoints.dnsCryptRelay,
            oDohViewModel = endpoints.oDoh,
            onBackClick = { nav.popBackStack() }
        )
    }
}

/**
 * The DNS screen, or its Advanced page when [advanced]. Advanced has no link to itself, so only
 * the main page offers the way in.
 */
@Composable
private fun DnsEntry(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    dns: HomeDnsDependencies,
    focusKey: String? = null,
    advanced: Boolean = false,
) {
    DnsDetailScreen(
        viewModel = dns.settingsViewModel,
        persistentState = core.persistentState,
        appDownloadManager = dns.appDownloadManager,
        initialFocusKey = focusKey,
        onCustomDnsClick = dns.actions.onDnsCustomDnsClick,
        onRethinkPlusDnsClick = dns.actions.onDnsRethinkPlusDnsClick,
        onLocalBlocklistConfigureClick = dns.actions.onDnsLocalBlocklistConfigureClick,
        onBlockFreeDnsClick = { nav.push(RethinkRoute.BlockFreeDns) },
        onBackClick = { nav.popBackStack() },
        advanced = advanced,
        onAdvancedClick = { nav.push(RethinkRoute.DnsAdvanced()) },
    )
}

/** Keepout DNS (Rethink) blocklists: the remote or local packs, or one saved configuration. */
@Composable
private fun BlocklistsEntry(
    nav: RethinkNavOps,
    core: HomeCoreDependencies,
    dns: HomeDnsDependencies,
    screenType: ConfigureRethinkScreenType,
    remoteName: String = "",
    remoteUrl: String = "",
    uid: Int = -1,
) {
    val blocklists = dns.blocklists
    ConfigureRethinkBasicScreen(
        screenType = screenType,
        remoteName = remoteName,
        remoteUrl = remoteUrl,
        uid = uid,
        persistentState = core.persistentState,
        appConfig = core.appConfig,
        appDownloadManager = dns.appDownloadManager,
        rethinkEndpointViewModel = blocklists.rethinkEndpoint,
        remoteFileTagViewModel = blocklists.remoteFileTag,
        localFileTagViewModel = blocklists.localFileTag,
        remoteBlocklistPacksMapViewModel = blocklists.remotePacksMap,
        localBlocklistPacksMapViewModel = blocklists.localPacksMap,
        onEditConfiguration = { type, name, url -> nav.push(configureRethinkBasicRoute(type, name, url)) },
        onBackClick = { nav.popBackStack() }
    )
}
