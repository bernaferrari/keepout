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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.bravedns.R
import com.bernaferrari.bravedns.service.PersistentState
import com.bernaferrari.bravedns.ui.compose.rpn.RpnAccountScreen
import com.bernaferrari.bravedns.ui.compose.rpn.RpnAvailabilityScreen
import com.bernaferrari.bravedns.ui.compose.rpn.RpnCountriesScreen
import com.bernaferrari.bravedns.ui.compose.rpn.RpnServerSettingsScreen
import com.bernaferrari.bravedns.ui.compose.rpn.RpnWinProxyDetailsScreen
import com.bernaferrari.bravedns.ui.compose.settings.CheckoutScreen
import com.bernaferrari.bravedns.ui.compose.theme.CompactEmptyState
import com.bernaferrari.bravedns.ui.compose.theme.RethinkTopBarLazyColumnScreen
import com.bernaferrari.bravedns.viewmodel.CheckoutViewModel

/** The Rethink Proxy Network: countries, server settings, the account and checkout. */
internal fun EntryProviderScope<NavKey>.rpnEntries(
    nav: RethinkNavOps,
    persistentState: PersistentState,
    rpn: HomeRpnDependencies,
    onNavigateToProxy: () -> Unit,
) {
    entry<RethinkRoute.RpnCountries> {
        RpnCountriesScreen(
            onBackClick = { nav.popBackStack() },
            onServerDetails = { nav.push(RethinkRoute.CountryProxy(it)) },
            onServerSettings = { nav.push(RethinkRoute.RpnSettings) },
        )
    }
    entry<RethinkRoute.RpnSettings> {
        RpnServerSettingsScreen(persistentState = persistentState, onBackClick = { nav.popBackStack() })
    }
    entry<RethinkRoute.RpnAvailability> {
        RpnAvailabilityScreen(onBackClick = { nav.popBackStack() })
    }
    entry<RethinkRoute.CountryProxy> { args ->
        RpnWinProxyDetailsScreen(countryCode = args.countryCode, onBackClick = { nav.popBackStack() })
    }
    entry<RethinkRoute.RpnServerDetails> { args ->
        RpnWinProxyDetailsScreen(countryCode = args.key, onBackClick = { nav.popBackStack() })
    }
    entry<RethinkRoute.Checkout> {
        val checkoutViewModel = rpn.checkoutViewModel
        if (checkoutViewModel == null) {
            CheckoutUnavailable(onBackClick = { nav.popBackStack() })
        } else {
            CheckoutEntry(nav, checkoutViewModel, onNavigateToProxy)
        }
    }
    entry<RethinkRoute.RpnAccount> {
        RpnAccountScreen(
            manageViewModel = rpn.managePurchaseViewModel,
            historyViewModel = rpn.purchaseHistoryViewModel,
            ordersViewModel = rpn.serverOrderHistoryViewModel,
            onBackClick = { nav.popBackStack() },
            onViewPlans = viewPlansAction(canCheckout = rpn.checkoutViewModel != null, nav = nav),
        )
    }
}

/** Checkout follows the payment worker: its progress drives the screen's status. */
@Composable
private fun CheckoutEntry(
    nav: RethinkNavOps,
    checkoutViewModel: CheckoutViewModel,
    onNavigateToProxy: () -> Unit,
) {
    val paymentStatus by checkoutViewModel.paymentStatus.collectAsStateWithLifecycle()
    val workInfoList by checkoutViewModel.paymentWorkInfo.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(workInfoList) {
        checkoutViewModel.updatePaymentStatusFromWorkInfo(workInfoList)
    }

    CheckoutScreen(
        paymentStatus = paymentStatus,
        onStartPayment = { checkoutViewModel.startPayment() },
        onNavigateToProxy = onNavigateToProxy,
        onManageAccount = { nav.push(RethinkRoute.RpnAccount) },
        onBackClick = { nav.popBackStack() }
    )
}

/** Builds without a store have no checkout; say so on a real screen with a way back. */
@Composable
private fun CheckoutUnavailable(onBackClick: () -> Unit) {
    RethinkTopBarLazyColumnScreen(
        title = stringResource(R.string.app_name),
        onBackClick = onBackClick,
    ) {
        item { CompactEmptyState(message = stringResource(R.string.checkout_unavailable_desc)) }
    }
}

/** Only offer plans where this channel can actually sell them. */
private fun viewPlansAction(canCheckout: Boolean, nav: RethinkNavOps): (() -> Unit)? =
    if (canCheckout) {
        { nav.push(RethinkRoute.Checkout) }
    } else {
        null
    }
