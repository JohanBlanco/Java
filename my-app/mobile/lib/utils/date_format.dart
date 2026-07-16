import 'dart:ui' as ui;

import 'package:intl/intl.dart';

/// Formato de fechas según idioma del dispositivo: dd/MM/yyyy (es) o MM/dd/yyyy (en).
class AppDateFormat {
  static bool get isEnglish =>
      ui.PlatformDispatcher.instance.locale.languageCode.startsWith('en');

  static String get datePattern => isEnglish ? 'MM/dd/yyyy' : 'dd/MM/yyyy';

  static String get dateTimePattern =>
      isEnglish ? 'MM/dd/yyyy HH:mm' : 'dd/MM/yyyy HH:mm';

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
}
