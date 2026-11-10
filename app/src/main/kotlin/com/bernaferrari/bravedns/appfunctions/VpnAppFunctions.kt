/*
 * Copyright 2026 RethinkDNS and its authors
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
package com.bernaferrari.bravedns.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import android.net.VpnService
import androidx.annotation.RequiresApi
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.ui.compose.home.HomePauseDuration
import com.bernaferrari.bravedns.ui.compose.home.VpnControllerPauseControls
import com.bernaferrari.bravedns.ui.compose.home.pauseProtection
import com.bernaferrari.bravedns.util.BioMetricType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.bernaferrari.bravedns.service.VpnController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AppFunctions that expose RethinkDNS VPN and firewall controls to on-device AI agents.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(serviceName = "KeepoutAppFunctionService", appFunctionXmlFileName = "keepout_functions")
abstract class VpnAppFunctions : AppFunctionService(), KoinComponent {
    private val preferences by inject<PersistentState>()

    private fun isLocked() = BioMetricType.fromValue(preferences.biometricAuthType).enabled()

    private fun lockedResult(): VpnActionResult? =
        if (isLocked()) {
            VpnActionResult(false, "Open and unlock Keepout to change protection", null)
        } else null


    /** The current VPN and firewall protection status. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class VpnStatus(
        /** Whether the VPN tunnel is currently active. */
        val isActive: Boolean,
        /** Human-readable connection status such as Protected, Paused, or Disconnected. */
        val status: String,
        /** Whether the user has requested VPN protection. */
        val activationRequested: Boolean,
        /** The connected DNS server name, or null when not connected. */
        val serverName: String?,
        /** Whether the firewall is temporarily paused. */
        val isPaused: Boolean,
    )

    /** The result of a VPN control action. */
    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class VpnActionResult(
        /** Whether the action completed successfully. */
        val success: Boolean,
        /** A human-readable result message. */
        val message: String,
        /** The VPN status after the action, if available. */
        val status: VpnStatus?,
    )

    /**
     * Returns the current VPN and firewall protection status.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getVpnStatus(): VpnStatus =
        withContext(Dispatchers.IO) {
            toVpnStatus(VpnController.state())
        }

    /**
     * Starts VPN and DNS firewall protection.
     * The user must have granted VPN permission beforehand.
     * Call [getVpnStatus] first to check whether protection is already active.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun startVpn(): VpnActionResult =
        withContext(Dispatchers.IO) {
            lockedResult()?.let { return@withContext it }
            if (VpnController.hasTunnel()) {
                return@withContext VpnActionResult(
                    success = true,
                    message = "VPN protection is already active",
                    status = toVpnStatus(VpnController.state()),
                )
            }
            if (VpnService.prepare(applicationContext) != null) {
                return@withContext VpnActionResult(false, "Grant VPN permission in Keepout first", null)
            }
            VpnController.start(applicationContext)
            VpnActionResult(
                success = true,
                message = "VPN start requested",
                status = toVpnStatus(VpnController.state()),
            )
        }

    /**
     * Stops VPN and DNS firewall protection.
     * Confirm with the user before calling this function because it removes network protection.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun stopVpn(): VpnActionResult =
        withContext(Dispatchers.IO) {
            lockedResult()?.let { return@withContext it }
            if (!VpnController.hasTunnel()) {
                return@withContext VpnActionResult(
                    success = true,
                    message = "VPN protection is already stopped",
                    status = toVpnStatus(VpnController.state()),
                )
            }
            VpnController.stop("appfunction", applicationContext)
            VpnActionResult(
                success = true,
                message = "VPN protection stopped",
                status = toVpnStatus(VpnController.state()),
            )
        }

    /**
     * Temporarily pauses firewall protection while keeping the VPN service running.
     * Call [getVpnStatus] first to verify protection is active and not already paused.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun pauseVpn(): VpnActionResult =
        withContext(Dispatchers.IO) {
            lockedResult()?.let { return@withContext it }
            if (!VpnController.hasTunnel()) {
                return@withContext VpnActionResult(
                    success = false,
                    message = "VPN is not active; nothing to pause",
                    status = toVpnStatus(VpnController.state()),
                )
            }
            if (VpnController.isAppPaused()) {
                return@withContext VpnActionResult(
                    success = true,
                    message = "Firewall is already paused",
                    status = toVpnStatus(VpnController.state()),
                )
            }
            if (!pauseProtection(HomePauseDuration.FifteenMinutes, VpnControllerPauseControls)) {
                return@withContext VpnActionResult(false, "Could not pause protection", toVpnStatus(VpnController.state()))
            }
            VpnActionResult(
                success = true,
                message = "Firewall protection paused",
                status = toVpnStatus(VpnController.state()),
            )
        }

    /**
     * Resumes firewall protection after a pause.
     * Call [getVpnStatus] first to verify the firewall is currently paused.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun resumeVpn(): VpnActionResult =
        withContext(Dispatchers.IO) {
            lockedResult()?.let { return@withContext it }
            if (!VpnController.isAppPaused()) {
                return@withContext VpnActionResult(
                    success = true,
                    message = "Firewall is not paused",
                    status = toVpnStatus(VpnController.state()),
                )
            }
            VpnController.resumeApp()
            VpnActionResult(
                success = true,
                message = "Firewall protection resumed",
                status = toVpnStatus(VpnController.state()),
            )
        }

    /**
     * Lists recent traffic destinations for an Android package. Requires the user's traffic
     * sharing opt-in in Keepout settings.
     *
     * @param packageName The exact Android package name, such as com.android.chrome.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun recentAppTraffic(packageName: String): TrafficHistory =
        TrafficHistoryLookup().recent(packageName, blockedOnly = false)

    /**
     * Lists recent blocked destinations and firewall rule IDs for an Android package.
     * Requires the user's traffic sharing opt-in in Keepout settings.
     *
     * @param packageName The exact Android package name.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun recentAppBlocks(packageName: String): TrafficHistory =
        TrafficHistoryLookup().recent(packageName, blockedOnly = true)

    private fun toVpnStatus(state: com.bernaferrari.bravedns.service.VpnState): VpnStatus {
        return VpnStatus(
            isActive = state.on,
            status = state.statusText,
            activationRequested = state.activationRequested,
            serverName = state.serverName.takeUnless { isLocked() },
            isPaused = state.isPaused,
        )
    }
}
