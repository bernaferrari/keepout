/* Copyright 2026 RethinkDNS and its authors */
package com.bernaferrari.bravedns.ui.compose.firewall

import androidx.compose.runtime.Immutable

/** What tapping a rule can do to it: switch it between Block and Trust, or delete it. */
@Immutable
sealed interface RethinkRuleEdit {
    /** [action] is [RethinkRuleAction.Block] or [RethinkRuleAction.Trust]. */
    data class SetAction(val action: RethinkRuleAction) : RethinkRuleEdit

    data object Delete : RethinkRuleEdit
}
