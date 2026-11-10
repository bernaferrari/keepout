package com.bernaferrari.bravedns.ui.compose.wireguard

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.service.WireguardManager
import com.bernaferrari.bravedns.ui.compose.common.findActivity
import com.bernaferrari.bravedns.ui.compose.common.writeTextDocument
import com.bernaferrari.bravedns.ui.compose.theme.RethinkConfirmDialog
import com.bernaferrari.bravedns.util.BioMetricType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun WgExportAction(configId: Int, preferences: PersistentState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.wg_export_title)
    val failed = stringResource(R.string.document_export_failed)
    var confirm by rememberSaveable { mutableStateOf(false) }
    var authorized by rememberSaveable(configId) { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val canWrite = authorized
        authorized = false
        if (uri != null && canWrite) scope.launch {
            val written = withContext(Dispatchers.IO) {
                val config = WireguardManager.getConfigById(configId) ?: return@withContext false
                context.contentResolver.writeTextDocument(uri, config.toWgQuickString())
            }
            if (!written) Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
        }
    }
    val export = {
        authorized = true
        picker.launch("wireguard-$configId.conf")
    }
    TextButton(onClick = { confirm = true }) { Text(title) }
    if (confirm) RethinkConfirmDialog(
        onDismissRequest = { confirm = false },
        title = title,
        message = stringResource(R.string.wg_export_description),
        confirmText = stringResource(R.string.document_export_action),
        dismissText = stringResource(R.string.lbl_cancel),
        onConfirm = {
            confirm = false
            if (BioMetricType.fromValue(preferences.biometricAuthType).enabled()) {
                val activity = context.findActivity<FragmentActivity>()
                if (activity == null) Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                else authenticateExport(activity, title, export)
            } else export()
        },
    )
}

private fun authenticateExport(activity: FragmentActivity, title: String, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        activity, ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build(),
    )
}
