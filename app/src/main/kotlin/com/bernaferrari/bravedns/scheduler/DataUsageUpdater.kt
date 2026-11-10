/*
 * Copyright 2023 RethinkDNS and its authors
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

import Logger
import android.content.Context
import androidx.sqlite.SQLiteException
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.bravedns.database.AppDatabase
import com.bernaferrari.bravedns.database.ConnectionTrackerRepository
import com.bernaferrari.bravedns.database.RethinkLogRepository
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.util.Constants
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DataUsageUpdater(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams), KoinComponent {
    private val connTrackRepository by inject<ConnectionTrackerRepository>()
    private val rethinkDb by inject<RethinkLogRepository>()
    private val database by inject<AppDatabase>()
    private val preferences by inject<PersistentState>()

    override suspend fun doWork(): Result {
        return try {
            val apps = database.appInfoDAO()
            val checkpoint = apps.dataUsageCheckpoint()
            val previous = checkpoint ?: preferences.previousDataUsageWatermark
            val through = System.currentTimeMillis()
            if (through <= previous) return Result.success()
            val usage = connTrackRepository.getDataUsage(previous, through).orEmpty()
                .filter { it.uid != Constants.INVALID_UID }.toMutableList()
            rethinkDb.getDataUsage(previous, through)?.let { local ->
                usage.add(local.copy(uid = applicationContext.applicationInfo.uid))
            }
            if (apps.commitDataUsage(checkpoint, through, usage)) Result.success() else Result.retry()
        } catch (error: SQLiteException) {
            Logger.e(Logger.LOG_TAG_SCHEDULER, "Could not commit data usage", error)
            Result.retry()
        }
    }
}
