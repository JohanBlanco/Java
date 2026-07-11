import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';

class ReservationsScreen extends StatefulWidget {
  const ReservationsScreen({super.key});

  @override
  State<ReservationsScreen> createState() => _ReservationsScreenState();
}

class _ReservationsScreenState extends State<ReservationsScreen> {
  List<dynamic> _reservations = [];
  bool _loading = true;

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

  Color _statusColor(String status) {
    switch (status) {
      case 'CONFIRMED': return Colors.green;
      case 'CANCELLED': return Colors.red;
      default: return Colors.orange;
    }
  }

  Future<void> _confirm(int id) async {
    try {
      await context.read<AuthProvider>().api.confirmReservation(id);
      _load();
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    if (_reservations.isEmpty) {
      return const Center(child: Text('No tienes reservaciones'));
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _reservations.length,
        itemBuilder: (context, index) {
          final r = _reservations[index];
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
                            label: Text(status, style: TextStyle(color: _statusColor(status), fontSize: 12)),
                            backgroundColor: _statusColor(status).withValues(alpha: 0.15),
                          ),
                          if (freeSlot && status != 'CANCELLED')
                            const Chip(label: Text('Gratis', style: TextStyle(fontSize: 12))),
                          if (paymentRequired && !paid && status != 'CANCELLED')
                            Chip(
                              label: const Text('Pago pendiente', style: TextStyle(fontSize: 12)),
                              backgroundColor: Colors.orange.withValues(alpha: 0.15),
                            ),
                        ],
                      ),
                    ],
                  ),
                  if (status == 'PENDING') ...[
                    const SizedBox(height: 12),
                    if (paymentRequired && !paid)
                      Text(
                        'Paga en recepción para confirmar tu cupo.',
                        style: Theme.of(context).textTheme.bodySmall,
                      )
                    else
                      Row(
                        children: [
                          FilledButton(
                            onPressed: () => _confirm(r['id']),
                            child: const Text('Confirmar'),
                          ),
                          const SizedBox(width: 8),
                          OutlinedButton(
                            onPressed: () async {
                              await context.read<AuthProvider>().api.cancelReservation(r['id']);
                              _load();
                            },
                            child: const Text('Cancelar'),
                          ),
                        ],
                      ),
                    if (paymentRequired && !paid)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: OutlinedButton(
                          onPressed: () async {
                            await context.read<AuthProvider>().api.cancelReservation(r['id']);
                            _load();
                          },
                          child: const Text('Cancelar'),
                        ),
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
