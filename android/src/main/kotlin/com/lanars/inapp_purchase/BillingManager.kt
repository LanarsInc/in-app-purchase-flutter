package com.lanars.inapp_purchase

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class BillingManager(
    private val applicationContext: Context,
//    private val coroutineScope: CoroutineScope,
//    private val ioDispatcher: CoroutineDispatcher
) : BillingClientStateListener, PurchasesUpdatedListener {

    private var billingClient = BillingClient.newBuilder(applicationContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _subscriptions = MutableStateFlow<List<ProductDetails>>(emptyList())
    val subscriptions: StateFlow<List<ProductDetails>> = _subscriptions

    init {
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {}
            else -> {}
        }
    }

    override fun onBillingServiceDisconnected() {
        // TODO("Not yet implemented")
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            purchases.forEach(::handlePurchase)
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // TODO: implement
    }

    suspend fun refreshProducts() {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                subscriptionIdentifiers.map {
                    Product.newBuilder()
                        .setProductId(it)
                        .setProductType(ProductType.SUBS)
                        .build()
                }
            )
            .build()

        val result = billingClient.queryProductDetails(queryProductDetailsParams)
        Log.d("BillingManager", "refreshProducts: ${result.productDetailsList}")
        _subscriptions.emit(result.productDetailsList.orEmpty())
        // TODO: handle result
    }

    fun launchPurchase(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        // TODO: handle result
    }

    companion object {
        private val subscriptionIdentifiers = listOf(
            "basic_subscription"
        )
    }
}
