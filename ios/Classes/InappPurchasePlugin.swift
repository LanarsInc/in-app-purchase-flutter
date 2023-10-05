import Flutter
import UIKit
import StoreKit
import Foundation
import Combine

public class InappPurchasePlugin: NSObject, FlutterPlugin {
    
    private static let store = Store()
    
    private static let availableSubscriptionStreamHandler = SubscriptionsStreamHandler()
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let methodChannel = FlutterMethodChannel(
            name: "com.lanars.inapp_purchase/methods",
            binaryMessenger: registrar.messenger()
        )
        let subscriptionsChannel = FlutterEventChannel(
            name: "com.lanars.inapp_purchase/subscriptions",
            binaryMessenger: registrar.messenger()
        )
        let purchasedSubscriptionsChannel = FlutterEventChannel(
            name: "com.lanars.inapp_purchase/purchased_subs",
            binaryMessenger: registrar.messenger()
        )
        let instance = InappPurchasePlugin()
        registrar.addMethodCallDelegate(instance, channel: methodChannel)
        subscriptionsChannel.setStreamHandler(self.availableSubscriptionStreamHandler)
        purchasedSubscriptionsChannel.setStreamHandler(PurchasedSubscriptionStreamHandler())
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
            await InappPurchasePlugin.store.requestProducts()
        }
        result(nil)
    }
    
    private func onBuyProduct(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        if let args = BuyArguments(call) {
            print("Buy product \(args.productId)")
            Task {
                do {
                    let product = InappPurchasePlugin.store.subscriptions.first { product in
                        product.id == args.productId
                    }
                    if let product = product {
                        Task {
                            try await InappPurchasePlugin.store.purchase(product)
                        }
                    }
                    result(nil)
                } catch {
                    result(nil)
                }
            }
        }
        result(nil)
    }
    
    class SubscriptionsStreamHandler : NSObject, FlutterStreamHandler {
        private var eventSink: FlutterEventSink?
        
        private var cancellabe: Cancellable?
        
        func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
            self.eventSink = events
            startListeningPurchases()
            return nil
        }
        
        func onCancel(withArguments arguments: Any?) -> FlutterError? {
            self.eventSink = nil
            cancellabe?.cancel()
            cancellabe = nil
            return nil
        }
        
        func sendEvent(_ event: Any) {
            eventSink?(event)
        }
        
        private func startListeningPurchases() {
            cancellabe = store.$subscriptions.sink { products in
                let jsonProducts = products.map { $0.toJsonString() }.filter { $0 != nil }
                self.sendEvent(jsonProducts)
            }
        }
    }
    
    class PurchasedSubscriptionStreamHandler : NSObject, FlutterStreamHandler {
        private var eventSink: FlutterEventSink?
        
        private var cancellable: Cancellable?
        
        func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
            self.eventSink = events
            startListeningPurchases()
            return nil
        }
        
        func onCancel(withArguments arguments: Any?) -> FlutterError? {
            self.eventSink = nil
            cancellable?.cancel()
            cancellable = nil
            return nil
        }
        
        func sendEvent(_ event: Any) {
            eventSink?(event)
        }
        
        private func startListeningPurchases() {
            cancellable = store.$purchasedSubscriptions.sink { products in
                let jsonProducts = products.map { $0.toJsonString() }.filter { $0 != nil }
                self.sendEvent(jsonProducts)
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

extension Product: Encodable {
    enum CodingKeys: CodingKey {
        case id, title, description, price, displayPrice
    }
    
    public func encode(to encoder: Encoder) throws {
        var container: KeyedEncodingContainer<Product.CodingKeys> = encoder.container(keyedBy: CodingKeys.self)
        
        try container.encode(id, forKey: .id)
        try container.encode(displayName, forKey: .title)
        try container.encode(description, forKey: .description)
        try container.encode(price, forKey: .price)
        try container.encode(displayPrice, forKey: .displayPrice)
    }
}

extension Product {
    func toJsonString() -> String? {
        if let data = try? JSONEncoder().encode(self) {
            return String(data: data, encoding: .utf8)
        }
        return nil
    }
}
