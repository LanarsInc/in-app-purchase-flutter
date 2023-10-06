import 'package:inapp_purchase/models/product_type.dart';

class Product {
  final String id;
  final String title;
  final String description;
  final double price;
  final String displayPrice;
  final ProductType type;

  Product({
    required this.id,
    required this.title,
    required this.description,
    required this.price,
    required this.displayPrice,
    required this.type,
  });

  @override
  String toString() {
    return 'Product{id: $id, title: $title, description: $description, price: $price, displayPrice: $displayPrice, type: $type}';
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Product &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          title == other.title &&
          description == other.description &&
          price == other.price &&
          displayPrice == other.displayPrice &&
          type == other.type;

  @override
  int get hashCode =>
      id.hashCode ^
      title.hashCode ^
      description.hashCode ^
      price.hashCode ^
      displayPrice.hashCode ^
      type.hashCode;
}

extension ProductJsonExtension on Product {
  static Product fromJson(Map<String, dynamic> json) {
    return Product(
      id: json['id'],
      title: json['title'],
      description: json['description'],
      price: (json['price'] as num).toDouble(),
      displayPrice: json['displayPrice'],
      type: ProductTypeExtension.fromString(json['type']),
    );
  }
}
