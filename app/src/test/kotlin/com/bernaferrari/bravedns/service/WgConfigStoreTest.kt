package com.bernaferrari.bravedns.service

import android.content.Context
import android.system.Os
import com.bernaferrari.bravedns.database.WgConfigFiles
import com.bernaferrari.bravedns.database.WgConfigFilesRepository
import com.bernaferrari.bravedns.wireguard.Config as WgConfig
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WgConfigStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val db = mockk<WgConfigFilesRepository>()
    private lateinit var store: WgConfigStore
    private lateinit var previous: WgConfigFiles
    private lateinit var original: File
    private val config = mockk<WgConfig> {
        every { getId() } returns 7
        every { getName() } returns "Edited"
        every { toWgQuickString() } returns "replacement"
    }

    @Before fun setup() {
        val context = mockk<Context> { every { filesDir } returns temporary.root }
        store = WgConfigStore(context, db)
        original = File(store.configDirPath(), "wg7.conf").apply { parentFile!!.mkdirs(); writeText("working") }
        previous = WgConfigFiles(7, "Original", original.path, "server metadata", true, false,
            oneWireGuard = false, useOnlyOnMetered = false)
        coEvery { db.isConfigAdded(7) } returns previous
        mockkStatic(Os::class)
        every { Os.rename(any(), any()) } answers {
            Files.move(File(firstArg<String>()).toPath(), File(secondArg<String>()).toPath(), REPLACE_EXISTING)
            Unit
        }
    }
    @After fun cleanup() = unmockkAll()

    @Test fun failedDatabaseUpdateLeavesOriginalFileAndMetadataUntouched() = runTest {
        coEvery { db.updateConfiguration(any(), any(), any(), any()) } throws androidx.sqlite.SQLiteException("disk full")
        try { store.persist(config); fail("Expected database failure") }
        catch (_: androidx.sqlite.SQLiteException) {
            assertEquals("working", original.readText())
            assertEquals("Original", previous.name)
            assertEquals(original.path, previous.configPath)
            assertEquals(listOf(original.name), original.parentFile!!.list()!!.toList())
        }
    }

    @Test fun databaseCommitSwitchesToCompleteCandidateBeforeOldFileIsRemoved() = runTest {
        coEvery { db.updateConfiguration(any(), any(), any(), any()) } coAnswers {
            val path = thirdArg<String>()
            assertEquals("working", original.readText())
            assertNotEquals(original.path, path)
            assertEquals("replacement", File(path).readText())
            assertEquals("server metadata", arg<String>(3))
            1
        }
        val saved = store.persist(config)
        assertEquals("replacement", File(saved.path).readText())
        assertFalse(original.exists())
        assertEquals(saved.path, saved.existing!!.configPath)
    }

    @Test fun failedInsertDoesNotLeaveAnUnreferencedConfig() = runTest {
        coEvery { db.isConfigAdded(7) } returns null
        coEvery { db.insert(any()) } throws androidx.sqlite.SQLiteException("disk full")
        try { store.persist(config); fail("Expected database failure") }
        catch (_: androidx.sqlite.SQLiteException) {
            assertEquals(listOf(original.name), original.parentFile!!.list()!!.toList())
        }
    }

    @Test fun restartCleanupKeepsDatabaseReferencedFileAndRemovesAbandonedCandidate() = runTest {
        val orphan = File(original.parentFile, "wg7-abcdef.conf").apply { writeText("uncommitted") }
        coEvery { db.getWgConfigs() } returns listOf(previous)
        store.removeOrphanedFiles()
        assertEquals("working", original.readText())
        assertFalse(orphan.exists())
    }
}
