/*
 * Copyright 2024 RethinkDNS and its authors
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
package com.bernaferrari.bravedns.ui.compose.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.ui.icons.MaterialSymbols

/** The four tabs of the navigation bar/rail, in order, with their labels and icons. */
enum class HomeDestination(
    val root: RethinkRootDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        RethinkRootDestination.Home,
        R.string.txt_home,
        MaterialSymbols.Filled.Home,
        MaterialSymbols.Outlined.Home,
    ),
    APPS(
        RethinkRootDestination.Apps,
        R.string.nav_apps,
        MaterialSymbols.Filled.Apps,
        MaterialSymbols.Outlined.Apps,
    ),
    ACTIVITY(
        RethinkRootDestination.Activity,
        R.string.nav_activity,
        MaterialSymbols.Filled.Monitoring,
        MaterialSymbols.Outlined.Monitoring,
    ),
    CONFIGURE(
        RethinkRootDestination.Configure,
        R.string.title_settings,
        MaterialSymbols.Filled.Settings,
        MaterialSymbols.Outlined.Settings,
    );

    val route: RethinkRoute get() = root.route
}
