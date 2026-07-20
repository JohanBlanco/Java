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
      final results = await Future.wait([
        api.getMyRoutines(),
        api.getRoutineRequests(),
      ]);
      if (mounted) {
        setState(() {
          _routines = results[0];
          _requests = results[1];
          _loading = false;
        });
      }
    }
  }

  bool get _hasOpenRequest {
    return _requests.any((r) {
      final status = (r['status'] as String?) ?? '';
      return status == 'PENDING' || status == 'IN_PROGRESS';
    });
  }

  Map<String, dynamic>? get _openRequest {
    for (final r in _requests) {
      final status = (r['status'] as String?) ?? '';
      if (status == 'PENDING' || status == 'IN_PROGRESS') {
        return Map<String, dynamic>.from(r as Map);
      }
    }
    return null;
  }

  Map<String, dynamic>? get _currentRoutine {
    if (_routines.isEmpty) return null;
    final sorted = [..._routines]
      ..sort((a, b) => ((b['id'] as num?) ?? 0).compareTo((a['id'] as num?) ?? 0));
    return Map<String, dynamic>.from(sorted.first as Map);
  }

  Future<void> _requestRoutine() async {
    if (_hasOpenRequest) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Ya tienes una solicitud abierta')),
        );
      }
      return;
    }
    final api = context.read<AuthProvider>().api;
    try {
      await api.createRoutineRequest(
        'Necesito una rutina personalizada',
        'Mejorar fuerza y resistencia',
      );
      await _load();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Solicitud enviada')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$e')),
        );
      }
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
    final routine = _currentRoutine;
    final expired = routine?['expired'] == true;
    final canRequest = !_hasOpenRequest;
    final open = _openRequest;

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (expired && canRequest)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Tu rutina ya no está vigente', style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Text(
                      'Venció${routine?['validUntil'] != null ? ' el ${routine!['validUntil']}' : ''}. Solicita una nueva.',
                    ),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: _requestRoutine,
                      child: const Text('Solicitar rutina'),
                    ),
                  ],
                ),
              ),
            ),
          if (open != null)
            Card(
              child: ListTile(
                title: Text(open['description']?.toString() ?? 'Solicitud abierta'),
                subtitle: const Text('Solo una solicitud abierta a la vez. Cuando te asignen la rutina podrás pedir otra.'),
                trailing: Text(open['status']?.toString() ?? ''),
              ),
            ),
          if (routine != null) ...[
            Card(
              child: ExpansionTile(
                title: Text(routine['name']?.toString() ?? 'Rutina'),
                subtitle: Text(
                  [
                    if (routine['instructorName'] != null) 'Instructor: ${routine['instructorName']}',
                    if (expired)
                      'Vencida'
                    else if (routine['validUntil'] != null)
                      'Vigente hasta ${routine['validUntil']}',
                  ].join(' · '),
                ),
                children: ((routine['days'] as List?) ?? []).isNotEmpty
                    ? (routine['days'] as List).expand<Widget>((day) {
                        final exercises = day['exercises'] as List? ?? [];
                        return [
                          ListTile(
                            dense: true,
                            title: Text(day['dayLabel']?.toString() ?? 'Día', style: const TextStyle(fontWeight: FontWeight.w600)),
                          ),
                          ...exercises.map((ex) => ListTile(
                                dense: true,
                                title: Text(ex['exerciseName']?.toString() ?? ''),
                                trailing: Text('${ex['sets']}x${ex['reps']}'),
                              )),
                        ];
                      }).toList()
                    : ((routine['exercises'] as List?) ?? []).map<Widget>((ex) {
                        return ListTile(
                          dense: true,
                          title: Text(ex['exerciseName']?.toString() ?? ''),
                          trailing: Text('${ex['sets']}x${ex['reps']}'),
                        );
                      }).toList(),
              ),
            ),
            if (canRequest && !expired)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: OutlinedButton(
                  onPressed: _requestRoutine,
                  child: const Text('Solicitar otra rutina'),
                ),
              ),
          ] else if (canRequest)
            Center(
              child: Column(
                children: [
                  const SizedBox(height: 48),
                  const Text('No tienes una rutina asignada'),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: _requestRoutine,
                    child: const Text('Solicitar rutina'),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildStaffView() {
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
            child: ListTile(
              title: Text(r['memberName'] ?? ''),
              subtitle: Text('${r['description']}\n${r['goals']}'),
              isThreeLine: true,
              trailing: Text(r['status'] ?? ''),
              onTap: () async {
                await context
                    .read<AuthProvider>()
                    .api
                    .updateRoutineRequestStatus(r['id'], 'IN_PROGRESS');
                _load();
              },
            ),
          );
        },
      ),
    );
  }
}
