import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';

class RoutineRequestsScreen extends StatefulWidget {
  const RoutineRequestsScreen({super.key});

  @override
  State<RoutineRequestsScreen> createState() => _RoutineRequestsScreenState();
}

class _RoutineRequestsScreenState extends State<RoutineRequestsScreen> {
  List<dynamic> _requests = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final data = await context.read<AuthProvider>().api.getRoutineRequests();
      if (mounted) setState(() { _requests = data; _loading = false; });
    } catch (_) {
      if (mounted) setState(() { _requests = []; _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: _requests.isEmpty
          ? ListView(children: const [
              SizedBox(height: 120),
              Center(child: Text('Sin solicitudes')),
            ])
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _requests.length,
              itemBuilder: (context, index) {
                final r = _requests[index];
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
                            Text(r['memberName'],
                                style: Theme.of(context).textTheme.titleMedium),
                            Chip(label: Text(r['status'])),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(r['description']),
                        Text('Objetivos: ${r['goals']}',
                            style: Theme.of(context).textTheme.bodySmall),
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
              },
            ),
    );
  }
}
