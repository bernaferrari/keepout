package com.bernaferrari.bravedns.ui.compose.settings

/** Probe each target independently, preserving display order and allowing a zero score. */
internal suspend fun checkConnectivity(targets: List<String>, probe: suspend (String) -> Boolean): List<Boolean> =
    targets.map { probe(it) }
