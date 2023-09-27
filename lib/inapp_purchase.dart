
import 'inapp_purchase_platform_interface.dart';

class InAppPurchase {
  Future<String?> getPlatformVersion() {
    return InAppPurchasePlatform.instance.getPlatformVersion();
  }
}
