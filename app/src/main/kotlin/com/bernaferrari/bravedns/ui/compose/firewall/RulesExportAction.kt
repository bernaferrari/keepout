package com.bernaferrari.bravedns.ui.compose.firewall

import Logger
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.sqlite.SQLiteException
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.database.AppDatabase
import com.bernaferrari.bravedns.rules.exportBlockRules
import com.bernaferrari.bravedns.ui.compose.common.writeTextDocument
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
internal fun RulesExportAction(uid: Int, tab: RethinkRulesTab) {
    val context = LocalContext.current
    val database = koinInject<AppDatabase>()
    val scope = rememberCoroutineScope()
    val failed = stringResource(R.string.document_export_failed)
    var pendingUid by rememberSaveable { mutableIntStateOf(uid) }
    var pendingTab by rememberSaveable { mutableStateOf(tab.name) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val uidToExport = pendingUid
        val tabToExport = RethinkRulesTab.valueOf(pendingTab)
        if (uri != null) scope.launch {
            val written = withContext(Dispatchers.IO) {
                try {
                    val text = exportBlockRules(database, uidToExport, tabToExport)
                    context.contentResolver.writeTextDocument(uri, text)
                } catch (error: SQLiteException) {
                    Logger.e(Logger.LOG_TAG_UI, "Could not read rules for export", error)
                    false
                }
            }
            if (!written) Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
        }
    }
    IconButton(onClick = {
        pendingUid = uid
        pendingTab = tab.name
        val filename = if (tab == RethinkRulesTab.DOMAIN) "blocked-domains.txt" else "blocked-addresses.txt"
        picker.launch(filename)
    }) {
        Icon(MaterialSymbols.Filled.Download, stringResource(R.string.rules_export_blocks))
    }
}
