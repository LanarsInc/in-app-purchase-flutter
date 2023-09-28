import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'inapp_purchase_method_channel.dart';

abstract class InAppPurchasePlatform extends PlatformInterface {
  /// Constructs a InAppPurchasePlatform.
  InAppPurchasePlatform() : super(token: _token);

  static final Object _token = Object();

  static InAppPurchasePlatform _instance = MethodChannelInAppPurchase();

  /// The default instance of [InAppPurchasePlatform] to use.
  ///
  /// Defaults to [MethodChannelInAppPurchase].
  static InAppPurchasePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [InAppPurchasePlatform] when
  /// they register themselves.
  static set instance(InAppPurchasePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Stream<dynamic> getEventStream() {
    throw UnimplementedError('getEventStream() has not been implemented.');
  }
}
