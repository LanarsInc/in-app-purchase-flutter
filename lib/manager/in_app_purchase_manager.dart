import 'package:inapp_purchase/manager/iap_event.dart';
import 'package:inapp_purchase/manager/iap_state.dart';
import 'package:inapp_purchase/models/product.dart';

abstract interface class InAppPurchaseManager {
  Stream<IAPState> get stateStream;

  Stream<IAPEvent> get eventStream;

  Stream<Product> get subscriptions;

  Stream<Product> get purchasedSubscriptions;

  void requestProducts();

  void buy(Product product);
}
