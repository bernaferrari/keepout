/*
 * Copyright 2026 RethinkDNS and its authors
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
package com.bernaferrari.bravedns.service

import com.bernaferrari.bravedns.wireguard.BadConfigException
import com.bernaferrari.bravedns.wireguard.Config
import com.bernaferrari.bravedns.wireguard.Peer
import com.bernaferrari.bravedns.wireguard.WgInterface

/**
 * Pure WireGuard config text parse + identity rebuild.
 *
 * [Config.parse] already owns the wg-quick grammar; this helper only attaches
 * manager identity (id/name) and rebuilds after interface/peer edits.
 */
internal object WgConfigParser {

    @Throws(BadConfigException::class)
    fun parseText(text: String): Config = Config.parse(text)

    fun withIdentity(parsed: Config, id: Int, name: String): Config {
        return rebuild(id, name, parsed.getInterface(), parsed.getPeers())
    }

    fun addPeer(config: Config, peer: Peer): Config {
        check(config.getPeers().orEmpty().none { it.getPublicKey().base64() == peer.getPublicKey().base64() }) {
            "A peer with this public key already exists"
        }
        return rebuild(config.getId(), config.getName(), config.getInterface(), config.getPeers().orEmpty() + peer)
    }

    fun replacePeer(config: Config, previous: Peer, replacement: Peer): Config {
        val peers = config.getPeers().orEmpty().toMutableList()
        val index = peers.indexOfFirst { it.toWgQuickString() == previous.toWgQuickString() }
        check(index >= 0) { "WireGuard peer changed; reopen the editor" }
        check(peers.withIndex().none { (i, peer) ->
            i != index && peer.getPublicKey().base64() == replacement.getPublicKey().base64()
        }) { "A peer with this public key already exists" }
        peers[index] = replacement
        return rebuild(config.getId(), config.getName(), config.getInterface(), peers)
    }

    fun rebuild(
        id: Int,
        name: String,
        iface: WgInterface?,
        peers: Collection<Peer>?,
    ): Config {
        return Config.Builder()
            .setId(id)
            .setName(name)
            .setInterface(iface)
            .addPeers(peers)
            .build()
    }
}
