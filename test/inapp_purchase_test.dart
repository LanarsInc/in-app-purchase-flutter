import 'package:flutter_test/flutter_test.dart';
import 'package:inapp_purchase/inapp_purchase.dart';
import 'package:inapp_purchase/inapp_purchase_platform_interface.dart';
import 'package:inapp_purchase/inapp_purchase_method_channel.dart';
import 'package:inapp_purchase/models/product.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockInAppPurchasePlatform
    with MockPlatformInterfaceMixin
    implements InAppPurchasePlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  // TODO: implement availableSubscriptions
  Stream<List<Product>> get availableSubscriptions => throw UnimplementedError();

  @override
  void buy(Product product) {
    // TODO: implement buy
  }

  @override
  void refreshProducts() {
    // TODO: implement refreshProducts
  }

  @override
  void restore() {
    // TODO: implement restore
  }

  @override
  // TODO: implement purchasedSubscriptions
  Stream<List<Product>> get purchasedSubscriptions => throw UnimplementedError();
}

void main() {
  final InAppPurchasePlatform initialPlatform = InAppPurchasePlatform.instance;

  test('$MethodChannelInAppPurchase is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelInAppPurchase>());
  });

  test('getPlatformVersion', () async {
    InAppPurchase inappPurchasePlugin = InAppPurchase();
    MockInAppPurchasePlatform fakePlatform = MockInAppPurchasePlatform();
    InAppPurchasePlatform.instance = fakePlatform;

    expect(await inappPurchasePlugin.getPlatformVersion(), '42');
  });
}
