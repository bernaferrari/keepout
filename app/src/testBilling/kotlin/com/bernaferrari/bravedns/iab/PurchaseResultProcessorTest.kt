package com.bernaferrari.bravedns.iab

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient.ProductType
import com.bernaferrari.bravedns.rpnproxy.SubscriptionStateMachineV2
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class PurchaseResultProcessorTest {
    @Test
    fun emptySubscriptionQueriesRetryTwiceAndUnsolicitedCallbacksDoNotRetry() = runTest {
        val machine = SubscriptionStateMachineV2(mockk(), mockk(), backgroundScope)
        var retries = 0
        val processor = PurchaseResultProcessor(
            scope = this, subscriptionStateMachine = machine, storeProductDetails = CopyOnWriteArrayList(),
            transactionErrorLiveData = MutableLiveData(), oneTimePurchaseCompletedFlow = MutableSharedFlow(),
            billingClient = {
                retries++
                null
            },
            isBillingClientSetup = { false }, detailFactory = mockk(),
            subsProcessor = { null }, oneTimeProcessor = { null }, queryEntitlementFromServer = { _, _, detail -> detail },
            getObfuscatedAccountId = { "" }, getObfuscatedDeviceId = { "" }, resolveDeviceId = { null },
            registerDevice = { _, _ -> }, reconcileDidForCid = { error("No device reconciliation expected") },
            handleUnauthorized401 = { _, _, _ -> }, handleConflict409 = { _, _, _, _, _, _ -> false to "" },
        )
        repeat(3) { processor.handlePurchase(null) }
        assertEquals(0, retries)

        repeat(2) { processor.handlePurchase(emptyList(), ProductType.SUBS) }
        assertEquals(2, retries)

        processor.handlePurchase(emptyList(), ProductType.SUBS)
        assertEquals(2, retries)
    }
}
