package com.bernaferrari.bravedns.scheduler

import android.app.Application
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class CapturedCrashReportingTest {
    @Test
    fun `captured crash is available in a manual bug report`() {
        val context = RuntimeEnvironment.getApplication()
        val crash = "java.lang.IllegalStateException: failed\n\tat example.Work.run(Work.kt:42)"
        EnhancedBugReport.writeLogsToFile(context, crash)

        val folder = File(context.filesDir, EnhancedBugReport.TOMBSTONE_DIR_NAME)
        val log = requireNotNull(folder.listFiles()).single { it.name.startsWith(EnhancedBugReport.PREFIX_KOTLIN) }
        assertTrue(log.readText().contains(crash))
    }
}
