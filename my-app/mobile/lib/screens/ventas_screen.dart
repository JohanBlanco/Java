import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';

class VentasScreen extends StatefulWidget {
  const VentasScreen({super.key});

  @override
  State<VentasScreen> createState() => _VentasScreenState();
}

class _VentasScreenState extends State<VentasScreen> {
  List<dynamic> _sales = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final data = await context.read<AuthProvider>().api.getSales();
      if (mounted) setState(() { _sales = data; _loading = false; });
    } catch (_) {
      if (mounted) setState(() { _sales = []; _loading = false; });
    }
  }

  double _sumToday() {
    final today = DateTime.now();
    return _sales.where((s) {
      final d = DateTime.parse(s['paidAt']);
      return d.year == today.year && d.month == today.month && d.day == today.day;
    }).fold<double>(0, (sum, s) => sum + (s['amount'] as num).toDouble());
  }

  double _sumMonth() {
    final today = DateTime.now();
    return _sales.where((s) {
      final d = DateTime.parse(s['paidAt']);
      return d.year == today.year && d.month == today.month;
    }).fold<double>(0, (sum, s) => sum + (s['amount'] as num).toDouble());
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Ventas', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: _StatCard(label: 'Total', value: '${_sales.length}')),
              const SizedBox(width: 8),
              Expanded(child: _StatCard(label: 'Hoy', value: '\$${_sumToday().toStringAsFixed(0)}')),
              const SizedBox(width: 8),
              Expanded(child: _StatCard(label: 'Mes', value: '\$${_sumMonth().toStringAsFixed(0)}')),
            ],
          ),
          const SizedBox(height: 16),
          if (_sales.isEmpty)
            const Card(child: Padding(padding: EdgeInsets.all(24), child: Center(child: Text('Sin ventas registradas'))))
          else
            ..._sales.map((s) => Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    title: Text(s['concept'] ?? 'Venta'),
                    subtitle: Text('${s['activityName']} · ${s['memberName']}'),
                    trailing: Text('\$${(s['amount'] as num).toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w700)),
                  ),
                )),
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.label, required this.value});
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Text(value, style: Theme.of(context).textTheme.titleLarge?.copyWith(color: Theme.of(context).colorScheme.primary)),
            Text(label, style: Theme.of(context).textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}
