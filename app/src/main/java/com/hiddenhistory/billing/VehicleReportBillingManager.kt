package com.hiddenhistory.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VehicleReportBillingManager(
    context: Context
) {

    private val applicationContext = context.applicationContext

    private val _purchaseState =
        MutableStateFlow<VehicleReportPurchaseState>(
            VehicleReportPurchaseState.Idle
        )

    val purchaseState: StateFlow<VehicleReportPurchaseState> =
        _purchaseState.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient =
        BillingClient.newBuilder(applicationContext)
            .setListener { billingResult, purchases ->
                handlePurchaseResult(
                    billingResult = billingResult,
                    purchases = purchases
                )
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

    init {
        connect()
    }

    private fun connect() {

        if (billingClient.isReady) {
            queryProduct()
            return
        }

        billingClient.startConnection(
            object : BillingClientStateListener {

                override fun onBillingSetupFinished(
                    billingResult: BillingResult
                ) {
                    if (
                        billingResult.responseCode ==
                        BillingClient.BillingResponseCode.OK
                    ) {
                        queryProduct()
                    } else {
                        _purchaseState.value =
                            VehicleReportPurchaseState.Error(
                                billingResult.debugMessage.ifBlank {
                                    "Unable to connect to Google Play Billing."
                                }
                            )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    /*
                     * Automatic reconnection is enabled on the BillingClient.
                     *
                     * Google Play Billing will attempt to reconnect when
                     * another billing request is made.
                     */
                    _purchaseState.value =
                        VehicleReportPurchaseState.Error(
                            "Google Play Billing disconnected."
                        )
                }
            }
        )
    }

    private fun queryProduct() {

        _purchaseState.value =
            VehicleReportPurchaseState.Loading

        val product =
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(
                    VehicleReportProduct.PRODUCT_ID
                )
                .setProductType(
                    BillingClient.ProductType.INAPP
                )
                .build()

        val params =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(product)
                )
                .build()

        billingClient.queryProductDetailsAsync(
            params
        ) { billingResult, productDetailsResult ->

            if (
                billingResult.responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        billingResult.debugMessage.ifBlank {
                            "Unable to load the AI vehicle report."
                        }
                    )

                return@queryProductDetailsAsync
            }

            val details =
                productDetailsResult.productDetailsList
                    .firstOrNull {
                        it.productId ==
                            VehicleReportProduct.PRODUCT_ID
                    }

            if (details == null) {
                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "The AI vehicle report product is not available."
                    )

                return@queryProductDetailsAsync
            }

            productDetails = details

            val price =
                details
                    .oneTimePurchaseOfferDetailsList
                    ?.firstOrNull()
                    ?.formattedPrice

            if (price.isNullOrBlank()) {
                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "The AI vehicle report price could not be loaded."
                    )

                return@queryProductDetailsAsync
            }

            _purchaseState.value =
                VehicleReportPurchaseState.Available(
                    formattedPrice = price
                )
        }
    }

    fun launchPurchase(
        activity: Activity
    ) {

        val details = productDetails

        if (details == null) {
            _purchaseState.value =
                VehicleReportPurchaseState.Error(
                    "The AI vehicle report is not currently available."
                )

            return
        }

        _purchaseState.value =
            VehicleReportPurchaseState.Purchasing

        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(details)
                .build()

        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(productDetailsParams)
                )
                .build()

        val billingResult =
            billingClient.launchBillingFlow(
                activity,
                billingFlowParams
            )

        if (
            billingResult.responseCode !=
            BillingClient.BillingResponseCode.OK
        ) {
            _purchaseState.value =
                VehicleReportPurchaseState.Error(
                    billingResult.debugMessage.ifBlank {
                        "Unable to start the purchase."
                    }
                )
        }
    }

    private fun handlePurchaseResult(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {

        when (billingResult.responseCode) {

            BillingClient.BillingResponseCode.OK -> {

                if (purchases.isNullOrEmpty()) {
                    _purchaseState.value =
                        VehicleReportPurchaseState.Error(
                            "Google Play returned no purchase."
                        )

                    return
                }

                purchases.forEach { purchase ->
                    processPurchase(purchase)
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Idle
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "This report purchase is already owned."
                    )
            }

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "Google Play Billing is temporarily unavailable."
                    )
            }

            else -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        billingResult.debugMessage.ifBlank {
                            "The purchase could not be completed."
                        }
                    )
            }
        }
    }

    private fun processPurchase(
        purchase: Purchase
    ) {

        when (purchase.purchaseState) {

            Purchase.PurchaseState.PURCHASED -> {

                if (!purchase.isAcknowledged) {

                    val acknowledgeParams =
                        AcknowledgePurchaseParams
                            .newBuilder()
                            .setPurchaseToken(
                                purchase.purchaseToken
                            )
                            .build()

                    billingClient.acknowledgePurchase(
                        acknowledgeParams
                    ) { acknowledgeResult ->

                        if (
                            acknowledgeResult.responseCode ==
                            BillingClient.BillingResponseCode.OK
                        ) {
                            _purchaseState.value =
                                VehicleReportPurchaseState.Purchased(
                                    purchaseToken =
                                        purchase.purchaseToken
                                )
                        } else {
                            _purchaseState.value =
                                VehicleReportPurchaseState.Error(
                                    acknowledgeResult.debugMessage.ifBlank {
                                        "The purchase could not be confirmed."
                                    }
                                )
                        }
                    }

                } else {

                    _purchaseState.value =
                        VehicleReportPurchaseState.Purchased(
                            purchaseToken =
                                purchase.purchaseToken
                        )
                }
            }

            Purchase.PurchaseState.PENDING -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Pending
            }

            Purchase.PurchaseState.UNSPECIFIED_STATE -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "The purchase has an unknown state."
                    )
            }
        }
    }

    fun consumePurchase(
        purchaseToken: String,
        onComplete: (Boolean, String?) -> Unit
    ) {

        val consumeParams =
            ConsumeParams
                .newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

        billingClient.consumeAsync(
            consumeParams
        ) { billingResult, _ ->

            if (
                billingResult.responseCode ==
                BillingClient.BillingResponseCode.OK
            ) {
                onComplete(
                    true,
                    null
                )
            } else {
                onComplete(
                    false,
                    billingResult.debugMessage.ifBlank {
                        "The report purchase could not be consumed."
                    }
                )
            }
        }
    }

    fun refreshProduct() {
        connect()
    }

    fun release() {

        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}