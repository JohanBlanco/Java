import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ExpandableNavGroup extends StatefulWidget {
  const ExpandableNavGroup({
    super.key,
    required this.groupId,
    required this.title,
    required this.items,
    required this.selectedId,
    required this.onSelect,
    this.autoExpand = false,
  });

  final String groupId;
  final String title;
  final List<({String id, String label, IconData icon})> items;
  final String? selectedId;
  final ValueChanged<String> onSelect;
  final bool autoExpand;

  @override
  State<ExpandableNavGroup> createState() => _ExpandableNavGroupState();
}

class _ExpandableNavGroupState extends State<ExpandableNavGroup> {
  bool? _expanded;

  @override
  void initState() {
    super.initState();
    _loadExpanded();
  }

  Future<void> _loadExpanded() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getBool('nav-expanded-${widget.groupId}');
    if (!mounted) return;
    setState(() {
      if (stored != null) {
        _expanded = stored;
      } else {
        _expanded = widget.autoExpand;
      }
    });
  }

  Future<void> _toggle() async {
    final next = !(_expanded ?? false);
    setState(() => _expanded = next);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('nav-expanded-${widget.groupId}', next);
  }

  @override
  Widget build(BuildContext context) {
    final expanded = _expanded ?? widget.autoExpand;
    final groupActive = widget.items.any((i) => i.id == widget.selectedId);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ListTile(
          dense: true,
          contentPadding: const EdgeInsets.symmetric(horizontal: 8),
          title: Text(
            widget.title,
            style: TextStyle(
              fontWeight: FontWeight.w600,
              color: groupActive ? Theme.of(context).colorScheme.primary : null,
            ),
          ),
          trailing: Icon(expanded ? Icons.expand_less : Icons.expand_more, size: 20),
          onTap: _toggle,
        ),
        if (expanded)
          ...widget.items.map((item) {
            final selected = widget.selectedId == item.id;
            return Padding(
              padding: const EdgeInsets.only(left: 12),
              child: ListTile(
                dense: true,
                leading: Icon(item.icon, size: 20),
                title: Text(item.label),
                selected: selected,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                onTap: () => widget.onSelect(item.id),
              ),
            );
          }),
      ],
    );
  }
}
