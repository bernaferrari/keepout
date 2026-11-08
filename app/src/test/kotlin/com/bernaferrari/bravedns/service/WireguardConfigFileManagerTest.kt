package com.bernaferrari.bravedns.service

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WireguardConfigFileManagerTest {
    @get:Rule val temporary = TemporaryFolder()
    @After fun cleanup() = unmockkAll()

    @Test fun failedCommitKeepsWorkingConfigAndRemovesStagingFile() {
        val target = temporary.newFile("wg1.conf").apply { writeText("working") }
        mockkStatic(Os::class)
        every { Os.rename(any(), any()) } answers {
            assertEquals("working", target.readText())
            throw ErrnoException("rename", OsConstants.EIO)
        }
        try {
            WireguardConfigFileManager.write(target, "replacement")
            fail("Expected failed commit")
        } catch (_: IOException) {
            assertEquals("working", target.readText())
            assertEquals(listOf("wg1.conf"), temporary.root.list()?.toList())
        }
    }
}
