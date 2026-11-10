package com.bernaferrari.bravedns.service

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnSocketAddressTest {
    @Test
    fun parsesGoSocketAddresses() {
        assertEquals("192.0.2.1" to 53, parseIpAndPort(" 192.0.2.1:53 "))
        assertEquals("2001:db8::1" to 443, parseIpAndPort("[2001:db8::1]:443"))
        assertEquals("fe80::1%wlan0" to 0, parseIpAndPort("[fe80::1%wlan0]"))
        assertEquals("" to 0, parseIpAndPort(null))
    }
}
