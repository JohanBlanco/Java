import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../config/role_helper.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final _birthYearController = TextEditingController();
  final _ageController = TextEditingController();
  final _goalsController = TextEditingController();
  final _phoneController = TextEditingController();
  bool _loading = true;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = context.read<AuthProvider>().api;
    final user = await api.getMe();
    final profile = user['profile'];
    if (profile != null) {
      _birthYearController.text = profile['birthYear']?.toString() ?? '';
      _ageController.text = profile['age']?.toString() ?? '';
      _goalsController.text = profile['goals'] ?? '';
      _phoneController.text = profile['phone'] ?? '';
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    final api = context.read<AuthProvider>().api;
    await api.updateProfile({
      'birthYear': _birthYearController.text.isNotEmpty ? int.parse(_birthYearController.text) : null,
      'age': _ageController.text.isNotEmpty ? int.parse(_ageController.text) : null,
      'goals': _goalsController.text,
      'phone': _phoneController.text,
    });
    if (mounted) {
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Perfil actualizado')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    final user = context.watch<AuthProvider>().user!;

    final roles = RoleHelper.normalizeRoles(user);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('${user['firstName']} ${user['lastName']}', style: Theme.of(context).textTheme.headlineSmall),
          Text(user['email'], style: Theme.of(context).textTheme.bodyMedium),
          Text('Perfiles: ${RoleHelper.formatRoles(roles)}', style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 24),
          TextField(
            controller: _birthYearController,
            decoration: const InputDecoration(labelText: 'Año de nacimiento', border: OutlineInputBorder()),
            keyboardType: TextInputType.number,
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _ageController,
            decoration: const InputDecoration(labelText: 'Edad', border: OutlineInputBorder()),
            keyboardType: TextInputType.number,
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _phoneController,
            decoration: const InputDecoration(labelText: 'Teléfono', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _goalsController,
            decoration: const InputDecoration(labelText: 'Objetivos', border: OutlineInputBorder()),
            maxLines: 3,
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _saving ? null : _save,
              child: _saving
                  ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Guardar perfil'),
            ),
          ),
        ],
      ),
    );
  }
}
