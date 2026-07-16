import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../utils/list_filter.dart';
import '../widgets/list_filter_field.dart';

const _appointmentTypeLabels = {
  'MEASUREMENT': 'Medición',
  'NUTRITION': 'Nutrición',
  'CONSULTATION': 'Consulta',
};

class AppointmentRequestsScreen extends StatefulWidget {
  final bool isStaff;
  const AppointmentRequestsScreen({super.key, required this.isStaff});

  @override
  State<AppointmentRequestsScreen> createState() => _AppointmentRequestsScreenState();
}

class _AppointmentRequestsScreenState extends State<AppointmentRequestsScreen> {
  List<dynamic> _requests = [];
  bool _loading = true;
  String _type = 'CONSULTATION';
  final _notesController = TextEditingController();
  bool _saving = false;
  String _filterQuery = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _notesController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final api = context.read<AuthProvider>().api;
    try {
      final data = widget.isStaff
          ? await api.getAppointmentRequests()
          : await api.getMyAppointmentRequests();
      if (mounted) setState(() { _requests = data; _loading = false; });
    } catch (_) {
      if (mounted) setState(() { _requests = []; _loading = false; });
    }
  }

  Future<void> _create() async {
    setState(() => _saving = true);
    try {
      await context.read<AuthProvider>().api.createAppointmentRequest(
            _type,
            _notesController.text.trim(),
          );
      _notesController.clear();
      await _load();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Solicitud enviada')),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  String _typeLabel(String type) => _appointmentTypeLabels[type] ?? type;

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    final filtered = filterByQuery(
      _requests,
      _filterQuery,
      extraValues: (r) => [_typeLabel(r['type'])],
    );

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (!widget.isStaff) ...[
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    DropdownButtonFormField<String>(
                      value: _type,
                      decoration: const InputDecoration(labelText: 'Tipo de cita'),
                      items: _appointmentTypeLabels.entries
                          .map((e) => DropdownMenuItem(
                                value: e.key,
                                child: Text(e.value),
                              ))
                          .toList(),
                      onChanged: (v) => setState(() => _type = v ?? 'CONSULTATION'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _notesController,
                      decoration: const InputDecoration(
                        labelText: 'Notas (opcional)',
                      ),
                      maxLines: 3,
                    ),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: _saving ? null : _create,
                      child: Text(_saving ? 'Enviando...' : 'Solicitar cita'),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
          ],
          if (_requests.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 48),
              child: Center(child: Text('Sin solicitudes de cita')),
            )
          else ...[
            ListFilterField(
              onChanged: (v) => setState(() => _filterQuery = v),
              resultCount: filtered.length,
              totalCount: _requests.length,
            ),
            if (filtered.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 24),
                child: Center(child: Text('Ningún resultado coincide con la búsqueda')),
              )
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
                          Text(
                            widget.isStaff
                                ? r['memberName']
                                : _typeLabel(r['type']),
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                          Chip(label: Text(r['status'])),
                        ],
                      ),
                      if (widget.isStaff) ...[
                        const SizedBox(height: 8),
                        Text('Tipo: ${_typeLabel(r['type'])}'),
                      ],
                      if ((r['notes'] ?? '').toString().isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 8),
                          child: Text(r['notes']),
                        ),
                      if (widget.isStaff && r['status'] == 'PENDING') ...[
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
                      if (widget.isStaff && r['status'] == 'SCHEDULED') ...[
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
        ],
      ),
    );
  }
}
