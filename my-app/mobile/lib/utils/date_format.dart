import 'dart:ui' as ui;

import 'package:intl/intl.dart';

/// Formato de fechas/horas según idioma: fechas locales y hora en 12 h.
class AppDateFormat {
  static bool get isEnglish =>
      ui.PlatformDispatcher.instance.locale.languageCode.startsWith('en');

  static String get datePattern => isEnglish ? 'MM/dd/yyyy' : 'dd/MM/yyyy';

  static String get timePattern => 'h:mm a';

  static String get dateTimePattern =>
      isEnglish ? 'MM/dd/yyyy h:mm a' : 'dd/MM/yyyy h:mm a';

  static String? formatIsoDate(String? iso) {
    if (iso == null || iso.isEmpty) return null;
    final normalized = iso.contains('T') ? iso : '${iso}T12:00:00';
    final parsed = DateTime.tryParse(normalized);
    if (parsed == null) return iso;
    return DateFormat(datePattern).format(parsed);
  }

  static String formatDateTime(DateTime value) {
    return DateFormat(dateTimePattern).format(value);
  }

  /// Acepta `HH:mm` / `HH:mm:ss` o ISO datetime; muestra 12 h.
  static String formatTime(String? value) {
    if (value == null || value.trim().isEmpty) return '';
    final raw = value.trim();
    final clock = RegExp(r'^(\d{1,2}):(\d{2})(?::(\d{2}))?$').firstMatch(raw);
    if (clock != null) {
      final h = int.tryParse(clock.group(1)!) ?? 0;
      final m = int.tryParse(clock.group(2)!) ?? 0;
      final s = int.tryParse(clock.group(3) ?? '0') ?? 0;
      return DateFormat(timePattern).format(DateTime(2000, 1, 1, h, m, s));
    }
    final parsed = DateTime.tryParse(raw);
    if (parsed == null) return raw;
    return DateFormat(timePattern).format(parsed);
  }

  static String formatTimeRange(String? start, String? end) {
    final a = formatTime(start);
    final b = formatTime(end);
    if (a.isEmpty && b.isEmpty) return '';
    if (a.isEmpty) return b;
    if (b.isEmpty) return a;
    return '$a – $b';
  }
}
