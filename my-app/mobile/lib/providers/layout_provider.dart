import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LayoutProvider extends ChangeNotifier {
  static const _storageKey = 'sidebarVisible';

  bool _sidebarVisible = true;
  bool _loaded = false;

  bool get sidebarVisible => _sidebarVisible;
  bool get isLoaded => _loaded;

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    _sidebarVisible = prefs.getBool(_storageKey) ?? true;
    _loaded = true;
    notifyListeners();
  }

  Future<void> setSidebarVisible(bool visible) async {
    if (_sidebarVisible == visible) return;
    _sidebarVisible = visible;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_storageKey, visible);
  }

  void toggleSidebar() {
    setSidebarVisible(!_sidebarVisible);
  }
}
