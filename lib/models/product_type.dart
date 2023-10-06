enum ProductType {
  consumable,
  nonConsumable,
  nonRenewable,
  autoRenewable,
}

extension ProductTypeExtension on ProductType {
  static ProductType fromString(String value) {
    switch (value) {
      case 'consumable':
        return ProductType.consumable;
      case 'nonConsumable':
        return ProductType.nonConsumable;
      case 'nonRenewable':
        return ProductType.nonRenewable;
      case 'autoRenewable':
        return ProductType.autoRenewable;
      default:
        throw Exception('Unknown product type: $value');
    }
  }
}
