package com.bernaferrari.bravedns.database

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.bravedns.data.DataUsage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class DataUsageCheckpointTest {
    @Test
    fun retryingAnAlreadyCommittedWindowDoesNotAddUsageAgain() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>()) {
            AppDatabaseConstructor.initialize()
        }.setDriver(AndroidSQLiteDriver()).build()
        try {
            val apps = db.appInfoDAO()
            apps.insert(AppInfo(packageName = "test.app", uid = 1234))
            val bytes = listOf(DataUsage(1234, 10, 20))
            assertTrue(apps.commitDataUsage(null, 100, bytes))
            assertFalse(apps.commitDataUsage(null, 100, bytes))
            assertTrue(apps.commitDataUsage(100, 200, bytes))
            assertEquals(200L, apps.dataUsageCheckpoint())
            assertEquals(DataUsage(1234, 20, 40), apps.getDataUsageByUid(1234))
        } finally {
            db.close()
        }
    }
}
