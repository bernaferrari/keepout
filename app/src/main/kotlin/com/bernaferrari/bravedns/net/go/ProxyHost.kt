package com.bernaferrari.bravedns.net.go

internal fun bracketIpv6Host(host: String): String =
    if (':' in host && !host.startsWith('[')) "[$host]" else host
