import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';

class ReceptionHoyScreen extends StatefulWidget {
  const ReceptionHoyScreen({super.key});

  @override
  State<ReceptionHoyScreen> createState() => _ReceptionHoyScreenState();
}

class _ReceptionHoyScreenState extends State<ReceptionHoyScreen> {
  List<dynamic> _activities = [];
  bool _loading = true;

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
    try {
      final data = await context.read<AuthProvider>().api.getActivities(
            from: _todayIso,
            to: _todayIso,
          );
      if (mounted) setState(() { _activities = data; _loading = false; });
    } catch (_) {
      if (mounted) setState(() { _activities = []; _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: _activities.isEmpty
          ? ListView(children: const [
              SizedBox(height: 120),
              Center(child: Text('No hay actividades hoy')),
            ])
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _activities.length,
              itemBuilder: (context, index) {
                final a = _activities[index];
                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    title: Text(a['name'] ?? ''),
                    subtitle: Text('${a['startTime']} · ${a['locationName'] ?? ''}'),
                    trailing: Text('${a['confirmedReservations'] ?? 0} conf.'),
                  ),
                );
              },
            ),
    );
  }
}
