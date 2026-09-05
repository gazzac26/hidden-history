package com.hiddenhistory.billing

import android.app.Activity
import android.content.Context
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

    private val applicationContext =
        context.applicationContext

    private val _purchaseState =
        MutableStateFlow<VehicleReportPurchaseState>(
            VehicleReportPurchaseState.Idle
        )

    val purchaseState:
        StateFlow<VehicleReportPurchaseState> =
        _purchaseState.asStateFlow()

    private val billingClient:
        BillingClient =
        BillingClient
            .newBuilder(
                applicationContext
            )
            .setListener { billingResult, purchases ->

                handlePurchaseResult(
                    billingResult =
                        billingResult,
                    purchases =
                        purchases
                )
            }
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

    init {
        connect()
    }

    /*
     * =========================================================
     * CONNECTION
     * =========================================================
     */

    private fun connect() {

        if (billingClient.isReady) {
            queryProduct()
            return
        }

        billingClient.startConnection(
            object :
                BillingClientStateListener {

                override fun onBillingSetupFinished(
                    billingResult: BillingResult
                ) {

                    if (
                        billingResult.responseCode ==
                        BillingClient
                            .BillingResponseCode
                            .OK
                    ) {

                        queryProduct()

                    } else {

                        _purchaseState.value =
                            VehicleReportPurchaseState.Error(
                                billingResult
                                    .debugMessage
                                    .ifBlank {
                                        "Unable to connect to Google Play Billing."
                                    }
                            )
                    }
                }

                override fun onBillingServiceDisconnected() {

                    _purchaseState.value =
                        VehicleReportPurchaseState.Error(
                            "Google Play Billing disconnected."
                        )
                }
            }
        )
    }

    /*
     * =========================================================
     * PRODUCT
     * =========================================================
     */

    private fun queryProduct() {

        _purchaseState.value =
            VehicleReportPurchaseState.Loading

        val product =
            QueryProductDetailsParams
                .Product
                .newBuilder()
                .setProductId(
                    VehicleReportProduct.PRODUCT_ID
                )
                .setProductType(
                    BillingClient
                        .ProductType
                        .INAPP
                )
                .build()

        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(
                    listOf(product)
                )
                .build()

        billingClient.queryProductDetailsAsync(
            params
        ) { billingResult,
            productDetailsResult ->

            if (
                billingResult.responseCode !=
                BillingClient
                    .BillingResponseCode
                    .OK
            ) {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        billingResult
                            .debugMessage
                            .ifBlank {
                                "Unable to load the Pro Vehicle Search."
                            }
                    )

                return@queryProductDetailsAsync
            }

            val details =
                productDetailsResult
                    .productDetailsList
                    .firstOrNull {

                        it.productId ==
                            VehicleReportProduct
                                .PRODUCT_ID
                    }

            if (details == null) {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "The Pro Vehicle Search product is not available."
                    )

                return@queryProductDetailsAsync
            }

            val price =
                details
                    .oneTimePurchaseOfferDetailsList
                    ?.firstOrNull()
                    ?.formattedPrice

            if (price.isNullOrBlank()) {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "The Pro Vehicle Search price could not be loaded."
                    )

                return@queryProductDetailsAsync
            }

            _purchaseState.value =
                VehicleReportPurchaseState.Available(
                    formattedPrice =
                        price
                )
        }
    }

    /*
     * =========================================================
     * LAUNCH PRO SEARCH PURCHASE
     * =========================================================
     */

    fun launchPurchase(
        activity: Activity,
        obfuscatedAccountId: String? = null
    ) {

        if (!billingClient.isReady) {

            _purchaseState.value =
                VehicleReportPurchaseState.Error(
                    "Google Play Billing is not ready."
                )

            connect()

            return
        }

        _purchaseState.value =
            VehicleReportPurchaseState.Purchasing

        queryProductForPurchase(
            activity =
                activity,
            obfuscatedAccountId =
                obfuscatedAccountId
        )
    }

    private fun queryProductForPurchase(
        activity: Activity,
        obfuscatedAccountId: String?
    ) {

        val product =
            QueryProductDetailsParams
                .Product
                .newBuilder()
                .setProductId(
                    VehicleReportProduct.PRODUCT_ID
                )
                .setProductType(
                    BillingClient
                        .ProductType
                        .INAPP
                )
                .build()

        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(
                    listOf(product)
                )
                .build()

        billingClient.queryProductDetailsAsync(
            params
        ) { billingResult,
            productDetailsResult ->

            if (
                billingResult.responseCode !=
                BillingClient
                    .BillingResponseCode
                    .OK
            ) {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        billingResult
                            .debugMessage
                            .ifBlank {
                                "Unable to prepare the Pro Vehicle Search purchase."
                            }
                    )

                return@queryProductDetailsAsync
            }

            val details =
                productDetailsResult
                    .productDetailsList
                    .firstOrNull {

                        it.productId ==
                            VehicleReportProduct
                                .PRODUCT_ID
                    }

            if (details == null) {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "The Pro Vehicle Search product is unavailable."
                    )

                return@queryProductDetailsAsync
            }

            launchBillingFlow(
                activity =
                    activity,
                productDetails =
                    details,
                obfuscatedAccountId =
                    obfuscatedAccountId
            )
        }
    }

    private fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        obfuscatedAccountId: String?
    ) {

        val productParams =
            BillingFlowParams
                .ProductDetailsParams
                .newBuilder()
                .setProductDetails(
                    productDetails
                )
                .build()

        val accountBuilder =
            BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(
                    listOf(productParams)
                )

        if (
            !obfuscatedAccountId.isNullOrBlank()
        ) {

            accountBuilder.setObfuscatedAccountId(
                obfuscatedAccountId
            )
        }

        val billingResult =
            billingClient.launchBillingFlow(
                activity,
                accountBuilder.build()
            )

        if (
            billingResult.responseCode !=
            BillingClient
                .BillingResponseCode
                .OK
        ) {

            _purchaseState.value =
                VehicleReportPurchaseState.Error(
                    billingResult
                        .debugMessage
                        .ifBlank {
                            "Unable to start the Pro Vehicle Search purchase."
                        }
                )
        }
    }

    /*
     * =========================================================
     * PURCHASE CALLBACK
     * =========================================================
     */

    private fun handlePurchaseResult(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {

        when (
            billingResult.responseCode
        ) {

            BillingClient
                .BillingResponseCode
                .OK -> {

                if (
                    purchases.isNullOrEmpty()
                ) {

                    _purchaseState.value =
                        VehicleReportPurchaseState.Error(
                            "Google Play returned no purchase."
                        )

                    return
                }

                purchases.forEach {
                    processPurchase(it)
                }
            }

            BillingClient
                .BillingResponseCode
                .USER_CANCELED -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Idle
            }

            BillingClient
                .BillingResponseCode
                .ITEM_ALREADY_OWNED -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "A Pro Vehicle Search purchase is already awaiting processing."
                    )
            }

            BillingClient
                .BillingResponseCode
                .SERVICE_UNAVAILABLE -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "Google Play Billing is temporarily unavailable."
                    )
            }

            else -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        billingResult
                            .debugMessage
                            .ifBlank {
                                "The Pro Vehicle Search purchase could not be completed."
                            }
                    )
            }
        }
    }

    /*
     * =========================================================
     * PURCHASE PROCESSING
     * =========================================================
     *
     * We do NOT grant the Hidden History Pro Search token here.
     *
     * The purchase token must go through the secure backend,
     * where Google verifies the purchase before the entitlement
     * is credited.
     */

    private fun processPurchase(
        purchase: Purchase
    ) {

        when (
            purchase.purchaseState
        ) {

            Purchase
                .PurchaseState
                .PURCHASED -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Purchased(
                        purchaseToken =
                            purchase.purchaseToken
                    )
            }

            Purchase
                .PurchaseState
                .PENDING -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Pending
            }

            Purchase
                .PurchaseState
                .UNSPECIFIED_STATE -> {

                _purchaseState.value =
                    VehicleReportPurchaseState.Error(
                        "The Google Play purchase has an unknown state."
                    )
            }
        }
    }

    /*
     * =========================================================
     * CONSUME GOOGLE PURCHASE
     * =========================================================
     *
     * This is available for the secure purchase-processing
     * path. For production, the secure backend should consume
     * the Google purchase after entitlement has been granted.
     */

    fun consumePurchase(
        purchaseToken: String,
        onComplete:
            (Boolean, String?) -> Unit
    ) {

        val consumeParams =
            ConsumeParams
                .newBuilder()
                .setPurchaseToken(
                    purchaseToken
                )
                .build()

        billingClient.consumeAsync(
            consumeParams
        ) { billingResult, _ ->

            if (
                billingResult.responseCode ==
                BillingClient
                    .BillingResponseCode
                    .OK
            ) {

                onComplete(
                    true,
                    null
                )

            } else {

                onComplete(
                    false,
                    billingResult
                        .debugMessage
                        .ifBlank {
                            "The Pro Vehicle Search purchase could not be consumed."
                        }
                )
            }
        }
    }

    /*
     * =========================================================
     * REFRESH
     * =========================================================
     */

    fun refreshProduct() {
        connect()
    }

    /*
     * =========================================================
     * RELEASE
     * =========================================================
     */

    fun release() {

        if (
            billingClient.isReady
        ) {

            billingClient.endConnection()
        }
    }
}