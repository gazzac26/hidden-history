package com.hiddenhistory.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hiddenhistory.billing.VehicleReportBillingManager
import com.hiddenhistory.billing.VehicleReportPurchaseState
import kotlinx.coroutines.flow.StateFlow

class VehicleReportPurchaseViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val billingManager =
        VehicleReportBillingManager(application)

    val purchaseState: StateFlow<VehicleReportPurchaseState> =
        billingManager.purchaseState

    fun purchaseReport(
        activity: Activity
    ) {
        billingManager.launchPurchase(activity)
    }

    fun consumePurchase(
        purchaseToken: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        billingManager.consumePurchase(
            purchaseToken = purchaseToken,
            onComplete = onComplete
        )
    }

    fun refreshProduct() {
        billingManager.refreshProduct()
    }

    override fun onCleared() {
        billingManager.release()
        super.onCleared()
    }
}