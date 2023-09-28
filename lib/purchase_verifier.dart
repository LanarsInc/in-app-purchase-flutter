import 'package:inapp_purchase/purchase.dart';

abstract interface class PurchaseVerifier {
  Future<bool> verify(Purchase purchase);
}
