package com.bernaferrari.bravedns.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.LogDatabase
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.ui.HomeScreenActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class WeeklyTrafficDigest(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    private val preferences by inject<PersistentState>()
    private val logs by inject<LogDatabase>()

    override suspend fun doWork(): Result {
        if (!preferences.weeklyDigestEnabled || !preferences.logsEnabled) return Result.success()
        val context = applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return Result.success()

        val today = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        val totals = logs.dailyStatsDAO().weeklyConnectionTotals(today - DAYS, today)
        if (totals.allowed + totals.blocked == 0L || !preferences.weeklyDigestEnabled) return Result.success()

        val title = context.getString(R.string.weekly_digest_title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, title, NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_ID, Intent(context, HomeScreenActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.weekly_digest_body, totals.allowed, totals.blocked))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "weekly_traffic_digest"
        private const val NOTIFICATION_ID = 8411
        private const val DAYS = 7L

        fun updateSchedule(context: Context, enabled: Boolean) {
            val work = WorkManager.getInstance(context)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<WeeklyTrafficDigest>(DAYS, TimeUnit.DAYS)
                    .setInitialDelay(DAYS, TimeUnit.DAYS)
                    .build()
                work.enqueueUniquePeriodicWork(CHANNEL, ExistingPeriodicWorkPolicy.KEEP, request)
            } else {
                work.cancelUniqueWork(CHANNEL)
                context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            }
        }
    }
}
