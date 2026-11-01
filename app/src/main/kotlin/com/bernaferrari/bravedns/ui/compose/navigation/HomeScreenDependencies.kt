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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.bernaferrari.bravedns.data.AppConfig
import com.bernaferrari.bravedns.data.SummaryStatisticsType
import com.bernaferrari.bravedns.database.AppDatabase
import com.bernaferrari.bravedns.database.ConnectionTrackerRepository
import com.bernaferrari.bravedns.database.ConsoleLogRepository
import com.bernaferrari.bravedns.database.DnsLogRepository
import com.bernaferrari.bravedns.database.EventDao
import com.bernaferrari.bravedns.database.RefreshDatabase
import com.bernaferrari.bravedns.download.AppDownloadManager
import com.bernaferrari.bravedns.service.EventLogger
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.ui.compose.dns.DnsSettingsViewModel
import com.bernaferrari.bravedns.ui.compose.home.HomeProtectionActions
import com.bernaferrari.bravedns.ui.compose.settings.AppLockResult
import com.bernaferrari.bravedns.viewmodel.AppConnectionsViewModel
import com.bernaferrari.bravedns.viewmodel.AppInfoViewModel
import com.bernaferrari.bravedns.viewmodel.BlockFreeDnsViewModel
import com.bernaferrari.bravedns.viewmodel.CheckoutViewModel
import com.bernaferrari.bravedns.viewmodel.ConsoleLogViewModel
import com.bernaferrari.bravedns.viewmodel.CustomDomainViewModel
import com.bernaferrari.bravedns.viewmodel.CustomIpViewModel
import com.bernaferrari.bravedns.viewmodel.DetailedStatisticsViewModel
import com.bernaferrari.bravedns.viewmodel.DnsCryptEndpointViewModel
import com.bernaferrari.bravedns.viewmodel.DnsCryptRelayEndpointViewModel
import com.bernaferrari.bravedns.viewmodel.DnsProxyEndpointViewModel
import com.bernaferrari.bravedns.viewmodel.DoHEndpointViewModel
import com.bernaferrari.bravedns.viewmodel.DoTEndpointViewModel
import com.bernaferrari.bravedns.viewmodel.DomainConnectionsViewModel
import com.bernaferrari.bravedns.viewmodel.EventsViewModel
import com.bernaferrari.bravedns.viewmodel.LocalBlocklistPacksMapViewModel
import com.bernaferrari.bravedns.viewmodel.ManagePurchaseViewModel
import com.bernaferrari.bravedns.viewmodel.ODoHEndpointViewModel
import com.bernaferrari.bravedns.viewmodel.ProxyAppsMappingViewModel
import com.bernaferrari.bravedns.viewmodel.PurchaseHistoryViewModel
import com.bernaferrari.bravedns.viewmodel.RemoteBlocklistPacksMapViewModel
import com.bernaferrari.bravedns.viewmodel.RethinkEndpointViewModel
import com.bernaferrari.bravedns.viewmodel.RethinkLocalFileTagViewModel
import com.bernaferrari.bravedns.viewmodel.RethinkRemoteFileTagViewModel
import com.bernaferrari.bravedns.viewmodel.ServerOrderHistoryViewModel
import com.bernaferrari.bravedns.viewmodel.SummaryStatisticsViewModel
import com.bernaferrari.bravedns.viewmodel.WgConfigViewModel

/*
 * What the Home nav graph needs from its host, grouped by the part of the app that uses it. Each
 * group feeds one `*Entries` builder, so a screen's dependencies are found next to its routes.
 *
 * The holders carry long-lived references (Activity-scoped ViewModels, singletons, callbacks
 * into the Activity) and are built once and `remember`ed by the host: they never change while
 * composed, which is the promise their @Stable / @Immutable annotations make to Compose.
 */

@Stable
class HomeScreenDependencies(
    val core: HomeCoreDependencies,
    val protection: HomeProtectionActions,
    val activity: HomeActivityDependencies,
    val diagnostics: HomeDiagnosticsDependencies,
    val apps: HomeAppsDependencies,
    val dns: HomeDnsDependencies,
    val proxy: HomeProxyDependencies,
    val rpn: HomeRpnDependencies,
    val settings: HomeSettingsDependencies,
)

/** Services nearly every screen reads: settings, config, the event log and the two log stores. */
@Stable
class HomeCoreDependencies(
    val persistentState: PersistentState,
    val appConfig: AppConfig,
    val eventLogger: EventLogger,
    val connectionTrackerRepository: ConnectionTrackerRepository,
    val dnsLogRepository: DnsLogRepository,
)

/** The Activity tab (log and insights) and the drill-downs it opens. */
@Stable
class HomeActivityDependencies(
    val summaryViewModel: SummaryStatisticsViewModel,
    val detailedStatsViewModel: DetailedStatisticsViewModel,
    val domainConnectionsViewModel: DomainConnectionsViewModel,
    val onOpenDetailedStats: (SummaryStatisticsType) -> Unit,
)

/** Logs & diagnostics: events, the app's console log and the database inspector. */
@Stable
class HomeDiagnosticsDependencies(
    val eventsViewModel: EventsViewModel,
    val eventDao: EventDao,
    val consoleLogViewModel: ConsoleLogViewModel,
    val consoleLogRepository: ConsoleLogRepository,
    val appDatabase: AppDatabase,
    val onShareConsoleLogs: () -> Unit,
    val onConsoleLogsDeleteComplete: () -> Unit,
)

/** The Apps tab: the app list, an app's detail and its rules. */
@Stable
class HomeAppsDependencies(
    val appInfoViewModel: AppInfoViewModel,
    val refreshDatabase: RefreshDatabase,
    val ipRulesViewModel: CustomIpViewModel,
    val domainRulesViewModel: CustomDomainViewModel,
    val connectionsViewModel: AppConnectionsViewModel,
)

/** DNS: the resolver screens, blocklists and the other-DNS endpoint lists. */
@Stable
class HomeDnsDependencies(
    val settingsViewModel: DnsSettingsViewModel,
    val appDownloadManager: AppDownloadManager,
    val blockFreeDnsViewModel: BlockFreeDnsViewModel,
    val blocklists: HomeBlocklistViewModels,
    val endpoints: HomeDnsEndpointViewModels,
    val actions: HomeDnsActions,
)

/** Keepout DNS (Rethink) endpoint and blocklist pickers. */
@Stable
class HomeBlocklistViewModels(
    val rethinkEndpoint: RethinkEndpointViewModel,
    val remoteFileTag: RethinkRemoteFileTagViewModel,
    val localFileTag: RethinkLocalFileTagViewModel,
    val remotePacksMap: RemoteBlocklistPacksMapViewModel,
    val localPacksMap: LocalBlocklistPacksMapViewModel,
)

/** One list per "other DNS" transport. */
@Stable
class HomeDnsEndpointViewModels(
    val doh: DoHEndpointViewModel,
    val dot: DoTEndpointViewModel,
    val dnsProxy: DnsProxyEndpointViewModel,
    val dnsCrypt: DnsCryptEndpointViewModel,
    val dnsCryptRelay: DnsCryptRelayEndpointViewModel,
    val oDoh: ODoHEndpointViewModel,
)

/** Proxies: per-app proxy mapping, Orbot, TCP proxy and WireGuard. */
@Stable
class HomeProxyDependencies(
    val mappingViewModel: ProxyAppsMappingViewModel,
    val wgConfigViewModel: WgConfigViewModel,
    val wgActions: HomeWgActions,
)

/** The Rethink Proxy Network: account, purchases and (where this channel can sell) checkout. */
@Stable
class HomeRpnDependencies(
    /** Null on channels without a store; the Checkout route then explains it is unavailable. */
    val checkoutViewModel: CheckoutViewModel?,
    val managePurchaseViewModel: ManagePurchaseViewModel,
    val purchaseHistoryViewModel: PurchaseHistoryViewModel,
    val serverOrderHistoryViewModel: ServerOrderHistoryViewModel,
)

/** The Settings tab and the host-owned actions its screens trigger. */
@Stable
class HomeSettingsDependencies(
    val isDebug: Boolean,
    val configureActions: HomeConfigureActions,
    val aboutActions: HomeAboutActions,
    val onOpenVpnProfile: () -> Unit,
    val onThemeModeChanged: ((Int) -> Unit)? = null,
    val onThemeColorChanged: ((Int) -> Unit)? = null,
    val onAppLockResult: (AppLockResult) -> Unit = {},
)

/** Settings actions that need the Activity (a dialog first, or another app's screen). */
@Immutable
data class HomeConfigureActions(
    /** The DNS tile checks Private DNS first (a dialog on the Activity). */
    val onConfigureDnsClick: () -> Unit,
    val onOpenAccessibilitySettings: () -> Unit,
    val onNavigateToProxy: () -> Unit,
)

@Immutable
data class HomeAboutActions(
    val onTelegramClick: () -> Unit,
    val onBugReportClick: () -> Unit,
    val onWhatsNewClick: () -> Unit,
    val onAppUpdateClick: () -> Unit,
    val onWebsiteClick: () -> Unit,
    val onGithubClick: () -> Unit,
    val onFaqClick: () -> Unit,
    val onDocsClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
    val onTermsOfServiceClick: () -> Unit,
    val onLicenseClick: () -> Unit,
    val onTwitterClick: () -> Unit,
    val onEmailClick: () -> Unit,
    val onRedditClick: () -> Unit,
    val onElementClick: () -> Unit,
    val onMastodonClick: () -> Unit,
    val onGeneralSettingsClick: () -> Unit,
    val onAppInfoClick: () -> Unit,
    val onVpnProfileClick: () -> Unit,
    val onNotificationClick: () -> Unit,
    val onStatsClick: () -> Unit,
    val onDbStatsClick: () -> Unit,
    val onFlightRecordClick: () -> Unit,
    val onEventLogsClick: () -> Unit,
    val onTokenClick: () -> Unit,
    val onFossClick: () -> Unit,
    val onFlossFundsClick: () -> Unit,
)

@Immutable
data class HomeDnsActions(
    val onDnsCustomDnsClick: () -> Unit,
    val onDnsRethinkPlusDnsClick: () -> Unit,
    val onDnsLocalBlocklistConfigureClick: () -> Unit,
    val onConfigureOtherDns: (Int) -> Unit,
)

@Immutable
data class HomeWgActions(
    val onWgCreateClick: () -> Unit,
    val onWgImportClick: () -> Unit,
    val onWgQrScanClick: () -> Unit,
)
