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
import Logger.LOG_TAG_PROXY
import android.content.Context
import com.bernaferrari.bravedns.backup.BackupHelper.Companion.TEMP_WG_DIR
import com.bernaferrari.bravedns.database.WgConfigFiles
import com.bernaferrari.bravedns.database.WgConfigFilesRepository
import com.bernaferrari.bravedns.util.Constants.Companion.WIREGUARD_FOLDER_NAME
import com.bernaferrari.bravedns.util.Utilities
import com.bernaferrari.bravedns.wireguard.BadConfigException
import com.bernaferrari.bravedns.wireguard.Config
import java.io.File
import java.nio.charset.StandardCharsets

internal data class WgPersistResult(
    val path: String,
    val existing: WgConfigFiles?,
)

/**
 * File + Room persistence for WireGuard configs.
 *
 * Owns plaintext `.conf` files, database path commits, and backup restore from [TEMP_WG_DIR].
 * The manager publishes committed configs to the in-memory registry.
 */
internal class WgConfigStore(
    private val context: Context,
    private val db: WgConfigFilesRepository,
) {
    fun configDirPath(): String {
        return context.filesDir.absolutePath +
            File.separator +
            WIREGUARD_FOLDER_NAME +
            File.separator
    }

    fun parseConfigFile(path: String): Result<Config> {
        return try {
            val bytes = WireguardConfigFileManager.read(File(path))
            Result.success(Config.parse(bytes.toString(StandardCharsets.UTF_8)))
        } catch (e: BadConfigException) {
            Logger.e(LOG_TAG_PROXY, "Config parse failure for wg config: $path", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Logger.e(LOG_TAG_PROXY, "Config parse failure for wg config: $path", e)
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Logger.e(LOG_TAG_PROXY, "Config parse failure for wg config: $path", e)
            Result.failure(e)
        }
    }

    /** The database path is the commit point; never overwrite a referenced file. */
    suspend fun persist(cfg: Config, serverResponse: String = ""): WgPersistResult =
        com.bernaferrari.bravedns.backup.BackupRestoreFileGate.withSourceFiles {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                val previous = db.isConfigAdded(cfg.getId())
                val name = "wg${cfg.getId()}-${java.util.UUID.randomUUID()}.conf"
                val target = File(configDirPath(), name)
                val updated = WgConfigFiles(
                    cfg.getId(), cfg.getName(), target.absolutePath,
                    serverResponse.ifEmpty { previous?.serverResponse.orEmpty() },
                    isActive = previous?.isActive ?: false,
                    isCatchAll = previous?.isCatchAll ?: false,
                    isLockdown = previous?.isLockdown ?: false,
                    oneWireGuard = previous?.oneWireGuard ?: false,
                    useOnlyOnMetered = previous?.useOnlyOnMetered ?: false,
                    isDeletable = previous?.isDeletable ?: true,
                    ssidEnabled = previous?.ssidEnabled ?: false,
                    ssids = previous?.ssids.orEmpty(),
                )
                var committed = false
                try {
                    WireguardConfigFileManager.write(context, cfg.toWgQuickString(), name)
                    if (previous == null) db.insert(updated) else check(
                        db.updateConfiguration(updated.id, updated.name, updated.configPath, updated.serverResponse) == 1,
                    ) { "WireGuard configuration no longer exists" }
                    committed = true
                } finally {
                    // If Room rejects the write, only the unreferenced candidate is removed.
                    if (!committed) removeUnreferencedFile(target)
                }
                previous?.let { removeUnreferencedFile(File(it.configPath)) }
                WgPersistResult(target.absolutePath, updated.takeIf { previous != null })
            }
        }

    fun removeUnreferencedFile(file: File) {
        try {
            if (file.canonicalFile.parentFile != File(configDirPath()).canonicalFile) return
            if (file.exists() && !file.delete()) Logger.w(LOG_TAG_PROXY, "Unable to remove unused WireGuard file: $file")
        } catch (error: java.io.IOException) {
            Logger.w(LOG_TAG_PROXY, "Unable to locate unused WireGuard file: $file", error)
        } catch (error: SecurityException) {
            Logger.w(LOG_TAG_PROXY, "Unable to remove unused WireGuard file: $file", error)
        }
    }

    /** Removes candidates left by a process death before the database commit. */
    suspend fun removeOrphanedFiles() = com.bernaferrari.bravedns.backup.BackupRestoreFileGate.withSourceFiles {
        val referenced = db.getWgConfigs().map { File(it.configPath).absolutePath }.toSet()
        File(configDirPath()).listFiles().orEmpty()
            .filter { it.name.matches(Regex("wg[0-9]+-[a-f0-9-]+\\.conf")) && it.absolutePath !in referenced }
            .forEach(::removeUnreferencedFile)
    }

    suspend fun performRestore() {
        try {
            val tempDir = File(context.filesDir, TEMP_WG_DIR)
            val dbconfs = db.getWgConfigs()

            val tempFiles = tempDir.listFiles()?.filter { it.name.endsWith(".conf") } ?: emptyList()
            Logger.i(
                LOG_TAG_PROXY,
                "performRestore: temp dir .conf files: ${tempFiles.size} (${tempFiles.joinToString { it.name }}), db size: ${dbconfs.size}"
            )

            if (tempFiles.isEmpty() && dbconfs.isEmpty()) {
                Logger.i(LOG_TAG_PROXY, "performRestore: no temp files and no db entries, nothing to restore")
                return
            }

            if (tempFiles.isEmpty()) {
                Logger.w(LOG_TAG_PROXY, "performRestore: db has ${dbconfs.size} entries but no temp .conf files found, cannot restore")
                return
            }

            if (dbconfs.isEmpty()) {
                Logger.w(
                    LOG_TAG_PROXY,
                    "performRestore: ${tempFiles.size} temp .conf files found but db has 0 entries. Files cannot be restored without db metadata. Preserving temp files for manual recovery."
                )
                return
            }

            val restoredIds = mutableSetOf<Int>()
            val unmatchedFiles = mutableListOf<String>()

            dbconfs.forEach { c ->
                if (restoreOneConfig(c, tempDir)) {
                    restoredIds.add(c.id)
                }
            }

            tempFiles.forEach { tf ->
                val id = tf.nameWithoutExtension.toIntOrNull()
                if (id == null || id !in restoredIds) {
                    unmatchedFiles.add(tf.name)
                }
            }

            if (unmatchedFiles.isNotEmpty()) {
                Logger.w(
                    LOG_TAG_PROXY,
                    "performRestore: ${unmatchedFiles.size} temp .conf files unmatched to any db entry: ${unmatchedFiles.joinToString()}. Preserving temp dir for manual recovery."
                )
                return
            }

            Logger.i(LOG_TAG_PROXY, "performRestore: successfully restored ${restoredIds.size} configs, cleaning up temp dir")
            val isResidueDeleted = Utilities.deleteRecursive(tempDir)
            if (isResidueDeleted) {
                Logger.i(LOG_TAG_PROXY, "deleted residue temp wg files: ${tempDir.absolutePath}")
            } else {
                Logger.w(
                    LOG_TAG_PROXY,
                    "failed to delete residue temp wg files: ${tempDir.absolutePath}"
                )
                tempDir.deleteRecursively()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: java.io.IOException) {
            Logger.e(LOG_TAG_PROXY, "performRestore: err restoring wg configs, ${e.message}", e)
        } catch (e: SecurityException) {
            Logger.e(LOG_TAG_PROXY, "performRestore: err restoring wg configs, ${e.message}", e)
        } catch (e: IllegalStateException) {
            Logger.e(LOG_TAG_PROXY, "performRestore: err restoring wg configs, ${e.message}", e)
        }
    }

    private suspend fun restoreOneConfig(c: WgConfigFiles, tempDir: File): Boolean {
        val file = File(tempDir, "${c.id}.conf")
        if (file.exists()) {
            Logger.i(LOG_TAG_PROXY, "performRestore: file exists: ${file.absolutePath} (${file.length()} bytes), proceed restore for config ${c.id}/${c.name}")
        } else {
            Logger.w(LOG_TAG_PROXY, "performRestore: no matching wg file for db entry ${c.id}/${c.name}, expected: ${file.absolutePath}, deleting config entry")
            db.deleteConfig(c.id)
            return false
        }
        val bytes = file.readBytes()
        Logger.i(LOG_TAG_PROXY, "performRestore: read ${bytes.size} bytes from ${file.name}")
        val targetFile = File(c.configPath)
        val parentDir = targetFile.parentFile
        if (parentDir == null) {
            Logger.e(LOG_TAG_PROXY, "performRestore: wg restore failed, invalid path: ${c.configPath}")
            db.deleteConfig(c.id)
            return false
        }
        val created = runCatching {
            if (!targetFile.exists()) {
                parentDir.mkdirs()
                targetFile.createNewFile()
            } else {
                true
            }
        }.getOrElse { ex ->
            Logger.w(
                LOG_TAG_PROXY,
                "performRestore: wg restore failed, unable to create file: ${targetFile.absolutePath}, err: ${ex.message}"
            )
            db.deleteConfig(c.id)
            return false
        }
        if (!created) {
            Logger.e(
                LOG_TAG_PROXY,
                "performRestore: wg restore failed, createNewFile returned false: ${targetFile.absolutePath}"
            )
            db.deleteConfig(c.id)
            return false
        }
        return try {
            targetFile.writeBytes(bytes)
            Logger.i(LOG_TAG_PROXY, "performRestore: restored wg config: ${c.id}, ${c.name} -> ${targetFile.absolutePath}")
            true
        } catch (e: java.io.IOException) {
            Logger.e(
                LOG_TAG_PROXY,
                "performRestore: I/O failure restoring wg config: ${c.id}, ${c.name}",
                e
            )
            false
        }
    }
}
