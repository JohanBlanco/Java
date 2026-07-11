import 'package:flutter/material.dart';

enum CalendarView { day, week, month, year }

class ActivityCalendar extends StatefulWidget {
  const ActivityCalendar({super.key, required this.activities, this.onActivityTap});

  final List<dynamic> activities;
  final void Function(Map<String, dynamic> activity)? onActivityTap;

  @override
  State<ActivityCalendar> createState() => _ActivityCalendarState();
}

class _ActivityCalendarState extends State<ActivityCalendar> {
  CalendarView _view = CalendarView.month;
  DateTime _anchor = DateTime.now();

  List<dynamic> _forDay(DateTime day) {
    final iso = _iso(day);
    return widget.activities.where((a) => a['activityDate'] == iso).toList();
  }

  String _iso(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  DateTime _startOfWeek(DateTime d) {
    final wd = d.weekday;
    return DateTime(d.year, d.month, d.day - (wd - 1));
  }

  void _shift(int delta) {
    setState(() {
      switch (_view) {
        case CalendarView.day:
          _anchor = _anchor.add(Duration(days: delta));
        case CalendarView.week:
          _anchor = _anchor.add(Duration(days: 7 * delta));
        case CalendarView.month:
          _anchor = DateTime(_anchor.year, _anchor.month + delta, _anchor.day);
        case CalendarView.year:
          _anchor = DateTime(_anchor.year + delta, _anchor.month, _anchor.day);
      }
    });
  }

  String _periodLabel() {
    switch (_view) {
      case CalendarView.day:
        return '${_anchor.day}/${_anchor.month}/${_anchor.year}';
      case CalendarView.week:
        final start = _startOfWeek(_anchor);
        final end = start.add(const Duration(days: 6));
        return '${start.day}/${start.month} – ${end.day}/${end.month}/${end.year}';
      case CalendarView.month:
        const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
        return '${months[_anchor.month - 1]} ${_anchor.year}';
      case CalendarView.year:
        return '${_anchor.year}';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final v in CalendarView.values)
                  ChoiceChip(
                    label: Text(_viewLabel(v)),
                    selected: _view == v,
                    onSelected: (_) => setState(() => _view = v),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                IconButton(onPressed: () => _shift(-1), icon: const Icon(Icons.chevron_left)),
                Expanded(child: Text(_periodLabel(), textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w600))),
                IconButton(onPressed: () => _shift(1), icon: const Icon(Icons.chevron_right)),
                TextButton(onPressed: () => setState(() => _anchor = DateTime.now()), child: const Text('Hoy')),
              ],
            ),
            const SizedBox(height: 8),
            if (widget.onActivityTap != null)
              Text('Toca una actividad para editarla.', style: Theme.of(context).textTheme.bodySmall),
            if (widget.onActivityTap != null) const SizedBox(height: 8),
            _buildBody(),
          ],
        ),
      ),
    );
  }

  String _viewLabel(CalendarView v) => switch (v) {
        CalendarView.day => 'Día',
        CalendarView.week => 'Semana',
        CalendarView.month => 'Mes',
        CalendarView.year => 'Año',
      };

  Widget _buildBody() {
    switch (_view) {
      case CalendarView.day:
        final acts = _forDay(_anchor);
        if (acts.isEmpty) return const Text('Sin actividades este día');
        return Column(
          children: acts.map((a) => _activityTile(a)).toList(),
        );
      case CalendarView.week:
        final start = _startOfWeek(_anchor);
        return SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: List.generate(7, (i) {
              final day = start.add(Duration(days: i));
              final acts = _forDay(day);
              return SizedBox(
                width: 120,
                child: Card(
                  margin: const EdgeInsets.only(right: 8),
                  child: Padding(
                    padding: const EdgeInsets.all(8),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('${day.day}/${day.month}', style: const TextStyle(fontWeight: FontWeight.w600)),
                        const SizedBox(height: 4),
                        if (acts.isEmpty) const Text('—', style: TextStyle(fontSize: 12))
                        else ...acts.map((a) => _activityTile(a)),
                      ],
                    ),
                  ),
                ),
              );
            }),
          ),
        );
      case CalendarView.month:
        final first = DateTime(_anchor.year, _anchor.month, 1);
        final start = _startOfWeek(first);
        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 7, mainAxisSpacing: 4, crossAxisSpacing: 4, childAspectRatio: 1),
          itemCount: 42,
          itemBuilder: (context, i) {
            final day = start.add(Duration(days: i));
            final inMonth = day.month == _anchor.month;
            final acts = _forDay(day);
            return Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
                borderRadius: BorderRadius.circular(6),
                color: inMonth ? null : Theme.of(context).colorScheme.surfaceContainerHighest.withValues(alpha: 0.4),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('${day.day}', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 11, color: inMonth ? null : Theme.of(context).colorScheme.onSurfaceVariant)),
                  if (acts.isNotEmpty)
                    Text(acts.first['name'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 9)),
                  if (acts.length > 1) Text('+${acts.length - 1}', style: const TextStyle(fontSize: 9)),
                ],
              ),
            );
          },
        );
      case CalendarView.year:
        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, mainAxisSpacing: 8, crossAxisSpacing: 8, childAspectRatio: 1.4),
          itemCount: 12,
          itemBuilder: (context, monthIndex) {
            final monthStart = DateTime(_anchor.year, monthIndex + 1, 1);
            final monthEnd = DateTime(_anchor.year, monthIndex + 2, 0);
            final count = widget.activities.where((a) {
              final parts = (a['activityDate'] as String).split('-').map(int.parse).toList();
              final d = DateTime(parts[0], parts[1], parts[2]);
              return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
            }).length;
            const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
            return InkWell(
              onTap: () => setState(() {
                _anchor = monthStart;
                _view = CalendarView.month;
              }),
              child: Card(
                child: Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(months[monthIndex], style: const TextStyle(fontWeight: FontWeight.w600)),
                      Text('$count act.', style: Theme.of(context).textTheme.bodySmall),
                    ],
                  ),
                ),
              ),
            );
          },
        );
    }
  }

  Widget _activityTile(dynamic a) {
    final map = a as Map<String, dynamic>;
    final hasOverride = map['hasOccurrenceOverride'] == true;
    final child = Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  '${map['name'] ?? ''}${hasOverride ? ' *' : ''}',
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                ),
              ),
            ],
          ),
          Text('${map['startTime']} · ${map['locationName'] ?? ''}', style: const TextStyle(fontSize: 11)),
        ],
      ),
    );
    if (widget.onActivityTap == null) return child;
    return InkWell(
      onTap: () => widget.onActivityTap!(map),
      borderRadius: BorderRadius.circular(6),
      child: child,
    );
  }
}
