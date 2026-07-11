import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config/role_helper.dart';
import '../providers/theme_provider.dart';

class UserMenuButton extends StatelessWidget {
  const UserMenuButton({
    super.key,
    required this.user,
    required this.activeRole,
    required this.onActiveRoleChange,
    required this.onLogout,
    this.onProfile,
    this.fullWidth = false,
  });

  final Map<String, dynamic> user;
  final String? activeRole;
  final ValueChanged<String> onActiveRoleChange;
  final VoidCallback onLogout;
  final VoidCallback? onProfile;
  final bool fullWidth;

  String get _initials {
    final first = user['firstName']?.toString().trim() ?? '';
    final last = user['lastName']?.toString().trim() ?? '';
    final a = first.isNotEmpty ? first.substring(0, 1) : '';
    final b = last.isNotEmpty ? last.substring(0, 1) : '';
    return '$a$b'.toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    final showProfile = RoleHelper.canViewProfile(activeRole) && onProfile != null;
    final firstName = user['firstName']?.toString() ?? '';
    final lastName = user['lastName']?.toString() ?? '';
    final fullName = '$firstName $lastName'.trim();
    final currentLabel = RoleHelper.profileLabel(activeRole);
    final switchableRoles = RoleHelper.getSwitchableRoles(user);
    final colorScheme = Theme.of(context).colorScheme;
    final themeProvider = context.watch<ThemeProvider>();
    final isDark = themeProvider.isDark;

    return PopupMenuButton<String>(
      tooltip: fullName.isEmpty ? 'Cuenta' : '$fullName · $currentLabel',
      offset: Offset(0, fullWidth ? 48 : 48),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: colorScheme.outlineVariant),
      ),
      onSelected: (value) {
        if (value == 'logout') {
          onLogout();
        } else if (value == 'profile') {
          onProfile?.call();
        } else if (value.startsWith('role:')) {
          onActiveRoleChange(value.substring(5));
        } else if (value == 'theme:dark') {
          themeProvider.setTheme(ThemeMode.dark);
        } else if (value == 'theme:light') {
          themeProvider.setTheme(ThemeMode.light);
        }
      },
      itemBuilder: (context) => [
        if (switchableRoles.length > 1) ...[
          const PopupMenuItem(
            enabled: false,
            height: 36,
            child: Text(
              'Perfil actual',
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
            ),
          ),
          for (final role in switchableRoles)
            PopupMenuItem(
              value: 'role:$role',
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    RoleHelper.roleLabels[role] ?? role,
                    style: TextStyle(
                      fontWeight: role == activeRole ? FontWeight.w600 : FontWeight.normal,
                      color: role == activeRole ? colorScheme.primary : null,
                    ),
                  ),
                  if (role == activeRole)
                    Icon(Icons.check, size: 18, color: colorScheme.primary),
                ],
              ),
            ),
          const PopupMenuDivider(),
        ],
        if (showProfile)
          const PopupMenuItem(value: 'profile', child: Text('Ver perfil')),
        const PopupMenuItem(
          enabled: false,
          height: 36,
          child: Text(
            'Tema',
            style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
          ),
        ),
        PopupMenuItem(
          value: 'theme:dark',
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Oscuro'),
              if (isDark) Icon(Icons.check, size: 18, color: colorScheme.primary),
            ],
          ),
        ),
        PopupMenuItem(
          value: 'theme:light',
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Claro'),
              if (!isDark) Icon(Icons.check, size: 18, color: colorScheme.primary),
            ],
          ),
        ),
        const PopupMenuDivider(),
        const PopupMenuItem(value: 'logout', child: Text('Cerrar sesión')),
      ],
      child: Container(
        width: fullWidth ? double.infinity : null,
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: colorScheme.surfaceContainerHighest,
          border: Border.all(color: colorScheme.outlineVariant),
          borderRadius: BorderRadius.circular(fullWidth ? 10 : 999),
        ),
        child: Row(
          mainAxisSize: fullWidth ? MainAxisSize.max : MainAxisSize.min,
          children: [
            CircleAvatar(
              radius: 16,
              backgroundColor: colorScheme.primary,
              foregroundColor: colorScheme.onPrimary,
              child: Text(
                _initials,
                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    fullWidth ? fullName : firstName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                  Text(
                    currentLabel,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            Icon(Icons.arrow_drop_down, color: colorScheme.onSurfaceVariant),
          ],
        ),
      ),
    );
  }
}
