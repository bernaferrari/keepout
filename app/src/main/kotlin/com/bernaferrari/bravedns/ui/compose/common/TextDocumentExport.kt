package com.bernaferrari.bravedns.ui.compose.common

import Logger
import android.content.ContentResolver
import android.net.Uri
import java.io.IOException

internal fun ContentResolver.writeTextDocument(uri: Uri, text: String): Boolean = try {
    openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) } != null
} catch (error: IOException) {
    Logger.e(Logger.LOG_TAG_UI, "Could not write exported document", error)
    false
} catch (error: SecurityException) {
    Logger.e(Logger.LOG_TAG_UI, "Could not access export destination", error)
    false
}
