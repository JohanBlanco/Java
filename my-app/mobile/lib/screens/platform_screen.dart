import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config/app_constants.dart';
import '../providers/auth_provider.dart';
import '../providers/layout_provider.dart';
import '../services/api_service.dart';
import '../utils/list_filter.dart';
import '../widgets/app_sidebar.dart';
import '../widgets/collapsible_sidebar_shell.dart';
import '../widgets/list_filter_field.dart';

class PlatformScreen extends StatefulWidget {
  const PlatformScreen({super.key});

  @override
  State<PlatformScreen> createState() => _PlatformScreenState();
}

class _PlatformScreenState extends State<PlatformScreen> {
  List<dynamic> _orgs = [];
  int? _selectedId;
  bool _loading = true;
  bool _saving = false;
  String _filterQuery = '';
  final _scaffoldKey = GlobalKey<ScaffoldState>();

  final _nameCtrl = TextEditingController();
  final _slugCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _ownerFirstCtrl = TextEditingController();
  final _ownerLastCtrl = TextEditingController();
  final _ownerEmailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController(text: AppConstants.defaultPassword);
  String _subscriptionStatus = 'ACTIVE';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _slugCtrl.dispose();
    _phoneCtrl.dispose();
    _ownerFirstCtrl.dispose();
    _ownerLastCtrl.dispose();
    _ownerEmailCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final api = context.read<AuthProvider>().api;
      final orgs = await api.getPlatformOrganizations();
      if (mounted) setState(() => _orgs = orgs);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _resetForm() {
    setState(() => _selectedId = null);
    _nameCtrl.clear();
    _slugCtrl.clear();
    _phoneCtrl.clear();
    _ownerFirstCtrl.clear();
    _ownerLastCtrl.clear();
    _ownerEmailCtrl.clear();
    _passwordCtrl.text = AppConstants.defaultPassword;
    _subscriptionStatus = 'ACTIVE';
  }

  void _populateForm(Map<String, dynamic> org) {
    _nameCtrl.text = org['name'] ?? '';
    _slugCtrl.text = org['slug'] ?? '';
    _phoneCtrl.text = org['contactPhone'] ?? '';
    _ownerFirstCtrl.text = org['ownerFirstName'] ?? '';
    _ownerLastCtrl.text = org['ownerLastName'] ?? '';
    _ownerEmailCtrl.text = org['ownerEmail'] ?? org['contactEmail'] ?? '';
    _passwordCtrl.text = AppConstants.defaultPassword;
    _subscriptionStatus = org['subscriptionStatus'] ?? 'ACTIVE';
  }

  Future<void> _selectOrg(Map<String, dynamic> org) async {
    setState(() => _selectedId = org['id']);
    try {
      final fresh = await context.read<AuthProvider>().api.getPlatformOrganization(org['id']);
      _populateForm(fresh);
    } catch (_) {
      _populateForm(org);
    }
    setState(() {});
  }

  Map<String, dynamic> _buildPayload() => {
        'name': _nameCtrl.text.trim(),
        'slug': _slugCtrl.text.trim(),
        'contactPhone': _phoneCtrl.text.trim(),
        'contactEmail': _ownerEmailCtrl.text.trim(),
        'ownerFirstName': _ownerFirstCtrl.text.trim(),
        'ownerLastName': _ownerLastCtrl.text.trim(),
        'ownerEmail': _ownerEmailCtrl.text.trim(),
        'ownerPassword': _passwordCtrl.text,
        'subscriptionStatus': _subscriptionStatus,
      };

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      final api = context.read<AuthProvider>().api;
      final payload = _buildPayload();
      if (_selectedId != null) {
        await api.updateOrganization(_selectedId!, payload);
      } else {
        await api.createOrganization(payload);
      }
      _resetForm();
      await _load();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Cliente guardado')),
        );
      }
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _toggleStatus(Map<String, dynamic> org) async {
    final newStatus = org['subscriptionStatus'] == 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      await context.read<AuthProvider>().api.updateOrganization(org['id'], {
        'name': org['name'],
        'slug': org['slug'],
        'contactPhone': org['contactPhone'],
        'contactEmail': org['contactEmail'],
        'ownerFirstName': org['ownerFirstName'] ?? 'Administrador',
        'ownerLastName': org['ownerLastName'] ?? org['name'],
        'ownerEmail': org['ownerEmail'] ?? org['contactEmail'],
        'subscriptionStatus': newStatus,
      });
      await _load();
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  Widget _buildContent() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    final filtered = filterByQuery(_orgs, _filterQuery);

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            'Clientes (Gimnasios)',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 4),
          Text(
            'Cada gimnasio tiene su propio administrador y usuarios',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 16),
          if (_orgs.isNotEmpty)
            ListFilterField(
              onChanged: (v) => setState(() => _filterQuery = v),
              resultCount: filtered.length,
              totalCount: _orgs.length,
            ),
          if (_orgs.isEmpty)
            const Card(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: Text('No hay clientes registrados.')),
              ),
            )
          else if (filtered.isEmpty)
            const Card(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: Text('Ningún resultado coincide con la búsqueda')),
              ),
            )
          else
            ...filtered.map((org) {
            final selected = _selectedId == org['id'];
            return Card(
              color: selected ? Theme.of(context).colorScheme.primaryContainer : null,
              child: ListTile(
                title: Text(org['name'] ?? ''),
                subtitle: Text(
                  '${org['slug']}\nAdmin: ${org['ownerEmail'] ?? org['contactEmail']}',
                ),
                isThreeLine: true,
                trailing: PopupMenuButton<String>(
                  onSelected: (action) {
                    if (action == 'edit') {
                      _selectOrg(org);
                    } else if (action == 'toggle') {
                      _toggleStatus(org);
                    }
                  },
                  itemBuilder: (_) => [
                    const PopupMenuItem(value: 'edit', child: Text('Editar')),
                    PopupMenuItem(
                      value: 'toggle',
                      child: Text(org['subscriptionStatus'] == 'ACTIVE' ? 'Suspender' : 'Activar'),
                    ),
                  ],
                ),
                onTap: () => _selectOrg(org),
              ),
            );
          }),
          const Divider(height: 32),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                _selectedId != null ? 'Editar cliente' : 'Nuevo cliente',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              if (_selectedId != null)
                TextButton(onPressed: _resetForm, child: const Text('Nuevo')),
            ],
          ),
          const SizedBox(height: 8),
          _sectionTitle('Datos del gimnasio'),
          TextField(
            controller: _nameCtrl,
            decoration: const InputDecoration(labelText: 'Nombre del gimnasio', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _slugCtrl,
            decoration: const InputDecoration(labelText: 'Slug', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _phoneCtrl,
            decoration: const InputDecoration(labelText: 'Teléfono', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 16),
          _sectionTitle('Cuenta del administrador (login)'),
          TextField(
            controller: _ownerFirstCtrl,
            decoration: const InputDecoration(labelText: 'Nombre', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _ownerLastCtrl,
            decoration: const InputDecoration(labelText: 'Apellido', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _ownerEmailCtrl,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(labelText: 'Correo de acceso', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _passwordCtrl,
            decoration: const InputDecoration(labelText: 'Contraseña', border: OutlineInputBorder()),
          ),
          if (_selectedId != null) ...[
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _subscriptionStatus,
              decoration: const InputDecoration(
                labelText: 'Estado de suscripción',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'ACTIVE', label: Text('ACTIVE')),
                DropdownMenuItem(value: 'TRIAL', label: Text('TRIAL')),
                DropdownMenuItem(value: 'SUSPENDED', label: Text('SUSPENDED')),
                DropdownMenuItem(value: 'INACTIVE', label: Text('INACTIVE')),
              ],
              onChanged: (v) => setState(() => _subscriptionStatus = v ?? 'ACTIVE'),
            ),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _saving ? null : _save,
              child: _saving
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Text(_selectedId != null ? 'Guardar cambios' : 'Crear cliente'),
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final layout = context.watch<LayoutProvider>();
    final user = auth.user!;

    final sidebar = AppSidebar(
      user: user,
      activeRole: auth.resolvedActiveRole,
      onActiveRoleChange: auth.setActiveRole,
      onLogout: auth.logout,
      children: const [
        ListTile(
          leading: Icon(Icons.business_outlined),
          title: Text('Clientes'),
          selected: true,
        ),
      ],
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final wide = constraints.maxWidth >= 768;

        if (wide) {
          return CollapsibleSidebarShell(
            sidebarVisible: layout.sidebarVisible,
            onToggleSidebar: layout.toggleSidebar,
            sidebar: sidebar,
            child: _buildContent(),
          );
        }

        return Scaffold(
          key: _scaffoldKey,
          drawer: Drawer(child: sidebar),
          body: Stack(
            children: [
              _buildContent(),
              Positioned(
                top: MediaQuery.paddingOf(context).top + 8,
                left: 8,
                child: IconButton(
                  tooltip: 'Menú',
                  icon: const Icon(Icons.menu),
                  onPressed: () => _scaffoldKey.currentState?.openDrawer(),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _sectionTitle(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text(text, style: const TextStyle(fontWeight: FontWeight.w600)),
    );
  }
}
