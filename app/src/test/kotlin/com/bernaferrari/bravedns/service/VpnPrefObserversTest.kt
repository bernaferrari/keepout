package com.bernaferrari.bravedns.service

import android.app.Application
import com.bernaferrari.bravedns.database.AppInfo
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class VpnPrefObserversTest {
    @Test
    fun tombstonedExclusionDoesNotRestartTheTunnel() = runTest {
        val excluded = AppInfo(packageName = "test.excluded", firewallStatus = FirewallManager.FirewallStatus.EXCLUDE.id)
        val restarts = mutableListOf<String>()
        val observers = VpnPrefObservers(
            context = mockk(), persistentState = mockk(), appConfig = mockk(), scope = this,
            adapter = { null }, excludedApps = { mutableSetOf() }, requestRestart = { restarts.add(it) },
            dnsTransport = mockk(), notifyForeground = {}, registerAccessibility = {}, unregisterAccessibility = {},
            notifyConnectionMonitor = {}, onTunPolicyChanged = {}, currentMtu = { 0 }, handleBubbleIfNeeded = {},
            newTunnelOptions = { error("No restart requested through tunnel options") }, onOrbotStatus = {}, logd = {},
        )
        observers.handleAppInfoChanges(listOf(excluded.copy(tombstoneTs = 1)))
        assertEquals(0, restarts.size)
        observers.handleAppInfoChanges(listOf(excluded))
        assertEquals(1, restarts.size)
    }
}
