import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:inapp_purchase/models/product.dart';

import 'inapp_purchase_platform_interface.dart';

/// An implementation of [InAppPurchasePlatform] that uses method channels.
class MethodChannelInAppPurchase extends InAppPurchasePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('com.lanars.inapp_purchase/methods');

  final subscriptionsChannel = const EventChannel('com.lanars.inapp_purchase/subscriptions');

  final purchasedSubscriptionsChannel =
      const EventChannel("com.lanars.inapp_purchase/purchased_subs");

  MethodChannelInAppPurchase() {
    methodChannel.setMethodCallHandler(_handleMethodCall);
  }

  Future<dynamic> _handleMethodCall(MethodCall methodCall) async {
    switch (methodCall.method) {
      case 'verifyPurchase':
        await Future.delayed(const Duration(milliseconds: 500));
        return Future.value(true);
      default:
        throw UnimplementedError();
    }
  }

  @override
  Stream<List<Product>> get availableSubscriptions =>
      subscriptionsChannel.receiveBroadcastStream().map(
            (event) => (event as List<dynamic>).map(
              (item) {
                final json = jsonDecode(item) as Map<String, dynamic>;
                return ProductJsonExtension.fromJson(json);
              },
            ).toList(),
          );

  @override
  Stream<List<Product>> get purchasedSubscriptions =>
      purchasedSubscriptionsChannel.receiveBroadcastStream().map(
            (event) => (event as List<dynamic>).map(
              (item) {
                final json = jsonDecode(item) as Map<String, dynamic>;
                return ProductJsonExtension.fromJson(json);
              },
            ).toList(),
          );

  @override
  void buy(Product product) {
    final args = {
      'productId': product.id,
    };
    methodChannel.invokeMethod('buy(Product)', args);
  }

  @override
  void requestProducts(Set<String> identifiers) {
    final args = {
      'identifiers': identifiers.toList(),
    };
    methodChannel.invokeMethod('requestProducts([String])', args);
  }

  @override
  void restore() {
    methodChannel.invokeMethod('restore()');
  }
}
