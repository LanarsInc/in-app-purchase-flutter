package com.lanars.inapp_purchase

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.Collections.emptyList

private const val TAG = "InAppPurchasePlugin"

/** InAppPurchasePlugin */
class InAppPurchasePlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var methodChannel: MethodChannel
    private lateinit var subscriptionsChannel: EventChannel

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, METHOD_CHANNEL_NAME)
        methodChannel.setMethodCallHandler(this)
        subscriptionsChannel = EventChannel(
            flutterPluginBinding.binaryMessenger,
            SUBSCRIPTIONS_CHANNEL_NAME
        )
        subscriptionsChannel.setStreamHandler(SubscriptionStreamHandler())
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val method = Method.fromMethodName(call.method) ?: run {
            result.notImplemented()
            return
        }

        when (method) {
            Method.REFRESH_PRODUCTS -> {
                // TODO: implement
                Log.d(TAG, "onMethodCall: ${method.methodName}")
                result.notImplemented()
            }

            Method.BUY_PRODUCT -> {
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
    }

    companion object {
        const val METHOD_CHANNEL_NAME = "com.lanars.inapp_purchase/methods"
        const val SUBSCRIPTIONS_CHANNEL_NAME = "com.lanars.inapp_purchase/subscriptions"
    }

    enum class Method(val methodName: String) {
        REFRESH_PRODUCTS("refreshProducts()"),
        BUY_PRODUCT("buy(Product)"),
        GET_PLATFORM_VERSION("getPlatformVersion"); // TODO: remove

        companion object {
            fun fromMethodName(methodName: String): Method? {
                return values().firstOrNull { it.methodName == methodName }
            }
        }
    }

    class SubscriptionStreamHandler : EventChannel.StreamHandler {
        private var eventSink: EventChannel.EventSink? = null

        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            this.eventSink = events
            sendEvent(emptyList<String>())
            // TODO: implement
        }

        override fun onCancel(arguments: Any?) {
            this.eventSink = null
        }

        fun sendEvent(event: Any) {
            eventSink?.success(event)
        }
    }
}
