import 'package:flutter/material.dart';

import '../utils/date_format.dart';

class RoleHelper {
  static const roleLabels = {
    'GYM_OWNER': 'Admin',
    'RECEPTIONIST': 'Recepcionista',
    'INSTRUCTOR': 'Instructor',
    'MEMBER': 'Miembro',
    'PLATFORM_OWNER': 'Plataforma',
  };

  static const gymRoles = ['GYM_OWNER', 'RECEPTIONIST', 'INSTRUCTOR', 'MEMBER'];

  static const _rolePriority = [
    'PLATFORM_OWNER',
    'GYM_OWNER',
    'RECEPTIONIST',
    'INSTRUCTOR',
    'MEMBER',
  ];

  static List<String> normalizeRoles(Map<String, dynamic>? user) {
    if (user == null) return [];
    final roles = user['roles'];
    if (roles is List && roles.isNotEmpty) {
      return roles.map((r) => r.toString()).toList();
    }
    final role = user['role'];
    if (role != null) return [role.toString()];
    return [];
  }

  static List<String> getSwitchableRoles(Map<String, dynamic>? user) {
    return normalizeRoles(user).where(gymRoles.contains).toList();
  }

  static String? resolveDefaultActiveRole(List<String> roles) {
    for (final role in _rolePriority) {
      if (roles.contains(role)) return role;
    }
    return roles.isEmpty ? null : roles.first;
  }

  static String? resolveActiveRole(Map<String, dynamic>? user, String? storedActiveRole) {
    final roles = normalizeRoles(user);
    if (storedActiveRole != null && roles.contains(storedActiveRole)) {
      return storedActiveRole;
    }
    return resolveDefaultActiveRole(roles);
  }

  static bool hasRole(Map<String, dynamic>? user, String role) {
    return normalizeRoles(user).contains(role);
  }

  static bool hasAnyRole(Map<String, dynamic>? user, List<String> roles) {
    final userRoles = normalizeRoles(user);
    return roles.any(userRoles.contains);
  }

  static bool canViewReception(String? activeRole) {
    return activeRole != null && ['GYM_OWNER', 'RECEPTIONIST'].contains(activeRole);
  }

  static bool canViewVentas(String? activeRole) => canViewReception(activeRole);
  static bool canViewEstadisticas(String? activeRole) => canViewReception(activeRole);

  static bool canViewProfile(String? activeRole) {
    return activeRole != null && ['INSTRUCTOR', 'MEMBER', 'RECEPTIONIST'].contains(activeRole);
  }

  static bool canViewAdmin(String? activeRole) {
    return activeRole == 'GYM_OWNER';
  }

  static bool isMemberView(String? activeRole) {
    return activeRole == 'MEMBER';
  }

  static bool isStaffView(String? activeRole) {
    return activeRole != null && ['GYM_OWNER', 'INSTRUCTOR', 'RECEPTIONIST'].contains(activeRole);
  }

  static String formatRoles(List<String> roles) {
    return roles
        .where((role) => role != 'PLATFORM_OWNER')
        .map((role) => roleLabels[role] ?? role)
        .join(', ');
  }

  static String profileLabel(String? activeRole) {
    if (activeRole == null) return 'Usuario';
    return roleLabels[activeRole] ?? activeRole;
  }

  static const membershipStatusLabels = {
    'ACTIVE': 'Activo',
    'PAYMENT_PENDING': 'Pendiente de pago',
    'INACTIVE': 'Inactivo',
  };

  static Color membershipStatusColor(BuildContext context, String? status) {
    final scheme = Theme.of(context).colorScheme;
    return switch (status) {
      'ACTIVE' => scheme.primary,
      'PAYMENT_PENDING' => scheme.tertiary,
      _ => scheme.error,
    };
  }

  static String? formatPaymentDate(String? date) => AppDateFormat.formatIsoDate(date);
}
