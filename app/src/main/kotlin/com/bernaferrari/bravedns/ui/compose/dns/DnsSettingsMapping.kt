/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.dns

import com.bernaferrari.bravedns.data.AppConfig
import com.bernaferrari.bravedns.service.BlockFreeDnsMode

// Two-way mapping between the Android DNS model and the shared DNS screens' types.

/** The Android view-model state, in the shared screen's vocabulary. */
internal fun DnsSettingsUiState.toRethinkDnsSettingsState(presetLabel: String) = RethinkDnsSettingsState(
    recentLatencies = recentLatencies,
    connectedDnsName = connectedDnsName,
    dnsLatency = dnsLatency,
    dnsType = dnsType.toRethinkDnsType(),
    isSmartDnsEnabled = isSmartDnsEnabled,
    isSystemDnsEnabled = isSystemDnsEnabled,
    isRethinkDnsConnected = isRethinkDnsConnected,
    fetchFavIcon = fetchFavIcon,
    preventDnsLeaks = preventDnsLeaks,
    enableDnsAlg = enableDnsAlg,
    periodicallyCheckBlocklistUpdate = periodicallyCheckBlocklistUpdate,
    useCustomDownloadManager = useCustomDownloadManager,
    enableDnsCache = enableDnsCache,
    proxyDns = proxyDns,
    useSystemDnsForUndelegatedDomains = useSystemDnsForUndelegatedDomains,
    useFallbackDnsToBypass = useFallbackDnsToBypass,
    blockFreeDnsMode = blockFreeDnsMode.toRethinkBlockFreeDnsMode(),
    blocklistEnabled = blocklistEnabled,
    numberOfLocalBlocklists = numberOfLocalBlocklists,
    bypassBlockInDns = bypassBlockInDns,
    splitDns = splitDns,
    dnsRecordTypesAutoMode = dnsRecordTypesAutoMode,
    allowedDnsRecordTypesSize = allowedDnsRecordTypesSize,
    showSplitDns = isShowSplitDns,
    showBypassDnsBlock = isShowBypassDnsBlock,
    isRefreshing = isRefreshing,
    rethinkPresetLabel = presetLabel,
    isDnsFailing = isDnsFailing,
    isRethinkMax = isRethinkMax,
    fallbackDnsName = fallbackDnsName,
    localBlocklistsDownloaded = localBlocklistsDownloaded,
)

private fun AppConfig.DnsType.toRethinkDnsType() = when (this) {
    AppConfig.DnsType.DOH -> RethinkDnsType.Doh
    AppConfig.DnsType.RETHINK_REMOTE -> RethinkDnsType.RethinkRemote
    AppConfig.DnsType.SMART_DNS -> RethinkDnsType.SmartDns
    AppConfig.DnsType.DOT -> RethinkDnsType.Dot
    AppConfig.DnsType.ODOH -> RethinkDnsType.Odoh
    AppConfig.DnsType.DNSCRYPT -> RethinkDnsType.DnsCrypt
    AppConfig.DnsType.DNS_PROXY -> RethinkDnsType.DnsProxy
    AppConfig.DnsType.SYSTEM_DNS -> RethinkDnsType.System
}

private fun BlockFreeDnsMode.toRethinkBlockFreeDnsMode() = when (this) {
    BlockFreeDnsMode.AUTO -> RethinkBlockFreeDnsMode.Auto
    BlockFreeDnsMode.GLOBAL -> RethinkBlockFreeDnsMode.Global
    BlockFreeDnsMode.FALLBACK -> RethinkBlockFreeDnsMode.Fallback
}

internal fun RethinkBlockFreeDnsMode.toAndroidBlockFreeDnsMode() = when (this) {
    RethinkBlockFreeDnsMode.Auto -> BlockFreeDnsMode.AUTO
    RethinkBlockFreeDnsMode.Global -> BlockFreeDnsMode.GLOBAL
    RethinkBlockFreeDnsMode.Fallback -> BlockFreeDnsMode.FALLBACK
}
