package com.bernaferrari.bravedns.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.geometry.Size
import com.bernaferrari.bravedns.ui.compose.settings.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BackupRestoreBusyTest {
    @Test fun runningOperationShowsProgressAndDisablesConflictingActions() = runSkikoComposeUiTest(size = Size(600f, 900f)) {
        val busy = mutableStateOf(true)
        val copy = RethinkBackupRestoreDialogCopy("Confirm", "Details", "Continue", "Cancel")
        setContent {
            MaterialTheme {
                RethinkBackupRestoreSheet("v0", RethinkBackupRestoreStrings(
                    "Backup and restore", "Manage data", "Back up", "Save data", "Restore", "Replace data",
                    copy, copy, copy, copy), null, {}, {}, {}, busy = busy.value, progressText = "Restoring data")
            }
        }
        onNodeWithText("Restoring data").assertExists()
        onNodeWithText("Back up").assertIsNotEnabled()
        onNodeWithText("Restore").assertIsNotEnabled()
        runOnIdle { busy.value = false }
        onNodeWithText("Restoring data").assertDoesNotExist()
        onNodeWithText("Back up").assertIsEnabled()
        onNodeWithText("Restore").assertIsEnabled()
    }
}
