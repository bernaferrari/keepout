/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.firewall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.bernaferrari.bravedns.service.FirewallManager
import com.bernaferrari.bravedns.util.Constants.Companion.UID_EVERYBODY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The app names behind rule owners' uids, each loaded once, when its first row shows. */
@Stable
internal class RuleOwnerNames(private val scope: CoroutineScope) {
    val names = mutableStateMapOf<Int, String>()

    fun load(uid: Int) {
        if (uid == UID_EVERYBODY || names.containsKey(uid)) return
        scope.launch { names[uid] = appNameOf(uid) }
    }
}

@Composable
internal fun rememberRuleOwnerNames(): RuleOwnerNames {
    val scope = rememberCoroutineScope()
    return remember { RuleOwnerNames(scope) }
}

/** [uid]'s app name when [load] is set (blank until loaded, or when not loading). */
@Composable
internal fun rememberAppName(uid: Int, load: Boolean): String {
    var name by remember(uid) { mutableStateOf("") }
    LaunchedEffect(uid) {
        if (load) name = appNameOf(uid)
    }
    return name
}

private suspend fun appNameOf(uid: Int): String = withContext(Dispatchers.IO) {
    FirewallManager.ensureLoaded()
    FirewallManager.getAppNameByUid(uid).orEmpty()
}
