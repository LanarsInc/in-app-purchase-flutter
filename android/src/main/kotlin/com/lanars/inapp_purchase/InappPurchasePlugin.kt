package com.lanars.inapp_purchase

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "InAppPurchasePlugin"

/** InAppPurchasePlugin */
class InAppPurchasePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var billingManager: BillingManager2
    private lateinit var methodChannel: MethodChannel
    private lateinit var subscriptionsChannel: EventChannel
    private lateinit var purchasedSubscriptionsChannel: EventChannel

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var weakActivity: WeakReference<Activity> = WeakReference(null)

    private val purchaseVerifier = PurchaseVerifier { purchaseToken, productId ->
        return@PurchaseVerifier suspendCoroutine { continuation ->
            methodChannel.invokeMethod(
                "verifyPurchase",
                mapOf(
                    "purchaseToken" to purchaseToken,
                    "productId" to productId
                ),
                object : MethodChannel.Result {
                    override fun success(result: Any?) {
                        continuation.resume(result as? Boolean ?: false)
                    }

                    override fun error(
                        errorCode: String,
                        errorMessage: String?,
                        errorDetails: Any?
                    ) {
                        continuation.resume(false)
                    }

                    override fun notImplemented() {
                        continuation.resume(false)
                    }
                }
            )
        }
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        billingManager = BillingManager2(
            flutterPluginBinding.applicationContext,
            coroutineScope,
            purchaseVerifier
        )
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, METHOD_CHANNEL_NAME)
        methodChannel.setMethodCallHandler(this)
        subscriptionsChannel = EventChannel(
            flutterPluginBinding.binaryMessenger,
            SUBSCRIPTIONS_CHANNEL_NAME
        )
        subscriptionsChannel.setStreamHandler(SubscriptionStreamHandler())
        purchasedSubscriptionsChannel = EventChannel(
            flutterPluginBinding.binaryMessenger,
            PURCHASED_SUBSCRIPTIONS_CHANNEL_NAME
        )
        purchasedSubscriptionsChannel.setStreamHandler(PurchasedSubscriptionStreamHandler())
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val method = Method.fromMethodName(call.method) ?: run {
            result.notImplemented()
            return
        }

        when (method) {
            Method.REFRESH_PRODUCTS -> {
                Log.d(TAG, "onMethodCall: ${method.methodName}")

                val identifiers = call.argument<List<String>>("identifiers") ?: run {
                    result.error("identifiers is null", null, null)
                    return
                }
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        billingManager.refreshProducts(identifiers)
                        result.success(null)
                    } catch (e: Throwable) {
                        result.error(e.localizedMessage.orEmpty(), null, null)
                    }
                }
            }

            Method.BUY_PRODUCT -> {
                Log.d(TAG, "onMethodCall: ${method.methodName}")

                val activity = weakActivity.get() ?: run {
                    result.error("activity is null", null, null)
                    return
                }
                val productId = call.argument<String>("productId") ?: run {
                    result.error("productId is null", null, null)
                    return
                }
                coroutineScope.launch {
                    val product = billingManager.products.first().firstOrNull {
                        it.productId == productId
                    } ?: run {
                        result.error("product not found", null, null)
                        return@launch
                    }
                    billingManager.launchPurchase(
                        activity,
                        product,
                        product.subscriptionOfferDetails!!.first().offerToken
                    )
                    result.success(null)
                }
            }

            Method.RESTORE_PURCHASES -> {
                // TODO: implement
                Log.d(TAG, "onMethodCall: ${method.methodName}")
                result.notImplemented()
            }

            Method.GET_PLATFORM_VERSION -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        coroutineScope.cancel()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.weakActivity = WeakReference(binding.activity)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        this.weakActivity.clear()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        this.weakActivity = WeakReference(binding.activity)
    }

    override fun onDetachedFromActivity() {
        this.weakActivity.clear()
    }

    companion object {
        const val METHOD_CHANNEL_NAME = "com.lanars.inapp_purchase/methods"
        const val SUBSCRIPTIONS_CHANNEL_NAME = "com.lanars.inapp_purchase/subscriptions"
        const val PURCHASED_SUBSCRIPTIONS_CHANNEL_NAME = "com.lanars.inapp_purchase/purchased_subs"
    }

    enum class Method(val methodName: String) {
        REFRESH_PRODUCTS("requestProducts([String])"),
        BUY_PRODUCT("buy(Product)"),
        RESTORE_PURCHASES("restorePurchases()"),
        GET_PLATFORM_VERSION("getPlatformVersion"); // TODO: remove

        companion object {
            fun fromMethodName(methodName: String): Method? {
                return values().firstOrNull { it.methodName == methodName }
            }
        }
    }

    inner class SubscriptionStreamHandler : EventChannel.StreamHandler {
        private var eventSink: EventChannel.EventSink? = null
        private var listenJob: Job? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            this.eventSink = events
            listenJob = coroutineScope.launch {
                billingManager.subscriptions.collect { subscriptions ->
                    sendEvent(subscriptions.map { it.toJsonString() })
                }
            }
        }

        override fun onCancel(arguments: Any?) {
            this.eventSink = null
            listenJob?.cancel()
        }

        private fun sendEvent(event: Any) {
            eventSink?.success(event)
        }
    }

    inner class PurchasedSubscriptionStreamHandler : EventChannel.StreamHandler {
        private var eventSink: EventChannel.EventSink? = null
        private var listenJob: Job? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            this.eventSink = events
            listenJob = coroutineScope.launch {
                billingManager.purchasedProducts.collect { products ->
                    sendEvent(products.map { it.toJsonString() })
                }
            }
        }

        override fun onCancel(arguments: Any?) {
            this.eventSink = null
            listenJob?.cancel()
        }

        private fun sendEvent(event: Any) {
            eventSink?.success(event)
        }
    }
}

fun ProductDetails.toJsonString(): String {
    val json = JSONObject().apply {
        put("id", productId)
        put("title", title)
        put("description", description)
        put(
            "price",
            subscriptionOfferDetails!!.first().pricingPhases.pricingPhaseList.first().priceAmountMicros / 1000000.0
        )
        put(
            "displayPrice",
            subscriptionOfferDetails!!.first().pricingPhases.pricingPhaseList.first().formattedPrice
        )
        put(
            "type",
            when (productType) {
                BillingClient.ProductType.SUBS -> "autoRenewable"
                BillingClient.ProductType.INAPP -> "consumable" // TODO: handle non-consumable
                else -> ""
            }
        )
    }
    return json.toString()
}
