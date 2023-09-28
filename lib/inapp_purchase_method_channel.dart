import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'inapp_purchase_platform_interface.dart';

/// An implementation of [InAppPurchasePlatform] that uses method channels.
class MethodChannelInAppPurchase extends InAppPurchasePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('inapp_purchase');

  final eventChannel = const EventChannel('inapp_purchase_events');

  @override
  Future<String?> getPlatformVersion() async {
    final args = {
      'productId': 'basic_monthly',
    };
    await methodChannel.invokeMethod('buy(Product)', args);
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    methodChannel.invokeMethod('refreshProducts()');
    return version;
  }

  @override
  Stream getEventStream() {
    return eventChannel.receiveBroadcastStream();
  }
}
