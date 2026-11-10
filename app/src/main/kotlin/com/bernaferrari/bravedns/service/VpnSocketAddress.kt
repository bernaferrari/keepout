package com.bernaferrari.bravedns.service

import inet.ipaddr.HostName
import inet.ipaddr.IPAddressString

internal fun parseIpAndPort(endpoint: String?): Pair<String, Int> {
    val text = endpoint?.trim().orEmpty()
    if (text.isEmpty()) return "" to 0

    val bracket = text.indexOf(']')
    if (text.startsWith('[') && bracket > 0) {
        val port = text.substring(bracket + 1).removePrefix(":").toIntOrNull() ?: 0
        return text.substring(1, bracket) to port
    }

    val separator = text.lastIndexOf(':')
    if (separator > 0) {
        val port = text.substring(separator + 1).toIntOrNull()
        val host = text.substring(0, separator)
        // Preserve the legacy unbracketed host:port form when its host is a complete address.
        if (port != null && (':' !in host || IPAddressString(host).isIPv6)) return host to port
    }
    return parseComplexAddress(text)
}

private fun parseComplexAddress(text: String): Pair<String, Int> = try {
    val host = HostName(text)
    (host.asAddress()?.toString() ?: text) to (host.port ?: 0)
} catch (_: inet.ipaddr.HostNameException) {
    text to 0
} catch (_: inet.ipaddr.AddressStringException) {
    text to 0
}
