import 'package:flutter/material.dart';
import 'user_menu_button.dart';

class AppSidebar extends StatelessWidget {
  const AppSidebar({
    super.key,
    required this.user,
    required this.activeRole,
    required this.onActiveRoleChange,
    required this.onLogout,
    this.onProfile,
    this.title = 'GymPlatform',
    this.children = const [],
  });

  final Map<String, dynamic> user;
  final String? activeRole;
  final ValueChanged<String> onActiveRoleChange;
  final VoidCallback onLogout;
  final VoidCallback? onProfile;
  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Material(
      color: colorScheme.surface,
      child: SafeArea(
        child: SizedBox(
          width: 260,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                child: Text(
                  title,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        color: colorScheme.primary,
                        fontWeight: FontWeight.w700,
                      ),
                ),
              ),
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                child: Divider(height: 1),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                child: UserMenuButton(
                  user: user,
                  activeRole: activeRole,
                  onActiveRoleChange: onActiveRoleChange,
                  onLogout: onLogout,
                  onProfile: onProfile,
                  fullWidth: true,
                ),
              ),
              const Divider(height: 1),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  children: children,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
