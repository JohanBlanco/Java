import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../config/role_helper.dart';
import '../services/api_service.dart';

class AuthProvider extends ChangeNotifier {
  final ApiService _api = ApiService();

  Map<String, dynamic>? _user;
  String? _activeRole;
  bool _isLoading = true;

  bool get isLoading => _isLoading;
  bool get isAuthenticated => _user != null;
  Map<String, dynamic>? get user => _user;
  String? get activeRole => _activeRole;
  List<String> get roles => RoleHelper.normalizeRoles(_user);
  ApiService get api => _api;

  String? get resolvedActiveRole => RoleHelper.resolveActiveRole(_user, _activeRole);

  bool hasRole(String role) => RoleHelper.hasRole(_user, role);

  String _activeRoleKey(int userId) => 'activeRole_$userId';

  Future<void> loadStoredAuth() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    final userJson = prefs.getString('user');

    if (token != null && userJson != null) {
      _api.setToken(token);
      _user = jsonDecode(userJson);
      final userId = (_user!['userId'] as num).toInt();
      _activeRole = RoleHelper.resolveActiveRole(
        _user,
        prefs.getString(_activeRoleKey(userId)),
      );
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<void> login(String email, String password) async {
    final response = await _api.login(email, password);
    _api.setToken(response['token']);
    _user = response;

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('token', response['token']);
    await prefs.setString('user', jsonEncode(response));

    final userId = (response['userId'] as num).toInt();
    _activeRole = RoleHelper.resolveActiveRole(
      _user,
      prefs.getString(_activeRoleKey(userId)),
    );
    if (_activeRole != null) {
      await prefs.setString(_activeRoleKey(userId), _activeRole!);
    }

    notifyListeners();
  }

  Future<void> setActiveRole(String role) async {
    if (_user == null) return;
    if (!RoleHelper.normalizeRoles(_user).contains(role)) return;

    _activeRole = role;
    final userId = (_user!['userId'] as num).toInt();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_activeRoleKey(userId), role);
    notifyListeners();
  }

  Future<void> logout() async {
    _user = null;
    _activeRole = null;
    _api.setToken(null);

    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('token');
    await prefs.remove('user');

    notifyListeners();
  }
}
