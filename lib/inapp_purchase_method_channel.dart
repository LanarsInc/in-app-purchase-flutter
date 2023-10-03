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

  final eventChannel = const EventChannel('com.lanars.inapp_purchase/subscriptions');

  @override
  Future<String?> getPlatformVersion() async {
    final args = {
      'productId': 'basic_monthly',
    };
    await methodChannel.invokeMethod('buy(Product)', args);
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Stream<List<Product>> get availableSubscriptions => eventChannel.receiveBroadcastStream().map(
        (event) => (event as List<dynamic>).map(
          (item) {
            final json = jsonDecode(item) as Map<String, dynamic>;
            return ProductJsonExtension.fromJson(json);
          },
        ).toList(),
      );
}
