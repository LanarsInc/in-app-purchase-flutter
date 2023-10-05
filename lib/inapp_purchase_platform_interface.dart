import 'package:inapp_purchase/inapp_purchase_method_channel.dart';
import 'package:inapp_purchase/models/product.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

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

  Stream<List<Product>> get availableSubscriptions =>
      throw UnimplementedError('availableSubscriptions has not been implemented.');

  Stream<List<Product>> get purchasedSubscriptions =>
      throw UnimplementedError('purchasedSubscriptions has not been implemented.');

  void buy(Product product) => throw UnimplementedError('buy has not been implemented.');

  void refreshProducts() => throw UnimplementedError('refreshProducts has not been implemented.');
}
