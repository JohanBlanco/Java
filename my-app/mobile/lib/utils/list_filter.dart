import 'package:flutter/material.dart';

const _maxDepth = 5;

List<String> _collectValues(dynamic value, [int depth = 0]) {
  if (depth > _maxDepth || value == null) return [];
  if (value is String || value is num || value is bool) {
    return [value.toString()];
  }
  if (value is DateTime) {
    return [value.toIso8601String(), value.toString()];
  }
  if (value is List) {
    return value.expand((v) => _collectValues(v, depth + 1)).toList();
  }
  if (value is Map) {
    return value.values.expand((v) => _collectValues(v, depth + 1)).toList();
  }
  return [];
}

bool itemMatchesQuery(
  dynamic item,
  String query, {
  List<String> extraValues = const [],
}) {
  final q = query.trim().toLowerCase();
  if (q.isEmpty) return true;
  final values = [..._collectValues(item), ...extraValues];
  return values.any((v) => v.toLowerCase().contains(q));
}

List<T> filterByQuery<T>(
  List<T> items,
  String query, {
  List<String> Function(T item)? extraValues,
}) {
  if (query.trim().isEmpty) return items;
  return items
      .where((item) => itemMatchesQuery(
            item,
            query,
            extraValues: extraValues?.call(item) ?? const [],
          ))
      .toList();
}
