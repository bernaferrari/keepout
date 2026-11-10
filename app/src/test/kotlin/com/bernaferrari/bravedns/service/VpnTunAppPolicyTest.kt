package com.bernaferrari.bravedns.service

import android.app.Application
import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.bravedns.data.AppConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class VpnTunAppPolicyTest {
    @Test
    fun lockdownKeepsConfiguredExcludedAppsOnTheTunnel() = runBlocking {
        val service = Robolectric.buildService(VpnService::class.java).get()
        val config = mockk<AppConfig>(relaxed = true)
        every { config.determineFirewallMode() } returns AppConfig.TunFirewallMode.NONE
        val preferences = mockk<PersistentState>(relaxed = true)
        every { preferences.routeRethinkInRethink } returns true
        val ownPackage = ApplicationProvider.getApplicationContext<Application>().packageName
        val policy = VpnTunAppPolicy(service, preferences, config)
        val locked = policy.newBuilder(null, vpnLockdown = true, isPaused = false, setOf(ownPackage))
        assertNull(disallowedApps(locked))
    }

    private fun disallowedApps(builder: VpnService.Builder): List<String>? {
        val config = ReflectionHelpers.getField<Any>(builder, "mConfig")
        return ReflectionHelpers.getField(config, "disallowedApplications")
    }
}
