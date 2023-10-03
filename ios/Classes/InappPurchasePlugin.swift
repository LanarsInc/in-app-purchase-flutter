import Flutter
import UIKit
import StoreKit
import Foundation
import Combine

public class InappPurchasePlugin: NSObject, FlutterPlugin {
    
    private static let store = Store()
    
    private static let streamHandler = StreamHandler()
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let methodChannel = FlutterMethodChannel(name: "com.lanars.inapp_purchase/methods", binaryMessenger: registrar.messenger())
        let eventChannel = FlutterEventChannel(name: "com.lanars.inapp_purchase/subscriptions", binaryMessenger: registrar.messenger())
        let instance = InappPurchasePlugin()
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
            await InappPurchasePlugin.store.requestProducts()
        }
        result(nil)
    }
    
    private func onBuyProduct(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        if let args = BuyArguments(call) {
            print("Buy product \(args.productId)")
            Task {
                do {
                    let product = InappPurchasePlugin.store.availableSubscriptions.first { product in
                        product.id == args.productId
                    }
                    let purchaseResult = try await product?.purchase()
                    result(nil)
                } catch {
                    result(nil)
                }
            }
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
                let jsonProducts = products.map { $0.toJsonString() }.filter { $0 != nil }
                self.sendEvent(jsonProducts)
                //                let jsonProducts = [
                //                    "{\"id\": \"1\", \"displayName\": \"Monthly premium\", \"description\": \"Monthly\", \"price\": 9.99, \"displayPrice\": \"$99.9\"}"]
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
