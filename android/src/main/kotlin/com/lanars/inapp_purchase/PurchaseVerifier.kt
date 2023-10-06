package com.lanars.inapp_purchase

fun interface PurchaseVerifier {
    suspend fun verify(purchaseToken: String, productId: String): Boolean
}
