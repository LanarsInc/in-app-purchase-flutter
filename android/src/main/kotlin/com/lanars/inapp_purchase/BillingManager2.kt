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
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "BillingManager"

class BillingManager2(
    applicationContext: Context,
    private val coroutineScope: CoroutineScope,
    private val purchaseVerifier: PurchaseVerifier
) : BillingClientStateListener,
    ProductDetailsResponseListener,
    PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(applicationContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    /**
     * Current state of billing client
     */
    private val _connectionState = MutableStateFlow(false)
    val connectionState = _connectionState.asStateFlow()

    /**
     * All available products, fetched from Google Play with [refreshProducts]
     */
    private val _subscriptions = MutableStateFlow<List<ProductDetails>>(emptyList())
    val subscriptions = _subscriptions.asStateFlow()

    private val _oneTimeProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val oneTimeProducts = _oneTimeProducts.asStateFlow()

    private val _purchasedSubscriptions = MutableStateFlow<List<ProductDetails>>(emptyList())
    val purchasedSubscrptions = _purchasedSubscriptions.asStateFlow()

    private val _purchasedOneTimeProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val purchasedOneTimeProducts = _purchasedOneTimeProducts.asStateFlow()

    init {
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Log.d(TAG, "Billing client connected")
                _connectionState.update { true }
            }

            else -> Log.d(TAG, billingResult.debugMessage)
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.d(TAG, "Billing client disconnected")
        _connectionState.update { false }
        // TODO: implement retry logic
    }

    override fun onProductDetailsResponse(
        billingResult: BillingResult,
        productDetailsList: List<ProductDetails>
    ) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                val newSubscriptions =
                    productDetailsList.filter { it.productType == ProductType.SUBS }
                val newOneTimeProducts =
                    productDetailsList.filter { it.productType == ProductType.INAPP }
                _subscriptions.update { newSubscriptions }
                _oneTimeProducts.update { newOneTimeProducts }
            }

            else -> Log.d(TAG, "onProductDetailsResponse: $billingResult")
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            /*for (purchase in purchases) {
                handlePurchase(purchase)
            }*/
            val purchasedProductIds = purchases.flatMap { it.products }
            val purchasedSubs = purchasedProductIds.mapNotNull { id ->
                subscriptions.value.firstOrNull { it.productId == id }
            }
            val purchasedOneTimes = purchasedProductIds.mapNotNull { id ->
                oneTimeProducts.value.firstOrNull { it.productId == id }
            }
            _purchasedSubscriptions.update { purchasedSubs }
            _purchasedOneTimeProducts.update { purchasedOneTimes }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    fun refreshProducts(identifiers: List<String>) {
        val subsRequestParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                identifiers.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(ProductType.SUBS)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(subsRequestParams, this)

        val inappRequestParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                identifiers.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(ProductType.INAPP)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(inappRequestParams, this)
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready")
        }

        coroutineScope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.SUBS)
                .build()
            val result = billingClient.queryPurchasesAsync(params)
            when (result.billingResult.responseCode) {
                BillingResponseCode.OK -> {
                    val purchasedProductIds = result.purchasesList.flatMap { it.products }
                    val updatedPurchasedProducts = subscriptions.value.filter {
                        purchasedProductIds.contains(it.productId)
                    }
                    _purchasedSubscriptions.update { updatedPurchasedProducts }
                }

                else -> Log.d(TAG, result.billingResult.debugMessage)
            }
        }

        coroutineScope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.INAPP)
                .build()
            val result = billingClient.queryPurchasesAsync(params)
            when (result.billingResult.responseCode) {
                BillingResponseCode.OK -> {
                    val purchasedProductIds = result.purchasesList.flatMap { it.products }
                    val updatedPurchasedProducts = oneTimeProducts.value.filter {
                        purchasedProductIds.contains(it.productId)
                    }
                    _purchasedOneTimeProducts.update { updatedPurchasedProducts }
                }

                else -> Log.d(TAG, result.billingResult.debugMessage)
            }
        }
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
}

@OptIn(ExperimentalCoroutinesApi::class)
val BillingManager2.products: Flow<List<ProductDetails>>
    get() = merge(subscriptions, oneTimeProducts)

@OptIn(ExperimentalCoroutinesApi::class)
val BillingManager2.purchasedProducts: Flow<List<ProductDetails>>
    get() = merge(purchasedSubscrptions, purchasedOneTimeProducts)
