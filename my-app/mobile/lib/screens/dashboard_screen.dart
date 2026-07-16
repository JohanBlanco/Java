import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config/role_helper.dart';
import '../providers/auth_provider.dart';
import '../utils/appointment_labels.dart';
import '../utils/list_filter.dart';
import '../widgets/list_filter_field.dart';

enum OpsPanel { actividadesHoy, pagosPendientes, solicitudesRutina, solicitudesCitas }

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  OpsPanel _panel = OpsPanel.actividadesHoy;
  List<dynamic> _todayActivities = [];
  List<dynamic> _routineRequests = [];
  List<dynamic> _appointmentRequests = [];
  List<dynamic> _pendingPayments = [];
  int _activities = 0;
  int _reservations = 0;
  int _routines = 0;
  bool _loading = true;
  String _filterQuery = '';

  String get _todayIso {
    final n = DateTime.now();
    return '${n.year}-${n.month.toString().padLeft(2, '0')}-${n.day.toString().padLeft(2, '0')}';
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final auth = context.read<AuthProvider>();
    final api = auth.api;
    final activeRole = auth.resolvedActiveRole;
    final showOps = RoleHelper.canViewReception(activeRole);
    final isMember = RoleHelper.isMemberView(activeRole);
    final isStaff = RoleHelper.isStaffView(activeRole);

    try {
      if (showOps) {
        final results = await Future.wait([
          api.getActivities(from: _todayIso, to: _todayIso),
          api.getPendingMembershipPayment(),
          api.getRoutineRequests(),
          api.getAppointmentRequests(),
        ]);
        if (mounted) {
          setState(() {
            _todayActivities = results[0] as List<dynamic>;
            _pendingPayments = results[1] as List<dynamic>;
            _routineRequests = results[2] as List<dynamic>;
            _appointmentRequests = results[3] as List<dynamic>;
            _loading = false;
          });
        }
      } else if (isMember) {
        final results = await Future.wait([
          api.getActivities(),
          api.getMyReservations(),
          api.getMyRoutines(),
        ]);
        final reservations = results[1] as List<dynamic>;
        if (mounted) {
          setState(() {
            _activities = (results[0] as List).length;
            _reservations =
                reservations.where((r) => r['status'] != 'CANCELLED').length;
            _routines = (results[2] as List).length;
            _loading = false;
          });
        }
      } else if (isStaff) {
        final requests = await api.getRoutineRequests();
        if (mounted) {
          setState(() {
            _routineRequests = requests;
            _panel = OpsPanel.solicitudesRutina;
            _loading = false;
          });
        }
      } else if (mounted) {
        setState(() => _loading = false);
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  int get _pendingRoutineCount =>
      _routineRequests.where((r) => r['status'] == 'PENDING').length;

  int get _pendingAppointmentCount =>
      _appointmentRequests.where((r) => r['status'] == 'PENDING').length;

  void _selectPanel(OpsPanel panel) {
    setState(() {
      _panel = panel;
      _filterQuery = '';
    });
  }

  Widget _filterBar(List<dynamic> source, List<dynamic> filtered) {
    if (source.isEmpty) return const SizedBox.shrink();
    return ListFilterField(
      onChanged: (v) => setState(() => _filterQuery = v),
      resultCount: filtered.length,
      totalCount: source.length,
    );
  }

  Widget _noMatchMessage() {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 24),
      child: Center(child: Text('Ningún resultado coincide con la búsqueda')),
    );
  }

  Widget _statCard(String value, String label, {bool selected = false, VoidCallback? onTap}) {
    return Card(
      elevation: selected ? 2 : 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: selected ? Theme.of(context).colorScheme.primary : Colors.transparent,
          width: 2,
        ),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            children: [
              Text(value, style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 4),
              Text(label, textAlign: TextAlign.center),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTodayActivities() {
    if (_todayActivities.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 24),
        child: Center(child: Text('No hay actividades hoy')),
      );
    }
    final filtered = filterByQuery(_todayActivities, _filterQuery);
    return Column(
      children: [
        _filterBar(_todayActivities, filtered),
        if (filtered.isEmpty)
          _noMatchMessage()
        else
          ...filtered.map((a) {
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            title: Text(a['name'] ?? ''),
            subtitle: Text('${a['startTime']} · ${a['locationName'] ?? ''}'),
            trailing: Text('${a['confirmedReservations'] ?? 0} conf.'),
          ),
        );
      }),
      ],
    );
  }

  Widget _buildPendingPayments() {
    if (_pendingPayments.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 24),
        child: Center(child: Text('No hay miembros pendientes de pago')),
      );
    }
    final filtered = filterByQuery(
      _pendingPayments,
      _filterQuery,
      extraValues: (u) {
        final user = u as Map<String, dynamic>;
        final packageName = user['membershipPackageName']?.toString() ?? '';
        final expiredDate = RoleHelper.formatPaymentDate(user['nextPaymentDate']?.toString()) ?? '';
        return [user['email']?.toString() ?? '', packageName, expiredDate];
      },
    );
    return Column(
      children: [
        _filterBar(_pendingPayments, filtered),
        if (filtered.isEmpty)
          _noMatchMessage()
        else
          ...filtered.map((u) {
        final expiredDate = RoleHelper.formatPaymentDate(u['nextPaymentDate']?.toString());
        final packageName = u['membershipPackageName']?.toString();
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            title: Text('${u['firstName']} ${u['lastName']}'),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(u['email'] ?? ''),
                if (expiredDate != null) Text('Venció el $expiredDate'),
                Text('Plan: ${packageName?.isNotEmpty == true ? packageName : 'Sin plan asignado'}'),
              ],
            ),
            trailing: Chip(
              label: Text(
                RoleHelper.membershipStatusLabels['PAYMENT_PENDING'] ?? 'Pendiente de pago',
                style: TextStyle(color: RoleHelper.membershipStatusColor(context, 'PAYMENT_PENDING')),
              ),
            ),
          ),
        );
      }),
      ],
    );
  }

  Widget _buildRoutineRequests() {
    if (_routineRequests.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 24),
        child: Center(child: Text('Sin solicitudes')),
      );
    }
    final filtered = filterByQuery(_routineRequests, _filterQuery);
    return Column(
      children: [
        _filterBar(_routineRequests, filtered),
        if (filtered.isEmpty)
          _noMatchMessage()
        else
          ...filtered.map((r) {
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(r['memberName'], style: Theme.of(context).textTheme.titleMedium),
                    Chip(label: Text(r['status'])),
                  ],
                ),
                const SizedBox(height: 8),
                Text(r['description']),
                Text('Objetivos: ${r['goals']}', style: Theme.of(context).textTheme.bodySmall),
                if (r['status'] == 'PENDING') ...[
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: () async {
                      await context.read<AuthProvider>().api
                          .updateRoutineRequestStatus(r['id'], 'IN_PROGRESS');
                      _load();
                    },
                    child: const Text('Tomar solicitud'),
                  ),
                ],
              ],
            ),
          ),
        );
      }),
      ],
    );
  }

  Widget _buildAppointmentRequests() {
    if (_appointmentRequests.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 24),
        child: Center(child: Text('Sin solicitudes de cita')),
      );
    }
    final filtered = filterByQuery(
      _appointmentRequests,
      _filterQuery,
      extraValues: (r) => [appointmentTypeLabel(r['type'])],
    );
    return Column(
      children: [
        _filterBar(_appointmentRequests, filtered),
        if (filtered.isEmpty)
          _noMatchMessage()
        else
          ...filtered.map((r) {
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(r['memberName'], style: Theme.of(context).textTheme.titleMedium),
                    Chip(label: Text(r['status'])),
                  ],
                ),
                const SizedBox(height: 8),
                Text('Tipo: ${appointmentTypeLabel(r['type'])}'),
                if ((r['notes'] ?? '').toString().isNotEmpty) Text(r['notes']),
                if (r['status'] == 'PENDING') ...[
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    children: [
                      FilledButton(
                        onPressed: () async {
                          await context.read<AuthProvider>().api
                              .updateAppointmentRequestStatus(r['id'], 'SCHEDULED');
                          _load();
                        },
                        child: const Text('Agendar'),
                      ),
                      OutlinedButton(
                        onPressed: () async {
                          await context.read<AuthProvider>().api
                              .updateAppointmentRequestStatus(r['id'], 'CANCELLED');
                          _load();
                        },
                        child: const Text('Cancelar'),
                      ),
                    ],
                  ),
                ],
                if (r['status'] == 'SCHEDULED') ...[
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: () async {
                      await context.read<AuthProvider>().api
                          .updateAppointmentRequestStatus(r['id'], 'COMPLETED');
                      _load();
                    },
                    child: const Text('Marcar completada'),
                  ),
                ],
              ],
            ),
          ),
        );
      }),
      ],
    );
  }

  Widget _buildOpsPanel() {
    final title = switch (_panel) {
      OpsPanel.actividadesHoy => 'Actividades del día',
      OpsPanel.pagosPendientes => 'Pendientes de pago',
      OpsPanel.solicitudesRutina => 'Solicitudes de rutina',
      OpsPanel.solicitudesCitas => 'Solicitudes de cita',
    };

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            switch (_panel) {
              OpsPanel.actividadesHoy => _buildTodayActivities(),
              OpsPanel.pagosPendientes => _buildPendingPayments(),
              OpsPanel.solicitudesRutina => _buildRoutineRequests(),
              OpsPanel.solicitudesCitas => _buildAppointmentRequests(),
            },
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final user = auth.user!;
    final activeRole = auth.resolvedActiveRole;
    final showOps = RoleHelper.canViewReception(activeRole);
    final isMember = RoleHelper.isMemberView(activeRole);
    final isStaff = RoleHelper.isStaffView(activeRole);

    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Hola, ${user['firstName']}',
              style: Theme.of(context).textTheme.headlineSmall),
          Text('Perfil: ${RoleHelper.profileLabel(activeRole)}'),
          const SizedBox(height: 24),
          if (showOps) ...[
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 1.4,
              children: [
                _statCard(
                  '${_todayActivities.length}',
                  'Actividades del día',
                  selected: _panel == OpsPanel.actividadesHoy,
                  onTap: () => _selectPanel(OpsPanel.actividadesHoy),
                ),
                _statCard(
                  '${_pendingPayments.length}',
                  'Pendientes de pago',
                  selected: _panel == OpsPanel.pagosPendientes,
                  onTap: () => _selectPanel(OpsPanel.pagosPendientes),
                ),
                _statCard(
                  '$_pendingRoutineCount',
                  'Solicitudes de rutina',
                  selected: _panel == OpsPanel.solicitudesRutina,
                  onTap: () => _selectPanel(OpsPanel.solicitudesRutina),
                ),
                _statCard(
                  '$_pendingAppointmentCount',
                  'Solicitudes de cita',
                  selected: _panel == OpsPanel.solicitudesCitas,
                  onTap: () => _selectPanel(OpsPanel.solicitudesCitas),
                ),
              ],
            ),
            const SizedBox(height: 16),
            _buildOpsPanel(),
          ],
          if (isStaff && !showOps) ...[
            _statCard(
              '$_pendingRoutineCount',
              'Solicitudes pendientes',
              selected: true,
            ),
            const SizedBox(height: 16),
            _buildOpsPanel(),
          ],
          if (isMember)
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 1.4,
              children: [
                _statCard('$_activities', 'Actividades'),
                _statCard('$_reservations', 'Mis reservaciones'),
                _statCard('$_routines', 'Mis rutinas'),
              ],
            ),
        ],
      ),
    );
  }
}
