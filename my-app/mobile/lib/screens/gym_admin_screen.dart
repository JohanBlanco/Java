import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config/app_constants.dart';
import '../config/role_helper.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';
import '../utils/list_filter.dart';
import '../widgets/list_filter_field.dart';
import '../widgets/multi_select_field.dart';

enum AdminSection { membresias, usuarios }

extension AdminSectionX on AdminSection {
  String get label => switch (this) {
        AdminSection.membresias => 'Membresías',
        AdminSection.usuarios => 'Usuarios',
      };

  String get description => switch (this) {
        AdminSection.membresias => 'Planes de acceso y actividades incluidas',
        AdminSection.usuarios => 'Personal y miembros del gimnasio',
      };

  IconData get icon => switch (this) {
        AdminSection.membresias => Icons.card_membership_outlined,
        AdminSection.usuarios => Icons.people_outline,
      };
}

class GymAdminScreen extends StatefulWidget {
  const GymAdminScreen({super.key, this.initialSection = AdminSection.membresias});

  final AdminSection initialSection;

  @override
  State<GymAdminScreen> createState() => _GymAdminScreenState();
}

class _GymAdminScreenState extends State<GymAdminScreen> {
  late AdminSection _section;
  List<dynamic> _packages = [];
  List<dynamic> _users = [];
  bool _loading = true;
  String _listFilterQuery = '';

  int? _selectedPackageId;
  int? _selectedUserId;
  bool _unlimitedFreeActivities = true;
  int? _selectedMembershipId;

  final _pkgName = TextEditingController();
  final _pkgPrice = TextEditingController();
  final _pkgDesc = TextEditingController();
  final _pkgFreeQuota = TextEditingController();
  final _addonName = TextEditingController();
  final _addonPrice = TextEditingController();

  final _userFirst = TextEditingController();
  final _userLast = TextEditingController();
  final _userEmail = TextEditingController();
  final _userPassword = TextEditingController(text: AppConstants.defaultPassword);
  final Set<String> _userRoles = {'INSTRUCTOR'};

  @override
  void initState() {
    super.initState();
    _section = widget.initialSection;
    _load();
  }

  @override
  void dispose() {
    _pkgName.dispose();
    _pkgPrice.dispose();
    _pkgDesc.dispose();
    _pkgFreeQuota.dispose();
    _addonName.dispose();
    _addonPrice.dispose();
    _userFirst.dispose();
    _userLast.dispose();
    _userEmail.dispose();
    _userPassword.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final api = context.read<AuthProvider>().api;
      final results = await Future.wait([
        api.getPackages(),
        api.getUsers(),
      ]);
      if (mounted) {
        setState(() {
          _packages = results[0];
          _users = results[1];
        });
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  bool get _canSaveUser => _userRoles.isNotEmpty;

  void _changeSection(AdminSection section) {
    setState(() {
      _section = section;
      _listFilterQuery = '';
      _resetPackageForm();
      _resetUserForm();
    });
  }

  void _resetPackageForm() {
    _selectedPackageId = null;
    _unlimitedFreeActivities = true;
    _pkgName.clear();
    _pkgPrice.clear();
    _pkgDesc.clear();
    _pkgFreeQuota.clear();
    _addonName.clear();
    _addonPrice.clear();
  }

  void _resetUserForm() {
    _selectedUserId = null;
    _selectedMembershipId = null;
    _userFirst.clear();
    _userLast.clear();
    _userEmail.clear();
    _userPassword.text = AppConstants.defaultPassword;
    _userRoles
      ..clear()
      ..add('INSTRUCTOR');
  }

  void _selectPackage(Map<String, dynamic> p) {
    final addons = (p['addons'] as List<dynamic>?) ?? [];
    final firstAddon = addons.isNotEmpty ? addons.first as Map<String, dynamic> : null;
    setState(() {
      _selectedPackageId = (p['id'] as num).toInt();
      _pkgName.text = p['name'] ?? '';
      _pkgPrice.text = '${p['price'] ?? ''}';
      _pkgDesc.text = p['description'] ?? '';
      _unlimitedFreeActivities = p['freeActivityQuota'] == null;
      _pkgFreeQuota.text = p['freeActivityQuota'] != null ? '${p['freeActivityQuota']}' : '';
      _addonName.text = firstAddon?['name'] ?? '';
      _addonPrice.text = firstAddon != null ? '${firstAddon['price'] ?? ''}' : '';
    });
  }

  void _selectUser(Map<String, dynamic> u) {
    setState(() {
      _selectedUserId = (u['id'] as num).toInt();
      _userFirst.text = u['firstName'] ?? '';
      _userLast.text = u['lastName'] ?? '';
      _userEmail.text = u['email'] ?? '';
      _userPassword.clear();
      _userRoles
        ..clear()
        ..addAll(
          RoleHelper.normalizeRoles(u)
              .where((role) => RoleHelper.gymRoles.contains(role)),
        );
      if (_userRoles.isEmpty) _userRoles.add('INSTRUCTOR');
    });
  }

  Future<void> _savePackage() async {
    try {
      final api = context.read<AuthProvider>().api;
      final addons = _addonName.text.trim().isNotEmpty
          ? [
              {
                'name': _addonName.text.trim(),
                'description': '',
                'price': double.tryParse(_addonPrice.text) ?? 0,
              }
            ]
          : <dynamic>[];
      final payload = {
        'name': _pkgName.text.trim(),
        'description': _pkgDesc.text.trim(),
        'price': double.tryParse(_pkgPrice.text) ?? 0,
        'durationMonths': 1,
        'freeActivityQuota': _unlimitedFreeActivities ? null : int.tryParse(_pkgFreeQuota.text) ?? 0,
        'addons': addons,
      };
      if (_selectedPackageId != null) {
        await api.updatePackage(_selectedPackageId!, payload);
      } else {
        await api.createPackage(payload);
      }
      _resetPackageForm();
      await _load();
    } on ApiException catch (e) {
      _showError(e.message);
    }
  }

  Future<void> _saveUser() async {
    try {
      if (_userRoles.isEmpty) {
        _showError('Selecciona al menos un rol');
        return;
      }
      final api = context.read<AuthProvider>().api;
      final payload = <String, dynamic>{
        'firstName': _userFirst.text.trim(),
        'lastName': _userLast.text.trim(),
        'email': _userEmail.text.trim(),
        'roles': _userRoles.toList(),
      };
      if (_selectedUserId == null) {
        payload['password'] = _userPassword.text.isEmpty ? AppConstants.defaultPassword : _userPassword.text;
        if (_userRoles.contains('MEMBER') && _selectedMembershipId != null) {
          payload['membershipPackageId'] = _selectedMembershipId;
        }
        await api.createUser(payload);
      } else {
        if (_userPassword.text.isNotEmpty) {
          payload['password'] = _userPassword.text;
        }
        await api.updateUser(_selectedUserId!, payload);
      }
      _resetUserForm();
      await _load();
    } on ApiException catch (e) {
      _showError(e.message);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Widget _sectionNav() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Administración', style: Theme.of(context).textTheme.titleSmall),
          const SizedBox(height: 4),
          ...AdminSection.values.map((s) {
            final selected = _section == s;
            return Padding(
              padding: const EdgeInsets.only(left: 8),
              child: ListTile(
                dense: true,
                contentPadding: const EdgeInsets.symmetric(horizontal: 8),
                leading: Icon(
                  s.icon,
                  size: 20,
                  color: selected ? Theme.of(context).colorScheme.primary : null,
                ),
                title: Text(
                  s.label,
                  style: TextStyle(
                    fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
                    color: selected ? Theme.of(context).colorScheme.primary : null,
                  ),
                ),
                selected: selected,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                onTap: () => _changeSection(s),
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _sectionHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(_section.label, style: Theme.of(context).textTheme.titleLarge),
          Text(
            _section.description,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
          ),
        ],
      ),
    );
  }

  Widget _splitLayout({required Widget list, required Widget form}) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final wide = constraints.maxWidth >= 720;
        if (wide) {
          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(flex: 3, child: list),
              const SizedBox(width: 12),
              Expanded(flex: 2, child: form),
            ],
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            list,
            const SizedBox(height: 16),
            form,
          ],
        );
      },
    );
  }

  Widget _formCard({
    required String title,
    required VoidCallback onReset,
    required bool isEditing,
    required Widget child,
  }) {
    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
                if (isEditing)
                  TextButton(onPressed: onReset, child: const Text('Nuevo')),
              ],
            ),
            const SizedBox(height: 12),
            child,
          ],
        ),
      ),
    );
  }

  Widget _selectableCard({
    required bool selected,
    required VoidCallback onTap,
    required Widget child,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Card(
        color: selected ? Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.35) : null,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(
            color: selected ? Theme.of(context).colorScheme.primary : Colors.transparent,
            width: 2,
          ),
        ),
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: onTap,
          child: child,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _sectionNav(),
        const Divider(height: 1),
        _sectionHeader(),
        Expanded(
          child: RefreshIndicator(
            onRefresh: _load,
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              child: switch (_section) {
                AdminSection.membresias => _packagesContent(),
                AdminSection.usuarios => _usersContent(),
              },
            ),
          ),
        ),
      ],
    );
  }

  Widget _packagesContent() {
    final filtered = filterByQuery(_packages, _listFilterQuery);
    final list = _packages.isEmpty
        ? const Padding(
            padding: EdgeInsets.all(16),
            child: Card(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: Text('No hay membresías registradas.')),
              ),
            ),
          )
        : Column(
            children: [
              ListFilterField(
                onChanged: (v) => setState(() => _listFilterQuery = v),
                resultCount: filtered.length,
                totalCount: _packages.length,
              ),
              if (filtered.isEmpty)
                const Card(
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: Text('Ningún resultado coincide con la búsqueda')),
                  ),
                )
              else
                ...filtered.map((p) {
              final id = (p['id'] as num).toInt();
              final addons = (p['addons'] as List<dynamic>?) ?? [];
              return _selectableCard(
                selected: _selectedPackageId == id,
                onTap: () => _selectPackage(p as Map<String, dynamic>),
                child: ListTile(
                  leading: const Icon(Icons.fitness_center),
                  title: Text(p['name'] ?? ''),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if ((p['description'] ?? '').toString().isNotEmpty) Text(p['description'] ?? ''),
                      Text(
                        p['freeActivityQuota'] == null
                            ? 'Actividades gratis: ilimitadas'
                            : 'Actividades gratis: ${p['freeActivityQuota']}/mes',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                      if (addons.isNotEmpty)
                        ...addons.map((a) => Text('+ ${a['name']} (\$${a['price']})')),
                    ],
                  ),
                  trailing: Text('\$${p['price']}/mes'),
                ),
              );
            }),
            ],
          );

    final form = _formCard(
      title: _selectedPackageId != null ? 'Editar membresía' : 'Nueva membresía',
      isEditing: _selectedPackageId != null,
      onReset: () => setState(_resetPackageForm),
      child: Column(
        children: [
          TextField(controller: _pkgName, decoration: const InputDecoration(labelText: 'Nombre', border: OutlineInputBorder())),
          const SizedBox(height: 8),
          TextField(controller: _pkgPrice, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Precio mensual', border: OutlineInputBorder())),
          const SizedBox(height: 8),
          TextField(controller: _pkgDesc, decoration: const InputDecoration(labelText: 'Descripción', border: OutlineInputBorder())),
          const SizedBox(height: 8),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Actividades gratuitas ilimitadas'),
            value: _unlimitedFreeActivities,
            onChanged: (v) => setState(() => _unlimitedFreeActivities = v),
          ),
          if (!_unlimitedFreeActivities) ...[
            TextField(
              controller: _pkgFreeQuota,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Actividades gratuitas incluidas (por mes)', border: OutlineInputBorder()),
            ),
            const SizedBox(height: 8),
          ],
          TextField(controller: _addonName, decoration: const InputDecoration(labelText: 'Complemento (opcional)', border: OutlineInputBorder())),
          const SizedBox(height: 8),
          TextField(controller: _addonPrice, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Precio del complemento', border: OutlineInputBorder())),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: _savePackage,
            child: Text(_selectedPackageId != null ? 'Guardar cambios' : 'Crear membresía'),
          ),
        ],
      ),
    );

    return _splitLayout(list: list, form: form);
  }

  Widget _usersContent() {
    final filtered = filterByQuery(
      _users,
      _listFilterQuery,
      extraValues: (u) {
        final user = u as Map<String, dynamic>;
        final roles = RoleHelper.normalizeRoles(user);
        final status = user['membershipStatus'] as String?;
        return [
          ...roles.map((role) => RoleHelper.roleLabels[role] ?? role),
          if (status != null) RoleHelper.membershipStatusLabels[status] ?? status,
          if (user['membershipPackageName'] != null) user['membershipPackageName'].toString(),
        ];
      },
    );
    final list = _users.isEmpty
        ? const Padding(
            padding: EdgeInsets.all(16),
            child: Card(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: Text('No hay usuarios registrados.')),
              ),
            ),
          )
        : Column(
            children: [
              ListFilterField(
                onChanged: (v) => setState(() => _listFilterQuery = v),
                resultCount: filtered.length,
                totalCount: _users.length,
              ),
              if (filtered.isEmpty)
                const Card(
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Center(child: Text('Ningún resultado coincide con la búsqueda')),
                  ),
                )
              else
                ...filtered.map((u) {
              final id = (u['id'] as num).toInt();
              final roles = RoleHelper.normalizeRoles(u as Map<String, dynamic>);
              final isMember = roles.contains('MEMBER');
              final membershipStatus = u['membershipStatus'] as String?;
              final nextPaymentDate = u['nextPaymentDate'] as String?;
              final packageName = u['membershipPackageName'] as String?;
              final subtitleLines = <String>[u['email'] ?? ''];
              if (isMember && membershipStatus == 'PAYMENT_PENDING') {
                if (nextPaymentDate != null) {
                  subtitleLines.add('Venció el ${RoleHelper.formatPaymentDate(nextPaymentDate)}');
                }
                subtitleLines.add('Plan: ${packageName?.isNotEmpty == true ? packageName : 'Sin plan asignado'}');
              } else if (isMember && membershipStatus == 'ACTIVE') {
                if (nextPaymentDate != null) {
                  subtitleLines.add('Próximo pago: ${RoleHelper.formatPaymentDate(nextPaymentDate)}');
                }
                if (packageName != null && packageName.isNotEmpty) {
                  subtitleLines.add('Plan: $packageName');
                }
              }
              return _selectableCard(
                selected: _selectedUserId == id,
                onTap: () => _selectUser(u as Map<String, dynamic>),
                child: ListTile(
                  leading: const Icon(Icons.person_outline),
                  title: Text('${u['firstName']} ${u['lastName']}'),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: subtitleLines.map((line) => Text(line)).toList(),
                  ),
                  trailing: Wrap(
                    spacing: 4,
                    runSpacing: 4,
                    children: [
                      ...roles
                          .where((role) => RoleHelper.gymRoles.contains(role))
                          .map((role) => Chip(label: Text(RoleHelper.roleLabels[role] ?? role))),
                      if (isMember && membershipStatus != null)
                        Chip(
                          label: Text(
                            RoleHelper.membershipStatusLabels[membershipStatus] ?? membershipStatus,
                            style: TextStyle(color: RoleHelper.membershipStatusColor(context, membershipStatus)),
                          ),
                        ),
                    ],
                  ),
                ),
              );
            }),
            ],
          );

    final form = _formCard(
      title: _selectedUserId != null ? 'Editar usuario' : 'Nuevo usuario',
      isEditing: _selectedUserId != null,
      onReset: () => setState(_resetUserForm),
      child: Column(
        children: [
          if (_selectedUserId == null)
            Text(
              'Asigna uno o más roles. Cada perfil habilita funciones distintas en la app.',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          if (_selectedUserId == null) const SizedBox(height: 12),
          MultiSelectField(
            label: 'Roles',
            options: {
              for (final role in RoleHelper.gymRoles) role: RoleHelper.roleLabels[role] ?? role,
            },
            selected: _userRoles,
            onChanged: (value) => setState(() {
              _userRoles
                ..clear()
                ..addAll(value);
            }),
            emptyLabel: 'Seleccionar roles...',
          ),
          if (!_canSaveUser)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(
                'Selecciona al menos un rol',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Theme.of(context).colorScheme.error),
              ),
            ),
          const SizedBox(height: 12),
          if (_userRoles.contains('MEMBER')) ...[
            DropdownButtonFormField<int?>(
              value: _selectedMembershipId,
              decoration: const InputDecoration(labelText: 'Membresía (miembros)', border: OutlineInputBorder()),
              items: [
                const DropdownMenuItem<int?>(value: null, child: Text('Sin asignar')),
                ..._packages.map((p) => DropdownMenuItem<int?>(
                      value: (p['id'] as num).toInt(),
                      child: Text(p['name'] ?? ''),
                    )),
              ],
              onChanged: (v) => setState(() => _selectedMembershipId = v),
            ),
            const SizedBox(height: 12),
          ],
          TextField(controller: _userFirst, decoration: const InputDecoration(labelText: 'Nombre', border: OutlineInputBorder())),
          const SizedBox(height: 8),
          TextField(controller: _userLast, decoration: const InputDecoration(labelText: 'Apellido', border: OutlineInputBorder())),
          const SizedBox(height: 8),
          TextField(controller: _userEmail, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Correo de acceso', border: OutlineInputBorder())),
          const SizedBox(height: 8),
          TextField(
            controller: _userPassword,
            decoration: InputDecoration(
              labelText: 'Contraseña',
              hintText: _selectedUserId != null ? 'Dejar vacío para no cambiar' : null,
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: _canSaveUser ? _saveUser : null,
            child: Text(_selectedUserId != null ? 'Guardar cambios' : 'Crear usuario'),
          ),
        ],
      ),
    );

    return _splitLayout(list: list, form: form);
  }
}
