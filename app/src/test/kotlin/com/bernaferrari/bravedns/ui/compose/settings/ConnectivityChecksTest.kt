package com.bernaferrari.bravedns.ui.compose.settings

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectivityChecksTest {
    @Test fun eachTargetGetsItsOwnResult() = runTest {
        val targets = listOf("1.1.1.1:53", "8.8.8.8:53", "broken.invalid:443")
        val probed = mutableListOf<String>()
        val results = checkConnectivity(targets) { target ->
            probed += target
            target == targets[0]
        }
        assertEquals(targets, probed)
        assertEquals(listOf(true, false, false), results)
        assertEquals(1, results.count { it })
        assertEquals(0, checkConnectivity(targets) { false }.count { it })
    }
}
