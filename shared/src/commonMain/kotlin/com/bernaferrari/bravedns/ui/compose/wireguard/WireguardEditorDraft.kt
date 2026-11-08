package com.bernaferrari.bravedns.ui.compose.wireguard

import com.bernaferrari.bravedns.wireguard.WgCrypto
import com.bernaferrari.bravedns.wireguard.WgInterface

/** Applies visible fields while preserving imported extension and application settings. */
fun RethinkWireguardEditorState.buildInterface(original: WgInterface?): WgInterface =
    WgInterface.Builder(original).apply {
        addresses.clear()
        dnsServers.clear()
        dnsSearchDomains.clear()
        parsePrivateKey(this@buildInterface.privateKey)
        parseAddresses(this@buildInterface.addresses)
        parseListenPort(this@buildInterface.listenPort.ifEmpty { "0" })
        parseDnsServers(this@buildInterface.dnsServers)
        parseMtu(this@buildInterface.mtu.ifEmpty { "-1" })
    }.build()

// Native key parsers report malformed keys as platform-specific exceptions.
@Suppress("TooGenericExceptionCaught")
fun RethinkWireguardEditorState.withPrivateKey(value: String): RethinkWireguardEditorState {
    val public = try {
        if (value.isBlank()) "" else WgCrypto.parsePrivateKey(value).mult().base64()
    } catch (_: Exception) {
        ""
    }
    return copy(privateKey = value, publicKey = public)
}
