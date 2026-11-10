package com.bernaferrari.bravedns.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import com.bernaferrari.bravedns.database.LogDatabase
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.util.BioMetricType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** The result of an opt-in traffic history lookup. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TrafficHistory(
    /** Whether the user allows access to traffic history. */
    val available: Boolean,
    /** Recent destination names or addresses with a firewall rule ID for blocked entries. */
    val entries: List<String>,
)

/** Returns up to 20 recent traffic destinations or reasons for blocked traffic. */
internal class TrafficHistoryLookup : KoinComponent {
    private val preferences by inject<PersistentState>()
    private val logs by inject<LogDatabase>()

    suspend fun recent(packageName: String, blockedOnly: Boolean): TrafficHistory {
        if (!canRead()) return TrafficHistory(false, emptyList())
        val entries = logs.connectionTrackerDAO().recentAppTraffic(packageName.trim(), blockedOnly)
        if (!canRead()) return TrafficHistory(false, emptyList())
        return TrafficHistory(true, entries)
    }

    private fun canRead() = preferences.appFunctionTrafficEnabled && preferences.logsEnabled &&
        !BioMetricType.fromValue(preferences.biometricAuthType).enabled()
}
