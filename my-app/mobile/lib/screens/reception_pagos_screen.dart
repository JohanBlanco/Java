import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';

class ReceptionPagosScreen extends StatefulWidget {
  const ReceptionPagosScreen({super.key});

  @override
  State<ReceptionPagosScreen> createState() => _ReceptionPagosScreenState();
}

class _ReceptionPagosScreenState extends State<ReceptionPagosScreen> {
  List<dynamic> _pending = [];
  bool _loading = true;
  int? _markingId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final data = await context.read<AuthProvider>().api.getPendingPaymentReservations();
      if (mounted) setState(() { _pending = data; _loading = false; });
    } catch (_) {
      if (mounted) setState(() { _pending = []; _loading = false; });
    }
  }

  Future<void> _markPaid(int id) async {
    setState(() => _markingId = id);
    try {
      await context.read<AuthProvider>().api.markReservationPaid(id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Pago registrado')));
        _load();
      }
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _markingId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: _pending.isEmpty
          ? ListView(children: const [SizedBox(height: 120), Center(child: Text('No hay pagos pendientes'))])
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _pending.length,
              itemBuilder: (context, index) {
                final r = _pending[index];
                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    title: Text(r['activityName'] ?? ''),
                    subtitle: Text('${r['memberName']} · ${r['status']}'),
                    trailing: FilledButton(
                      onPressed: _markingId == r['id'] ? null : () => _markPaid(r['id']),
                      child: Text(_markingId == r['id'] ? '...' : 'Pagado'),
                    ),
                  ),
                );
              },
            ),
    );
  }
}
