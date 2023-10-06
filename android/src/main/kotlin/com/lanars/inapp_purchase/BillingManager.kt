package com.lanars.inapp_purchase

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BillingManager"

class BillingManager(
    private val applicationContext: Context,
    private val coroutineScope: CoroutineScope,
    private val purchaseVerifier: PurchaseVerifier
//    private val ioDispatcher: CoroutineDispatcher
) : BillingClientStateListener, PurchasesUpdatedListener {

    private var billingClient = BillingClient.newBuilder(applicationContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _subscriptions = MutableStateFlow<List<ProductDetails>>(emptyList())
    val subscriptions: StateFlow<List<ProductDetails>> = _subscriptions

    private val _purchasedSubscriptions = MutableStateFlow<List<ProductDetails>>(emptyList())
    val purchasedSubscriptions: StateFlow<List<ProductDetails>> = _purchasedSubscriptions

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
        Log.d(TAG, "handlePurchase: $purchase")
        coroutineScope.launch {
            val verified = true /*purchaseVerifier.verify(
                purchase.purchaseToken,
                purchase.products.first()
            )*/
            if (verified) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                val consumeResult = withContext(Dispatchers.IO) {
                    billingClient.consumePurchase(consumeParams)
                }
                Log.d(TAG, "handlePurchase result: ${consumeResult.billingResult.responseCode}")
                if (consumeResult.billingResult.responseCode == BillingResponseCode.OK) {
                    val products = purchase.products.mapNotNull { productId ->
                        _subscriptions.value.find { it.productId == productId }
                    }
                    val updatedPurchasedSubscriptions =
                        _purchasedSubscriptions.value.map { subscription ->
                            products.find { it.productId == subscription.productId } ?: subscription
                        }
                    _purchasedSubscriptions.emit(
                        updatedPurchasedSubscriptions
                    )
                }
            }

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                val ackPurchaseResult = withContext(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                }
            }
        }
    }

    suspend fun requestProducts(identifiers: List<String>) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                identifiers.map {
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
        Log.d("BillingManager", "launchPurchase: $billingResult")
        // TODO: handle result
    }

    suspend fun refreshPurchases() {
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()
        val inAppPurchasesResult = billingClient.queryPurchasesAsync(inAppParams)
        if (inAppPurchasesResult.billingResult.responseCode == BillingResponseCode.OK) {
            inAppPurchasesResult.purchasesList.forEach {
                handlePurchase(it)
            }
        }
        val subsPurchasesResult = billingClient.queryPurchasesAsync(subsParams)
        if (subsPurchasesResult.billingResult.responseCode == BillingResponseCode.OK) {
            val list = subsPurchasesResult.purchasesList
            list.forEach {
                handlePurchase(it)
            }
        }
    }
}
