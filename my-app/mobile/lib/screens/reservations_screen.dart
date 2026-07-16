import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';
import '../utils/list_filter.dart';
import '../widgets/list_filter_field.dart';

class ReservationsScreen extends StatefulWidget {
  const ReservationsScreen({super.key});

  @override
  State<ReservationsScreen> createState() => _ReservationsScreenState();
}

class _ReservationsScreenState extends State<ReservationsScreen> {
  List<dynamic> _reservations = [];
  bool _loading = true;
  String _filterQuery = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = context.read<AuthProvider>().api;
    try {
      final data = await api.getMyReservations();
      if (mounted) setState(() { _reservations = data; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _statusLabel(String status) {
    switch (status) {
      case 'CONFIRMED': return 'Confirmada';
      case 'CANCELLED': return 'Cancelada';
      default: return status;
    }
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'CONFIRMED': return Colors.green;
      case 'CANCELLED': return Colors.red;
      default: return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    if (_reservations.isEmpty) {
      return const Center(child: Text('No tienes reservaciones'));
    }

    final filtered = filterByQuery(_reservations, _filterQuery);

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: filtered.isEmpty ? 2 : filtered.length + 1,
        itemBuilder: (context, index) {
          if (index == 0) {
            return ListFilterField(
              onChanged: (v) => setState(() => _filterQuery = v),
              resultCount: filtered.length,
              totalCount: _reservations.length,
            );
          }
          if (filtered.isEmpty) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: Text('Ningún resultado coincide con la búsqueda')),
            );
          }

          final r = filtered[index - 1];
          final status = r['status'] as String;
          final paymentRequired = r['paymentRequired'] == true;
          final paid = r['paid'] == true;
          final freeSlot = r['freeSlot'] == true;

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
                      Expanded(child: Text(r['activityName'], style: Theme.of(context).textTheme.titleMedium)),
                      Wrap(
                        spacing: 4,
                        children: [
                          Chip(
                            label: Text(_statusLabel(status), style: TextStyle(color: _statusColor(status), fontSize: 12)),
                            backgroundColor: _statusColor(status).withValues(alpha: 0.15),
                          ),
                          if (freeSlot && status == 'CONFIRMED')
                            const Chip(label: Text('Gratis', style: TextStyle(fontSize: 12))),
                          if (paymentRequired && !paid && status == 'CONFIRMED')
                            Chip(
                              label: const Text('Pago pendiente', style: TextStyle(fontSize: 12)),
                              backgroundColor: Colors.orange.withValues(alpha: 0.15),
                            ),
                        ],
                      ),
                    ],
                  ),
                  if (status == 'CONFIRMED' && paymentRequired && !paid) ...[
                    const SizedBox(height: 12),
                    Text(
                      'Paga en recepción para conservar tu cupo.',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                  if (status == 'CONFIRMED') ...[
                    const SizedBox(height: 12),
                    OutlinedButton(
                      onPressed: () async {
                        await context.read<AuthProvider>().api.cancelReservation(r['id']);
                        _load();
                      },
                      child: const Text('Cancelar'),
                    ),
                  ],
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
