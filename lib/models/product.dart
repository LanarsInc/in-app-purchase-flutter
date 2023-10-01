import 'dart:io';

class Product {
  final String id;
  final String title;
  final String description;
  final double price;
  final String displayPrice;

  Product({
    required this.id,
    required this.title,
    required this.description,
    required this.price,
    required this.displayPrice,
  });

  @override
  String toString() {
    return 'Product{id: $id, title: $title, description: $description, price: $price, displayPrice: $displayPrice}';
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
          displayPrice == other.displayPrice;

  @override
  int get hashCode =>
      id.hashCode ^ title.hashCode ^ description.hashCode ^ price.hashCode ^ displayPrice.hashCode;
}

extension ProductJsonExtension on Product {
  static Product fromJson(Map<String, dynamic> json) {
    switch (Platform.operatingSystem) {
      case 'android':
        return _androidFromJson(json);
      case 'ios':
        return _iosFromJson(json);
      default:
        throw UnsupportedError('Unsupported platform ${Platform.operatingSystem}');
    }
  }

  static Product _androidFromJson(Map<String, dynamic> json) {
    return Product(
      id: json['productId'],
      title: json['title'],
      description: json['description'],
      price: double.parse(json['price']),
      displayPrice: json['displayPrice'],
    );
  }

  static Product _iosFromJson(Map<String, dynamic> json) {
    return Product(
      id: json['id'],
      title: json['displayName'],
      description: json['description'],
      price: json['price'],
      displayPrice: json['displayPrice'],
    );
  }
}
