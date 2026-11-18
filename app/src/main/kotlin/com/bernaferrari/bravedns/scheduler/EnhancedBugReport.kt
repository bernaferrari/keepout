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
package com.bernaferrari.bravedns.scheduler

import Logger.LOG_TAG_BUG_REPORT
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.bernaferrari.bravedns.util.Constants
import com.bernaferrari.bravedns.util.Utilities
import java.io.File
import java.io.IOException
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream


object EnhancedBugReport {

    const val TOMBSTONE_DIR_NAME = "tombstone"
    const val TOMBSTONE_ZIP_FILE_NAME = "rethinkdns.tombstone.zip"


    // GoCrashFileDescriptorReader → gocrash_<ts>.txt (Go runtime panic dump)
    const val PREFIX_GO_CRASH = "gocrash_"
    // EnhancedBugReport (Kotlin/JVM) → kotlin_<ts>.txt
    const val PREFIX_KOTLIN = "kotlin_"

    const val FILE_EXTENSION = ".txt"
    const val FLIGHT_REC_EXT = ".pprof"

    // Total cap across all diagnostic files in the tombstone folder.
    private const val MAX_TOTAL_FILES = 20

    /**
     * Tracks file names that are currently being written by an active reader
     * (GoLogFileDescriptorReader or GoCrashFileDescriptorReader).
     *
     * A file is added here the moment it is created (before any reader touches it) and removed
     * after the writer closes it. Rotation skips files in this set.
     */
    private val activeSessionFileNames = CopyOnWriteArraySet<String>()
    private val fileSequence = AtomicLong()

    /**
     * Marks [fileName] as belonging to the current session.
     * Called immediately after file creation, before handing it to a writer.
     */
    fun markFileAsActive(fileName: String) {
        activeSessionFileNames.add(fileName)
        Log.d(LOG_TAG_BUG_REPORT, "tombstone active: $fileName")
    }

    fun markFileAsClosed(fileName: String) {
        activeSessionFileNames.remove(fileName)
    }

    /** Returns a new File for a Go crash dump (gocrash_<ts>.txt). */
    fun newGoCrashFile(context: Context): File? =
        newTombstoneFile(context, PREFIX_GO_CRASH)

    fun writeLogsToFile(context: Context, logs: String) {
        val file = newTombstoneFile(context, PREFIX_KOTLIN) ?: return
        try {
            val time = Utilities.convertLongToTime(System.currentTimeMillis(), Constants.TIME_FORMAT_3)
            file.writeText("$time\n$logs\n")
            enforceMaxFiles(context, justWritten = file)
        } catch (error: IOException) {
            Log.e(LOG_TAG_BUG_REPORT, "Cannot write Kotlin crash log", error)
        } finally {
            markFileAsClosed(file.name)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addLogsToZipFile(context: Context) {
        try {
            val zipFile = File(context.filesDir, TOMBSTONE_ZIP_FILE_NAME)
            val folder = tombstoneFolder(context.filesDir) ?: return
            val files = folder.listFiles { f ->
                f.isFile() && f.length() > 0L
            } ?: return

            Log.d(LOG_TAG_BUG_REPORT, "zipping ${files.size} non-empty tombstone file(s)")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                files.forEach { file ->
                    try {
                        FileInputStream(file).use { fis ->
                            zos.putNextEntry(ZipEntry(file.name))
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    } catch (_: FileNotFoundException) {
                        Log.e(LOG_TAG_BUG_REPORT, "file vanished: ${file.name}")
                    } catch (e: Exception) {
                        Log.e(LOG_TAG_BUG_REPORT, "err adding ${file.name} to zip: ${e.message}")
                    }
                }
            }
            Log.i(LOG_TAG_BUG_REPORT, "zip created: ${zipFile.absolutePath}")
        } catch (e: FileNotFoundException) {
            Log.e(LOG_TAG_BUG_REPORT, "zip: FileNotFound: ${e.message}")
        } catch (e: ZipException) {
            Log.e(LOG_TAG_BUG_REPORT, "zip: ZipException: ${e.message}")
        } catch (e: Exception) {
            Log.e(LOG_TAG_BUG_REPORT, "zip: unexpected error: ${e.message}")
        }
    }

    fun getTombstoneZipFile(context: Context): File? {
        return try {
            val f = File(context.filesDir, TOMBSTONE_ZIP_FILE_NAME)
            if (f.exists() && f.length() > 0) f else null
        } catch (e: Exception) {
            Log.e(LOG_TAG_BUG_REPORT, "err getting zip: ${e.message}")
            null
        }
    }

    /**
     * Deletes the oldest files until the total count is ≤ [MAX_TOTAL_FILES].
     * [justWritten] is never deleted (it is the file being actively written by a reader).
     */
    private fun enforceFileCountCap(files: List<File>, justWritten: File?) {
        if (files.size <= MAX_TOTAL_FILES) return
        val deleteCount = files.size - MAX_TOTAL_FILES
        Log.i(LOG_TAG_BUG_REPORT,
            "tombstone cap: ${files.size} files, deleting $deleteCount oldest (max=$MAX_TOTAL_FILES)")
        files.filterNot { it == justWritten || activeSessionFileNames.contains(it.name) }
            .take(deleteCount)
            .forEach { f ->
                val ok = f.delete()
                Log.d(LOG_TAG_BUG_REPORT, "rotation: deleted ${f.name}, ok=$ok")
            }
    }

    /**
     * Called by each reader after creating/writing a new file.
     * Re-reads the full folder so rotation is always applied to the combined total.
     */
    fun enforceMaxFiles(context: Context, justWritten: File? = null) {
        try {
            val folder = tombstoneFolder(context.filesDir) ?: return
            // Delete empty files first.
            folder.listFiles { f -> f.isFile && f.length() == 0L && !activeSessionFileNames.contains(f.name) }?.forEach { it.delete() }
            val files = folder.listFiles { f -> f.isFile && f.length() > 0 }
                ?.sortedBy { it.lastModified() } ?: return
            enforceFileCountCap(files, justWritten)
        } catch (e: Exception) {
            Log.e(LOG_TAG_BUG_REPORT, "enforceMaxFiles error: ${e.message}")
        }
    }

    private fun newTombstoneFile(context: Context, prefix: String): File? {
        val folder = tombstoneFolder(context.filesDir) ?: return null
        return try {
            var file: File
            do {
                val timestamp = fileSequence.updateAndGet { maxOf(System.currentTimeMillis(), it + 1) }
                file = File(folder, "$prefix$timestamp$FILE_EXTENSION")
            } while (!file.createNewFile())
            markFileAsActive(file.name)
            file
        } catch (error: IOException) {
            Log.e(LOG_TAG_BUG_REPORT, "Cannot create crash file", error)
            null
        }
    }

    fun tombstoneFolder(filesDir: File): File? {
        val folder = File(filesDir, TOMBSTONE_DIR_NAME)
        if (!folder.exists()) {
            val created = folder.mkdirs()
            if (!created) {
                Log.e(LOG_TAG_BUG_REPORT, "failed to create tombstone dir: ${folder.absolutePath}")
                return null
            }
        }
        if (!folder.isDirectory) {
            Log.e(LOG_TAG_BUG_REPORT, "tombstone path is not a dir: ${folder.absolutePath}")
            return null
        }
        if (!folder.canWrite()) {
            Log.e(LOG_TAG_BUG_REPORT, "tombstone dir not writable: ${folder.absolutePath}")
            return null
        }
        return folder
    }
}
