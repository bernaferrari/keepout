/*
 * Copyright 2025 RethinkDNS and its authors
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
package com.bernaferrari.bravedns.util

import Logger
import Logger.LOG_TAG_APP
import android.content.Context
import com.bernaferrari.bravedns.scheduler.EnhancedBugReport
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

/**
 * Saves uncaught exceptions for manual bug reports and delegates to Android.
 */
class GlobalExceptionHandler private constructor(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    contextRef: Context?
) : Thread.UncaughtExceptionHandler {

    private val contextRef: WeakReference<Context>? = contextRef?.let { WeakReference(it) }
    companion object {
        private var instance: GlobalExceptionHandler? = null

        /**
         * Initialize the global exception handler
         * This should be called early in the application lifecycle
         */
        fun initialize(ctx: Context) {
            if (instance != null) {
                Logger.w(LOG_TAG_APP, "err-handler already initialized")
                return
            }

            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            instance = GlobalExceptionHandler(defaultHandler, ctx.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(instance)

            Logger.i(LOG_TAG_APP, "err-handler initialized successfully")
        }

        /**
         * Get the current instance of the global exception handler
         */
        fun getInstance(): GlobalExceptionHandler? = instance
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val exception = Logger.throwableToException(throwable)
            Logger.e(LOG_TAG_APP, "uncaught exception in thread ${thread.name}", exception)

            logExceptionContext(thread, exception)

        } catch (e: Exception) {
            Logger.e(LOG_TAG_APP, "err while handling uncaught exception", e)
        } finally {
            // Call the default handler to ensure proper app termination
            defaultHandler?.uncaughtException(thread, throwable) ?: run {
                // If no default handler, terminate the process
                Logger.e(LOG_TAG_APP, "no default exception handler found, terminating process")
                exitProcess(1)
            }
        }
    }

    /**
     * Log additional context information about the exception
     */
    private fun logExceptionContext(thread: Thread, exception: Throwable) {
        try {
            val stackTrace = exception.stackTraceToString()
            @Suppress("DEPRECATION")
            val threadInfo = "Thread: ${thread.name} (ID: ${thread.id}, State: ${thread.state})"

            val stringBuilder = StringBuilder()
            stringBuilder.appendLine("---Uncaught Exception ${thread.name}---")
            stringBuilder.appendLine("Exception Type: ${exception.javaClass.name}")
            stringBuilder.appendLine("Exception Message: ${exception.message}")
            stringBuilder.appendLine(threadInfo)
            stringBuilder.appendLine("Stack Trace:")
            stringBuilder.appendLine(stackTrace)
            stringBuilder.appendLine("--------------------------------------------")
            val msg = stringBuilder.toString()

            // Log cause chain if available
            var cause = exception.cause
            var causeLevel = 1
            while (cause != null && causeLevel <= 5) { // Limit to 5 levels to avoid infinite loops
                Logger.e(LOG_TAG_APP, "caused by (level $causeLevel): ${cause.javaClass.name}: ${cause.message}")
                cause = cause.cause
                causeLevel++
            }
            val ex = Logger.throwableToException(exception)
            Logger.crash(LOG_TAG_APP, msg, ex)

            // Try to write logs to file with context
            writeLogsToFileWithFallback(msg)
        } catch (e: Exception) {
            Logger.e(LOG_TAG_APP, "err while logging exception context", e)
        }
    }

    private fun writeLogsToFileWithFallback(message: String) {
        val context = contextRef?.get()
        if (context != null) {
            EnhancedBugReport.writeLogsToFile(context, message)
        } else {
            System.err.println(message)
        }
    }
}
