import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';
import '../utils/date_format.dart';

const _weekdays = [
  ('MONDAY', 'Lun'),
  ('TUESDAY', 'Mar'),
  ('WEDNESDAY', 'Mié'),
  ('THURSDAY', 'Jue'),
  ('FRIDAY', 'Vie'),
  ('SATURDAY', 'Sáb'),
  ('SUNDAY', 'Dom'),
];

class ReceptionActividadesScreen extends StatefulWidget {
  const ReceptionActividadesScreen({super.key});

  @override
  State<ReceptionActividadesScreen> createState() => _ReceptionActividadesScreenState();
}

class _ReceptionActividadesScreenState extends State<ReceptionActividadesScreen> {
  List<dynamic> _activities = [];
  bool _loading = true;
  int? _selectedId;

  final _name = TextEditingController();
  final _desc = TextEditingController();
  final _location = TextEditingController();
  final _startDate = TextEditingController();
  final _endDate = TextEditingController();
  final _startTime = TextEditingController();
  final _endTime = TextEditingController();
  final _capacity = TextEditingController();
  bool _recurring = false;
  bool _unlimitedCapacity = true;
  final Set<String> _repeatDays = {};

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _name.dispose();
    _desc.dispose();
    _location.dispose();
    _startDate.dispose();
    _endDate.dispose();
    _startTime.dispose();
    _endTime.dispose();
    _capacity.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final api = context.read<AuthProvider>().api;
      final list = await api.getActivitySeries();
      if (mounted) setState(() => _activities = list);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _resetForm() {
    setState(() {
      _selectedId = null;
      _recurring = false;
      _unlimitedCapacity = true;
      _repeatDays.clear();
      _name.clear();
      _desc.clear();
      _location.clear();
      _startDate.clear();
      _endDate.clear();
      _startTime.clear();
      _endTime.clear();
      _capacity.clear();
    });
  }

  void _selectActivity(Map<String, dynamic> a) {
    final repeat = (a['repeatDays'] as List<dynamic>?)?.map((d) => '$d').toList() ?? [];
    setState(() {
      _selectedId = (a['id'] as num).toInt();
      _name.text = a['name'] ?? '';
      _desc.text = a['description'] ?? '';
      _location.text = a['locationName'] ?? '';
      _startDate.text = a['startDate'] ?? a['activityDate'] ?? '';
      _endDate.text = a['endDate'] ?? '';
      _startTime.text = _timeShort(a['startTime']);
      _endTime.text = _timeShort(a['endTime']);
      _recurring = a['recurring'] == true;
      _repeatDays
        ..clear()
        ..addAll(repeat);
      _unlimitedCapacity = a['capacity'] == null;
      _capacity.text = a['capacity'] != null ? '${a['capacity']}' : '';
    });
  }

  String _timeShort(dynamic value) {
    final s = '$value';
    return s.length >= 5 ? s.substring(0, 5) : s;
  }

  String _formatSeries(Map<String, dynamic> a) {
    if (a['recurring'] == true) {
      final days = ((a['repeatDays'] as List<dynamic>?) ?? [])
          .map((d) => _weekdays.firstWhere((w) => w.$1 == d, orElse: () => ('', '$d')).$2)
          .join(', ');
      final start = AppDateFormat.formatIsoDate(a['startDate'] as String?) ?? '';
      final end = AppDateFormat.formatIsoDate(a['endDate'] as String?) ?? '';
      return '$start → $end · $days';
    }
    return AppDateFormat.formatIsoDate((a['startDate'] ?? a['activityDate']) as String?) ?? '';
  }

  Map<String, dynamic> _buildPayload({bool confirmAffectedReservations = false}) => {
        'name': _name.text.trim(),
        'description': _desc.text.trim(),
        'locationName': _location.text.trim(),
        'startDate': _startDate.text.trim(),
        'endDate': _recurring ? _endDate.text.trim() : _startDate.text.trim(),
        'startTime': _startTime.text.trim(),
        'endTime': _endTime.text.trim().isEmpty ? null : _endTime.text.trim(),
        'capacity': _unlimitedCapacity ? null : int.tryParse(_capacity.text),
        'recurring': _recurring,
        'repeatDays': _recurring ? _repeatDays.toList() : <String>[],
        'confirmAffectedReservations': confirmAffectedReservations,
      };

  Future<bool> _confirmImpact(Map<String, dynamic> impact, {required bool isDelete}) async {
    final affected = (impact['affectedReservations'] as num?)?.toInt()
        ?? (impact['activeReservations'] as num?)?.toInt()
        ?? 0;
    if (affected == 0 && !isDelete) return true;

    final items = (impact['items'] as List<dynamic>?) ?? [];
    final lines = items.take(5).map((raw) {
      final item = raw as Map<String, dynamic>;
      return '· ${item['occurrenceDate']} — ${item['memberName']} (${item['status']})';
    }).join('\n');
    final extra = items.length > 5 ? '\n· … y ${items.length - 5} más' : '';
    final intro = isDelete
        ? 'Esta actividad tiene $affected reservaciones activas.'
        : 'Este cambio afectará $affected reservaciones activas.';

    return await showDialog<bool>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: Text(isDelete ? 'Eliminar actividad' : 'Confirmar cambios'),
            content: Text('$intro\n\n$lines$extra\n\n¿Cancelar esas reservaciones y continuar?'),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
              FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Continuar')),
            ],
          ),
        ) ??
        false;
  }

  Future<void> _save() async {
    if (_recurring && _endDate.text.trim().isEmpty) {
      _showError('Indica la fecha de fin para actividades recurrentes');
      return;
    }
    if (_recurring && _repeatDays.isEmpty) {
      _showError('Selecciona al menos un día de la semana');
      return;
    }
    try {
      final api = context.read<AuthProvider>().api;
      final payload = _buildPayload();
      if (_selectedId != null) {
        final impact = await api.previewActivityUpdateImpact(_selectedId!, payload);
        final affected = (impact['affectedReservations'] as num?)?.toInt() ?? 0;
        if (affected > 0) {
          final ok = await _confirmImpact(impact, isDelete: false);
          if (!ok) return;
          await api.updateActivity(_selectedId!, _buildPayload(confirmAffectedReservations: true));
        } else {
          await api.updateActivity(_selectedId!, payload);
        }
      } else {
        await api.createActivity(payload);
      }
      _resetForm();
      await _load();
    } on ApiException catch (e) {
      _showError(e.message);
    }
  }

  Future<void> _delete() async {
    if (_selectedId == null) return;
    try {
      final api = context.read<AuthProvider>().api;
      final impact = await api.getActivityDeleteImpact(_selectedId!);
      final active = (impact['activeReservations'] as num?)?.toInt() ?? 0;
      if (active > 0) {
        final ok = await _confirmImpact(impact, isDelete: true);
        if (!ok) return;
        await api.deleteActivity(_selectedId!, cancelReservations: true);
      } else {
        final ok = await showDialog<bool>(
              context: context,
              builder: (ctx) => AlertDialog(
                title: const Text('Eliminar actividad'),
                content: const Text('¿Eliminar esta actividad?'),
                actions: [
                  TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
                  FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Eliminar')),
                ],
              ),
            ) ??
            false;
        if (!ok) return;
        await api.deleteActivity(_selectedId!, cancelReservations: false);
      }
      _resetForm();
      await _load();
    } on ApiException catch (e) {
      _showError(e.message);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_activities.isEmpty)
            const Card(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: Text('No hay actividades registradas.')),
              ),
            )
          else
            ..._activities.map((raw) {
              final a = raw as Map<String, dynamic>;
              final id = (a['id'] as num).toInt();
              final selected = _selectedId == id;
              return Card(
                margin: const EdgeInsets.only(bottom: 8),
                color: selected
                    ? Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.35)
                    : null,
                child: ListTile(
                  onTap: () => _selectActivity(a),
                  leading: const Icon(Icons.event),
                  title: Row(
                    children: [
                      Expanded(child: Text(a['name'] ?? '')),
                      if (a['recurring'] == true)
                        const Padding(
                          padding: EdgeInsets.only(left: 8),
                          child: Chip(label: Text('Recurrente', visualDensity: VisualDensity.compact)),
                        ),
                    ],
                  ),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${_formatSeries(a)} · ${AppDateFormat.formatTimeRange(a['startTime'] as String?, a['endTime'] as String?)}'),
                      Text(a['locationName'] ?? ''),
                      Text(
                        'Cupo: ${a['capacity'] ?? 'Ilimitado'} · ${((a['confirmedReservations'] as num?) ?? 0).toInt()} reservaciones activas',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
              );
            }),
          const SizedBox(height: 8),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        _selectedId != null ? 'Editar actividad' : 'Nueva actividad',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      if (_selectedId != null)
                        TextButton(onPressed: _resetForm, child: const Text('Nuevo')),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextField(controller: _name, decoration: const InputDecoration(labelText: 'Nombre', border: OutlineInputBorder())),
                  const SizedBox(height: 8),
                  TextField(controller: _location, decoration: const InputDecoration(labelText: 'Ubicación', border: OutlineInputBorder())),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _startDate,
                    decoration: const InputDecoration(labelText: 'Fecha inicio (YYYY-MM-DD)', border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 8),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Actividad recurrente'),
                    value: _recurring,
                    onChanged: (v) => setState(() {
                      _recurring = v;
                      if (!v) _repeatDays.clear();
                    }),
                  ),
                  if (_recurring) ...[
                    TextField(
                      controller: _endDate,
                      decoration: const InputDecoration(labelText: 'Fecha fin (YYYY-MM-DD)', border: OutlineInputBorder()),
                    ),
                    const SizedBox(height: 8),
                    Text('Días de la semana', style: Theme.of(context).textTheme.labelLarge),
                    const SizedBox(height: 4),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: _weekdays.map((day) {
                        final selected = _repeatDays.contains(day.$1);
                        return FilterChip(
                          label: Text(day.$2),
                          selected: selected,
                          onSelected: (_) => setState(() {
                            if (selected) {
                              _repeatDays.remove(day.$1);
                            } else {
                              _repeatDays.add(day.$1);
                            }
                          }),
                        );
                      }).toList(),
                    ),
                    const SizedBox(height: 8),
                  ],
                  TextField(
                    controller: _startTime,
                    decoration: const InputDecoration(labelText: 'Hora inicio (HH:MM)', border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _endTime,
                    decoration: const InputDecoration(labelText: 'Hora fin (HH:MM)', border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _desc,
                    maxLines: 2,
                    decoration: const InputDecoration(labelText: 'Descripción', border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 8),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Cupo ilimitado'),
                    value: _unlimitedCapacity,
                    onChanged: (v) => setState(() => _unlimitedCapacity = v),
                  ),
                  if (!_unlimitedCapacity) ...[
                    TextField(
                      controller: _capacity,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Límite de cupos', border: OutlineInputBorder()),
                    ),
                    const SizedBox(height: 8),
                  ],
                  FilledButton(
                    onPressed: _save,
                    child: Text(_selectedId != null ? 'Guardar cambios' : 'Crear actividad'),
                  ),
                  if (_selectedId != null) ...[
                    const SizedBox(height: 8),
                    OutlinedButton(
                      onPressed: _delete,
                      style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
                      child: const Text('Eliminar actividad'),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
