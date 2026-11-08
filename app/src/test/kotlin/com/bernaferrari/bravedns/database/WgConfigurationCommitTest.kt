package com.bernaferrari.bravedns.database

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class WgConfigurationCommitTest {
    @Test fun committingFilePathPreservesFlagsChangedSinceTheEditStarted() = runTest {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>()) {
            AppDatabaseConstructor.initialize()
        }.setDriver(AndroidSQLiteDriver()).build()
        try {
            val dao = database.wgConfigFilesDAO()
            dao.insert(WgConfigFiles(7, "Original", "old.conf", "metadata", false, false,
                oneWireGuard = false, useOnlyOnMetered = false))
            // These settings can change while a config candidate is being written.
            dao.updateLockdownConfig(7, true)
            dao.updateSsidEnabled(7, true)
            dao.updateSsids(7, "Office")
            val repository = WgConfigFilesRepository(dao)
            assertEquals(1, repository.updateConfiguration(7, "Edited", "new.conf", "metadata"))
            val committed = dao.isConfigAdded(7)!!
            assertEquals("Edited", committed.name)
            assertEquals("new.conf", committed.configPath)
            assertTrue(committed.isLockdown)
            assertTrue(committed.ssidEnabled)
            assertEquals("Office", committed.ssids)
            assertEquals(0, repository.updateConfiguration(8, "Missing", "unused.conf", ""))
        } finally { database.close() }
    }
}
