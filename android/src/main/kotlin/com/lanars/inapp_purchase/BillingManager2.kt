package com.lanars.inapp_purchase

import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "BillingManager"

class BillingManager2(
    applicationContext: Context,
    private val coroutineScope: CoroutineScope
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
    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products = _products.asStateFlow()

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
        productDetailsList.first().oneTimePurchaseOfferDetails
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> _products.update { productDetailsList }
            else -> Log.d(TAG, "onProductDetailsResponse: $billingResult")
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        TODO("Not yet implemented")
    }

    fun refreshProducts(identifiers: List<String>) {
        val subsRequestParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                identifiers.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
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
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        billingClient.queryProductDetailsAsync(inappRequestParams, this)
    }
}
