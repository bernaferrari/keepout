package com.bernaferrari.bravedns.backup

import Logger
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

internal fun unzipBounded(input: InputStream?, path: String, maxEntries: Int, maxBytes: Long): Boolean {
    if (input == null) return false
    return try {
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            extractEntries(zip, File(path).canonicalFile, maxEntries, maxBytes)
        }
    } catch (error: IOException) {
        Logger.e(Logger.LOG_TAG_BACKUP_RESTORE, "Could not extract backup", error)
        false
    } catch (error: SecurityException) {
        Logger.e(Logger.LOG_TAG_BACKUP_RESTORE, "Cannot access backup files", error)
        false
    }
}

private fun extractEntries(zip: ZipInputStream, root: File, maxEntries: Int, maxBytes: Long): Boolean {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    val seen = mutableSetOf<File>()
    var remaining = maxBytes
    while (true) {
        val entry = zip.nextEntry ?: return true
        val out = BackupHelper.sanitizedZipEntryFile(root, entry.name) ?: return false
        if (seen.size >= maxEntries || !seen.add(out)) return false
        if (entry.isDirectory) {
            if (!ensureDirectory(out) || zip.read() != -1) return false
        } else {
            remaining -= extractFile(zip, out, buffer, remaining) ?: return false
        }
        zip.closeEntry()
    }
}

private fun ensureDirectory(dir: File): Boolean = dir.isDirectory || dir.mkdirs()

private fun extractFile(zip: ZipInputStream, out: File, buffer: ByteArray, limit: Long): Long? {
    val parent = out.parentFile ?: return null
    if (!ensureDirectory(parent)) return null
    var written = 0L
    out.outputStream().use { output ->
        while (true) {
            val size = zip.read(buffer)
            if (size < 0) break
            written += size
            if (written > limit) return null
            output.write(buffer, 0, size)
        }
    }
    return written
}
