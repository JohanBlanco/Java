import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';

class ApiException implements Exception {
  final String message;
  final int statusCode;
  ApiException(this.message, this.statusCode);

  @override
  String toString() => message;
}

class ApiService {
  String? _token;

  void setToken(String? token) => _token = token;

  Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        if (_token != null) 'Authorization': 'Bearer $_token',
      };

  Future<dynamic> _request(String method, String path, {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    late http.Response response;

    switch (method) {
      case 'GET':
        response = await http.get(uri, headers: _headers);
        break;
      case 'POST':
        response = await http.post(uri, headers: _headers, body: jsonEncode(body));
        break;
      case 'PUT':
        response = await http.put(uri, headers: _headers, body: jsonEncode(body));
        break;
      default:
        throw ApiException('Método no soportado', 0);
    }

    if (response.statusCode >= 400) {
      final data = jsonDecode(response.body);
      throw ApiException(data['message'] ?? 'Error en la solicitud', response.statusCode);
    }

    if (response.body.isEmpty) return null;
    return jsonDecode(response.body);
  }

  Future<Map<String, dynamic>> login(String login, String password) async {
    return await _request('POST', '/auth/login', body: {'login': login, 'password': password});
  }

  Future<Map<String, dynamic>> getMe() async {
    return await _request('GET', '/users/me');
  }

  Future<void> updateProfile(Map<String, dynamic> data) async {
    await _request('PUT', '/users/me/profile', body: data);
  }

  Future<List<dynamic>> getActivities({String? from, String? to}) async {
    final params = <String, String>{};
    if (from != null) params['from'] = from;
    if (to != null) params['to'] = to;
    final q = params.entries.map((e) => '${e.key}=${Uri.encodeComponent(e.value)}').join('&');
    return await _request('GET', '/activities${q.isNotEmpty ? '?$q' : ''}') ?? [];
  }

  Future<List<dynamic>> getActivitySeries() async {
    return await _request('GET', '/activities?series=true') ?? [];
  }

  Future<Map<String, dynamic>> getActivityDeleteImpact(int id) async {
    return await _request('GET', '/activities/$id/reservation-impact');
  }

  Future<Map<String, dynamic>> previewActivityUpdateImpact(int id, Map<String, dynamic> data) async {
    return await _request('POST', '/activities/$id/reservation-impact/preview', body: data);
  }

  Future<void> deleteActivity(int id, {bool cancelReservations = false}) async {
    await _request('DELETE', '/activities/$id?cancelReservations=$cancelReservations');
  }

  Future<Map<String, dynamic>> editActivityOccurrence(int id, Map<String, dynamic> data) async {
    return await _request('PUT', '/activities/$id/occurrence-edit', body: data);
  }

  Future<List<dynamic>> getSales() async {
    return await _request('GET', '/sales') ?? [];
  }

  Future<Map<String, dynamic>> getStatsSummary() async {
    return await _request('GET', '/stats/summary');
  }

  Future<Map<String, dynamic>> createActivity(Map<String, dynamic> data) async {
    return await _request('POST', '/activities', body: data);
  }

  Future<Map<String, dynamic>> updateActivity(int id, Map<String, dynamic> data) async {
    return await _request('PUT', '/activities/$id', body: data);
  }

  Future<Map<String, dynamic>> getMyMembershipUsage() async {
    return await _request('GET', '/users/me/membership-usage');
  }

  Future<List<dynamic>> getPendingMembershipPayment() async {
    return await _request('GET', '/users/pending-membership-payment') ?? [];
  }

  Future<List<dynamic>> getPendingPaymentReservations() async {
    return await _request('GET', '/reservations/pending-payment') ?? [];
  }

  Future<List<dynamic>> getMyReservations() async {
    return await _request('GET', '/reservations/me') ?? [];
  }

  Future<Map<String, dynamic>> createReservation(
    int activityId, {
    bool payAtReception = false,
    String? occurrenceDate,
  }) async {
    return await _request('POST', '/activities/$activityId/reservations', body: {
      'payAtReception': payAtReception,
      if (occurrenceDate != null) 'occurrenceDate': occurrenceDate,
    });
  }

  Future<Map<String, dynamic>> markReservationPaid(int id) async {
    return await _request('POST', '/reservations/$id/mark-paid');
  }

  Future<Map<String, dynamic>> confirmReservation(int id) async {
    return await _request('POST', '/reservations/$id/confirm');
  }

  Future<Map<String, dynamic>> cancelReservation(int id) async {
    return await _request('POST', '/reservations/$id/cancel');
  }

  Future<List<dynamic>> getMyRoutines() async {
    return await _request('GET', '/routines/me') ?? [];
  }

  Future<Map<String, dynamic>> createRoutineRequest(String description, String goals) async {
    return await _request('POST', '/routine-requests', body: {
      'description': description,
      'goals': goals,
    });
  }

  Future<List<dynamic>> getRoutineRequests() async {
    return await _request('GET', '/routine-requests') ?? [];
  }

  Future<Map<String, dynamic>> updateRoutineRequestStatus(int id, String status) async {
    return await _request('PUT', '/routine-requests/$id/status', body: {'status': status});
  }

  Future<Map<String, dynamic>> createAppointmentRequest(String type, String notes) async {
    return await _request('POST', '/appointment-requests', body: {
      'type': type,
      if (notes.isNotEmpty) 'notes': notes,
    });
  }

  Future<List<dynamic>> getAppointmentRequests() async {
    return await _request('GET', '/appointment-requests') ?? [];
  }

  Future<List<dynamic>> getMyAppointmentRequests() async {
    return await _request('GET', '/appointment-requests/me') ?? [];
  }

  Future<Map<String, dynamic>> updateAppointmentRequestStatus(int id, String status) async {
    return await _request('PUT', '/appointment-requests/$id/status', body: {'status': status});
  }

  Future<List<dynamic>> getPlatformOrganizations() async {
    return await _request('GET', '/platform/organizations') ?? [];
  }

  Future<Map<String, dynamic>> getPlatformOrganization(int id) async {
    return await _request('GET', '/platform/organizations/$id');
  }

  Future<Map<String, dynamic>> createOrganization(Map<String, dynamic> data) async {
    return await _request('POST', '/platform/organizations', body: data);
  }

  Future<Map<String, dynamic>> updateOrganization(int id, Map<String, dynamic> data) async {
    return await _request('PUT', '/platform/organizations/$id', body: data);
  }

  Future<List<dynamic>> getPackages() async {
    return await _request('GET', '/packages') ?? [];
  }

  Future<Map<String, dynamic>> createPackage(Map<String, dynamic> data) async {
    return await _request('POST', '/packages', body: data);
  }

  Future<Map<String, dynamic>> updatePackage(int id, Map<String, dynamic> data) async {
    return await _request('PUT', '/packages/$id', body: data);
  }

  Future<List<dynamic>> getUsers() async {
    return await _request('GET', '/users') ?? [];
  }

  Future<Map<String, dynamic>> createUser(Map<String, dynamic> data) async {
    return await _request('POST', '/users', body: data);
  }

  Future<Map<String, dynamic>> updateUser(int id, Map<String, dynamic> data) async {
    return await _request('PUT', '/users/$id', body: data);
  }
}
