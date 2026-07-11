import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';

class ActivitiesScreen extends StatefulWidget {
  final bool isStaff;
  const ActivitiesScreen({super.key, required this.isStaff});

  @override
  State<ActivitiesScreen> createState() => _ActivitiesScreenState();
}

class _ActivitiesScreenState extends State<ActivitiesScreen> {
  List<dynamic> _activities = [];
  Map<String, dynamic>? _membershipUsage;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final api = context.read<AuthProvider>().api;
    try {
      final results = await Future.wait([
        api.getActivities(),
        if (!widget.isStaff) api.getMyMembershipUsage(),
      ]);
      if (mounted) {
        setState(() {
          _activities = results[0] as List<dynamic>;
          if (!widget.isStaff && results.length > 1) {
            _membershipUsage = results[1] as Map<String, dynamic>;
          }
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _reserve(Map<String, dynamic> activity) async {
    final api = context.read<AuthProvider>().api;
    final activityId = (activity['id'] as num).toInt();
    final occurrenceDate = activity['activityDate'] as String?;
    try {
      await api.createReservation(activityId, occurrenceDate: occurrenceDate);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Reservación creada')),
        );
        _load();
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.message.contains('Debes pagar en recepción')) {
        final pay = await showDialog<bool>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('Actividades gratuitas agotadas'),
            content: Text('${e.message}\n\n¿Deseas reservar y pagar en recepción?'),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
              FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Reservar')),
            ],
          ),
        );
        if (pay == true) {
          await api.createReservation(activityId, payAtReception: true, occurrenceDate: occurrenceDate);
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Reservación creada. Paga en recepción para confirmar.')),
            );
            _load();
          }
        }
      } else {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  Widget _membershipBanner() {
    final usage = _membershipUsage;
    if (usage == null || usage['membershipName'] == null) return const SizedBox.shrink();

    final unlimited = usage['unlimitedFreeActivities'] == true;
    final text = unlimited
        ? 'Actividades gratuitas: ilimitadas'
        : 'Gratis: ${usage['freeActivitiesUsed']} usadas · ${usage['freeActivitiesRemaining']} restantes de ${usage['freeActivityQuota']}';

    return Card(
      margin: const EdgeInsets.fromLTRB(16, 16, 16, 0),
      child: ListTile(
        leading: const Icon(Icons.card_membership),
        title: Text('Membresía: ${usage['membershipName']}'),
        subtitle: Text(text),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: _activities.isEmpty
          ? ListView(
              children: [
                if (!widget.isStaff) _membershipBanner(),
                const SizedBox(height: 120),
                const Center(child: Text('No hay actividades programadas')),
              ],
            )
          : ListView.builder(
              padding: const EdgeInsets.only(bottom: 16),
              itemCount: _activities.length + (widget.isStaff ? 0 : 1),
              itemBuilder: (context, index) {
                if (!widget.isStaff && index == 0) return _membershipBanner();

                final a = _activities[index - (widget.isStaff ? 0 : 1)];
                final capacity = a['capacity'];
                final hasCapacity = a['hasCapacity'] ?? true;

                return Card(
                  margin: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(a['name'], style: Theme.of(context).textTheme.titleMedium),
                        const SizedBox(height: 4),
                        Text('${a['activityDate']} · ${a['startTime']} - ${a['endTime']}'),
                        Text(a['locationName'] ?? '', style: Theme.of(context).textTheme.bodySmall),
                        if ((a['description'] ?? '').toString().isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 4),
                            child: Text(a['description'], style: Theme.of(context).textTheme.bodySmall),
                          ),
                        const SizedBox(height: 4),
                        Text(
                          'Cupo: ${capacity ?? 'Ilimitado'} · ${a['confirmedReservations'] ?? 0} confirmados',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                        if (!widget.isStaff && hasCapacity) ...[
                          const SizedBox(height: 12),
                          FilledButton(
                            onPressed: () => _reserve(a as Map<String, dynamic>),
                            child: const Text('Reservar'),
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
