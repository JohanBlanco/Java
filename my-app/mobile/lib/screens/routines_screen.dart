import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../utils/list_filter.dart';
import '../widgets/list_filter_field.dart';

class RoutinesScreen extends StatefulWidget {
  final bool isStaff;
  const RoutinesScreen({super.key, required this.isStaff});

  @override
  State<RoutinesScreen> createState() => _RoutinesScreenState();
}

class _RoutinesScreenState extends State<RoutinesScreen> {
  List<dynamic> _routines = [];
  List<dynamic> _requests = [];
  bool _loading = true;
  String _filterQuery = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = context.read<AuthProvider>().api;
    if (widget.isStaff) {
      final requests = await api.getRoutineRequests();
      if (mounted) setState(() { _requests = requests; _loading = false; });
    } else {
      final routines = await api.getMyRoutines();
      if (mounted) setState(() { _routines = routines; _loading = false; });
    }
  }

  Future<void> _requestRoutine() async {
    final api = context.read<AuthProvider>().api;
    await api.createRoutineRequest(
      'Necesito una rutina personalizada',
      'Mejorar fuerza y resistencia',
    );
    _load();
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Solicitud enviada')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    if (widget.isStaff) {
      return _buildStaffView();
    }
    return _buildMemberView();
  }

  Widget _buildMemberView() {
    if (_routines.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('No tienes rutinas asignadas'),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _requestRoutine,
              child: const Text('Solicitar rutina'),
            ),
          ],
        ),
      );
    }

    final filtered = filterByQuery(_routines, _filterQuery);

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
              totalCount: _routines.length,
            );
          }
          if (filtered.isEmpty) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: Text('Ningún resultado coincide con la búsqueda')),
            );
          }

          final r = filtered[index - 1];
          final exercises = r['exercises'] as List<dynamic>? ?? [];

          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: ExpansionTile(
              title: Text(r['name']),
              subtitle: Text('Instructor: ${r['instructorName']}${r['temporary'] == true ? ' (Temporal)' : ''}'),
              children: exercises.map<Widget>((ex) {
                return ListTile(
                  dense: true,
                  title: Text(ex['exerciseName']),
                  trailing: Text('${ex['sets']}x${ex['reps']}'),
                );
              }).toList(),
            ),
          );
        },
      ),
    );
  }

  Widget _buildStaffView() {
    if (_requests.isEmpty) {
      return const Center(child: Text('Sin solicitudes de rutina'));
    }

    final filtered = filterByQuery(_requests, _filterQuery);

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
              totalCount: _requests.length,
            );
          }
          if (filtered.isEmpty) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: Text('Ningún resultado coincide con la búsqueda')),
            );
          }

          final r = filtered[index - 1];

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
        },
      ),
    );
  }
}
