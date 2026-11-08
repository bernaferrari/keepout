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

import Logger
import android.content.Context
import com.bernaferrari.bravedns.util.Constants.Companion.WIREGUARD_FOLDER_NAME
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/** Plain UTF-8 configs under private files/wireguard, committed by synced atomic replacement. */
object WireguardConfigFileManager {
    private const val LOG_TAG = "WgConfigFileMgr"

    /**
     * Returns the directory used for WireGuard config files.
     * Creates the directory if it does not exist.
     */
    fun getConfigDirectory(ctx: Context): File {
        val dir = File(ctx.filesDir, WIREGUARD_FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Writes a WireGuard config string to a plain file.
     *
     * @return the written [File]
     * @throws IOException if the file cannot be written
     */
    @Throws(IOException::class)
    fun write(ctx: Context, cfg: String, fileName: String): File {
        val dir = getConfigDirectory(ctx)
        val file = File(dir, fileName)
        write(file, cfg)
        return file
    }

    /**
     * Writes a WireGuard config string to the given file.
     *
     * @throws IOException if the file cannot be written
     */
    @Throws(IOException::class)
    fun write(file: File, cfg: String) {
        Logger.d(LOG_TAG, "writing wg config to plain file: ${file.absolutePath}")
        file.parentFile?.mkdirs()
        val staged = File.createTempFile("wg-write-", ".tmp", file.absoluteFile.parentFile)
        try {
            java.io.FileOutputStream(staged).use { output ->
                output.write(cfg.toByteArray(StandardCharsets.UTF_8))
                output.fd.sync()
            }
            // Same-directory POSIX rename keeps the previous config intact until commit.
            android.system.Os.rename(staged.absolutePath, file.absolutePath)
        } catch (error: android.system.ErrnoException) {
            throw IOException("Unable to commit WireGuard configuration", error)
        } finally {
            staged.delete()
        }
    }

    /**
     * Reads a WireGuard config file as a byte array.
     *
     * @throws IOException if the file cannot be read
     */
    @Throws(IOException::class)
    fun read(file: File): ByteArray {
        return file.readBytes()
    }

}
