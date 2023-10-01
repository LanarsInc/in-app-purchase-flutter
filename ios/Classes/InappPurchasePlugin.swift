import Flutter
import UIKit
import StoreKit
import Foundation
import Combine

public class InAppPurchasePlugin: NSObject, FlutterPlugin {
    
    private static let store = Store()
    
    private static let streamHandler = StreamHandler()
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let methodChannel = FlutterMethodChannel(name: "inapp_purchase_methods", binaryMessenger: registrar.messenger())
        let eventChannel = FlutterEventChannel(name: "inapp_purchase_events", binaryMessenger: registrar.messenger())
        let instance = InAppPurchasePlugin()
        registrar.addMethodCallDelegate(instance, channel: methodChannel)
        eventChannel.setStreamHandler(self.streamHandler)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let method = Method(rawValue: call.method) else {
            result(FlutterMethodNotImplemented)
            return
        }
        
        switch method {
        case .refreshProducts:
            onRefreshProducts(call, result)
        case .buy:
            onBuyProduct(call, result)
        case .getPlatformVersion:
            result("iOS " + UIDevice.current.systemVersion)
        }
    }
    
    private func onRefreshProducts(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        print("Refresh products")
        Task {
            await InAppPurchasePlugin.store.requestProducts()
        }
        result(nil)
    }
    
    private func onBuyProduct(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        if let args = BuyArguments(call) {
            print("Buy product \(args.productId)")
            result(nil)
        }
        result(nil)
    }
    
    class StreamHandler : NSObject, FlutterStreamHandler {
        private var eventSink: FlutterEventSink?
        
        private var availableSubscriptionsCancelable: Cancellable?
        
        func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
            self.eventSink = events
            startListeningPurchases()
            return nil
        }
        
        func onCancel(withArguments arguments: Any?) -> FlutterError? {
            self.eventSink = nil
            availableSubscriptionsCancelable?.cancel()
            availableSubscriptionsCancelable = nil
            return nil
        }
        
        func sendEvent(_ event: Any) {
            eventSink?(event)
        }
        
        private func startListeningPurchases() {
            availableSubscriptionsCancelable = store.$availableSubscriptions.sink { products in
                self.sendEvent("Products updated")
            }
        }
    }
}

enum Method: String {
    case refreshProducts = "refreshProducts()"
    case buy = "buy(Product)"
    case getPlatformVersion = "getPlatformVersion" // TODO: remove
}

struct BuyArguments {
    let productId: String
    
    init?(_ call: FlutterMethodCall) {
        if let args = call.arguments as? [String: Any],
           let productId = args["productId"] as? String {
            self.productId = productId
        } else {
            return nil
        }
    }
}
