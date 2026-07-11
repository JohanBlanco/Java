import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';
import '../widgets/activity_calendar.dart';

class ReceptionCalendarioScreen extends StatefulWidget {
  const ReceptionCalendarioScreen({super.key});

  @override
  State<ReceptionCalendarioScreen> createState() => _ReceptionCalendarioScreenState();
}

class _ReceptionCalendarioScreenState extends State<ReceptionCalendarioScreen> {
  List<dynamic> _activities = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final data = await context.read<AuthProvider>().api.getActivities();
      if (mounted) setState(() { _activities = data; _loading = false; });
    } catch (_) {
      if (mounted) setState(() { _activities = []; _loading = false; });
    }
  }

  Future<void> _editActivity(Map<String, dynamic> activity) async {
    final id = (activity['id'] as num).toInt();
    final recurring = activity['recurring'] == true;
    final occurrenceDate = activity['activityDate'] as String? ?? '';

    final startCtrl = TextEditingController(text: _timeShort(activity['startTime']));
    final endCtrl = TextEditingController(text: _timeShort(activity['endTime']));
    final locationCtrl = TextEditingController(text: activity['locationName'] ?? '');
    final capacityCtrl = TextEditingController(
      text: activity['capacity'] != null ? '${activity['capacity']}' : '',
    );
    var unlimited = activity['capacity'] == null;
    var scope = 'OCCURRENCE';

    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
          ),
          child: StatefulBuilder(
            builder: (context, setModalState) {
              return SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(activity['name'] ?? '', style: Theme.of(context).textTheme.titleMedium),
                    Text(occurrenceDate, style: Theme.of(context).textTheme.bodySmall),
                    const SizedBox(height: 12),
                    if (recurring) ...[
                      RadioListTile<String>(
                        contentPadding: EdgeInsets.zero,
                        title: Text('Solo esta clase ($occurrenceDate)'),
                        value: 'OCCURRENCE',
                        groupValue: scope,
                        onChanged: (v) => setModalState(() => scope = v!),
                      ),
                      RadioListTile<String>(
                        contentPadding: EdgeInsets.zero,
                        title: const Text('Toda la serie'),
                        value: 'SERIES',
                        groupValue: scope,
                        onChanged: (v) => setModalState(() => scope = v!),
                      ),
                    ],
                    TextField(
                      controller: startCtrl,
                      decoration: const InputDecoration(labelText: 'Hora inicio (HH:MM)', border: OutlineInputBorder()),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: endCtrl,
                      decoration: const InputDecoration(labelText: 'Hora fin (HH:MM)', border: OutlineInputBorder()),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: locationCtrl,
                      decoration: const InputDecoration(labelText: 'Ubicación', border: OutlineInputBorder()),
                    ),
                    const SizedBox(height: 8),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Cupo ilimitado'),
                      value: unlimited,
                      onChanged: (v) => setModalState(() => unlimited = v),
                    ),
                    if (!unlimited)
                      TextField(
                        controller: capacityCtrl,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(labelText: 'Cupo', border: OutlineInputBorder()),
                      ),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: () async {
                        try {
                          await context.read<AuthProvider>().api.editActivityOccurrence(id, {
                            'occurrenceDate': occurrenceDate,
                            'startTime': startCtrl.text.trim(),
                            'endTime': endCtrl.text.trim(),
                            'locationName': locationCtrl.text.trim(),
                            'capacity': unlimited ? null : int.tryParse(capacityCtrl.text),
                            'scope': recurring ? scope : 'SERIES',
                          });
                          if (ctx.mounted) Navigator.pop(ctx, true);
                        } on ApiException catch (e) {
                          if (ctx.mounted) {
                            ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(content: Text(e.message)));
                          }
                        }
                      },
                      child: const Text('Guardar'),
                    ),
                  ],
                ),
              );
            },
          ),
        );
      },
    );

    startCtrl.dispose();
    endCtrl.dispose();
    locationCtrl.dispose();
    capacityCtrl.dispose();

    if (saved == true) await _load();
  }

  String _timeShort(dynamic value) {
    final s = '$value';
    return s.length >= 5 ? s.substring(0, 5) : s;
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          ActivityCalendar(
            activities: _activities,
            onActivityTap: _editActivity,
          ),
        ],
      ),
    );
  }
}
