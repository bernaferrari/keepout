/*
 * Copyright 2026 RethinkDNS and its authors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.bravedns.service

import com.bernaferrari.bravedns.wireguard.BadConfigException
import com.bernaferrari.bravedns.wireguard.Peer
import com.bernaferrari.bravedns.wireguard.WgInterface
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WgConfigParserTest {

    @Test
    fun `withIdentity attaches id and name`() {
        val iface = mockk<WgInterface>(relaxed = true)
        val parsed = WgConfigParser.rebuild(-1, "", iface, emptyList())
        val identified = WgConfigParser.withIdentity(parsed, id = 7, name = "home")
        assertEquals(7, identified.getId())
        assertEquals("home", identified.getName())
        assertSame(iface, identified.getInterface())
    }

    @Test
    fun `rebuild keeps peers from source`() {
        val iface = mockk<WgInterface>(relaxed = true)
        val peers = listOf(mockk<Peer>(relaxed = true))
        val rebuilt = WgConfigParser.rebuild(3, "office", iface, peers)
        assertEquals(3, rebuilt.getId())
        assertEquals("office", rebuilt.getName())
        assertEquals(peers, rebuilt.getPeers())
    }

    @Test fun replacementKeepsOriginalUntilCompleteConfigIsReady() {
        val original = mockk<Peer>(relaxed = true) { io.mockk.every { toWgQuickString() } returns "original" }
        val other = mockk<Peer>(relaxed = true) { io.mockk.every { toWgQuickString() } returns "other" }
        val replacement = mockk<Peer>(relaxed = true) {
            io.mockk.every { toWgQuickString() } returns "replacement"
            io.mockk.every { getPublicKey().base64() } returns "new-key"
        }
        val config = WgConfigParser.rebuild(3, "office", mockk(relaxed = true), listOf(original, other))
        val edited = WgConfigParser.replacePeer(config, original, replacement)
        assertEquals(listOf(original, other), config.getPeers())
        assertEquals(listOf(replacement, other), edited.getPeers())
        assertEquals(3, edited.getId())
        assertSame(config.getInterface(), edited.getInterface())
    }

    @Test(expected = IllegalStateException::class)
    fun addingPeerRejectsAnExistingPublicKey() {
        val first = mockk<Peer> { io.mockk.every { getPublicKey().base64() } returns "same" }
        val second = mockk<Peer> { io.mockk.every { getPublicKey().base64() } returns "same" }
        val cfg = WgConfigParser.rebuild(3, "office", mockk(relaxed = true), listOf(first))
        WgConfigParser.addPeer(cfg, second)
    }

    @Test(expected = IllegalStateException::class)
    fun replacementRejectsAStalePeer() {
        val config = WgConfigParser.rebuild(3, "office", mockk(relaxed = true), emptyList())
        WgConfigParser.replacePeer(config, mockk(relaxed = true), mockk(relaxed = true))
    }

    @Test(expected = BadConfigException::class)
    fun `parseText rejects missing interface section`() {
        WgConfigParser.parseText("PrivateKey = aaa\n")
    }

    @Test(expected = BadConfigException::class)
    fun `parseText rejects unknown section`() {
        WgConfigParser.parseText("[Nope]\nFoo = bar\n")
    }
}
