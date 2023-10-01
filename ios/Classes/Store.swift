//
//  Store.swift
//  inapp_purchase
//
//  Created by George Stryhun Lanars on 27.09.2023.
//

import Foundation
import StoreKit

public enum StoreError: Error {
    case failedVerification
}

class Store {
    private let productIds = ["basic_subscription_monthly", "basic_subscription_yearly"] // TODO: make dynamic
    
    @Published private(set) var availableSubscriptions: [Product]
    @Published private(set) var purchasedSubscriptions: [Product] = []
    
    var updateListenerTask: Task<Void, Error>? = nil
    
    init() {
        availableSubscriptions = []
        
        updateListenerTask = listenForTransactions()
        
        Task {
            await requestProducts()
            await updateCustomerProductStatus()
        }
    }
    
    deinit {
        updateListenerTask?.cancel()
    }
    
    func listenForTransactions() -> Task<Void, Error> {
        return Task.detached {
            for await result in Transaction.updates {
                do {
                    let transaction = try self.checkVerified(result)
                    // TODO: deliver products
                    await transaction.finish()
                    print("Transaction updated: \(transaction)")
                } catch {
                    print("Transaction failed verification")
                }
            }
        }
    }
    
    @MainActor
    func requestProducts() async {
        do {
            let storeProducts = try await Product.products(for: productIds)
            
            var newSubscriptions: [Product] = []
            
            for product in storeProducts {
                switch product.type {
                case .autoRenewable:
                    newSubscriptions.append(product)
                default:
                    //Ignore this product.
                    print("Unknown product")
                }
            }
            
            availableSubscriptions = newSubscriptions
            print("Subscriptions updated: \(availableSubscriptions)")
        } catch {
            print("Failed product request from the App Store server: \(error)")
        }
    }
    
    func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        //Check whether the JWS passes StoreKit verification.
        switch result {
        case .unverified:
            //StoreKit parses the JWS, but it fails verification.
            throw StoreError.failedVerification
        case .verified(let safe):
            //The result is verified. Return the unwrapped value.
            return safe
        }
    }
    
    @MainActor
    func updateCustomerProductStatus() async {
        var purchasedSubscriptions: [Product] = []
        
        for await result in Transaction.currentEntitlements {
            do {
                //Check whether the transaction is verified. If it isn’t, catch `failedVerification` error.
                let transaction = try checkVerified(result)
                
                print("Entitlenemnt updated: \(transaction)")
                
                //Check the `productType` of the transaction and get the corresponding product from the store.
                switch transaction.productType {
                case .autoRenewable:
                    if let subscription = availableSubscriptions.first(where: { $0.id == transaction.productID }) {
                        purchasedSubscriptions.append(subscription)
                    }
                default:
                    break
                }
            } catch {
                print()
            }
        }
        
        self.purchasedSubscriptions = purchasedSubscriptions
    }
}
