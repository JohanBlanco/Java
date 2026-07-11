import 'package:flutter/material.dart';

class MultiSelectField extends StatelessWidget {
  const MultiSelectField({
    super.key,
    required this.label,
    required this.options,
    required this.selected,
    required this.onChanged,
    this.emptyLabel = 'Sin seleccionar',
  });

  final String label;
  final Map<String, String> options;
  final Set<String> selected;
  final ValueChanged<Set<String>> onChanged;
  final String emptyLabel;

  String get _display {
    if (selected.isEmpty) return emptyLabel;
    return selected.map((key) => options[key] ?? key).join(', ');
  }

  Future<void> _openPicker(BuildContext context) async {
    final temp = Set<String>.from(selected);
    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: Text(label),
              content: SizedBox(
                width: double.maxFinite,
                child: options.isEmpty
                    ? Text('No hay opciones disponibles', style: Theme.of(context).textTheme.bodySmall)
                    : ListView(
                        shrinkWrap: true,
                        children: options.entries.map((entry) {
                          return CheckboxListTile(
                            value: temp.contains(entry.key),
                            title: Text(entry.value),
                            onChanged: (checked) {
                              setDialogState(() {
                                if (checked == true) {
                                  temp.add(entry.key);
                                } else {
                                  temp.remove(entry.key);
                                }
                              });
                            },
                          );
                        }).toList(),
                      ),
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
                FilledButton(
                  onPressed: () {
                    onChanged(temp);
                    Navigator.pop(context);
                  },
                  child: const Text('Aplicar'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
      ),
      child: InkWell(
        onTap: () => _openPicker(context),
        child: Row(
          children: [
            Expanded(
              child: Text(
                _display,
                style: TextStyle(
                  color: selected.isEmpty ? Theme.of(context).hintColor : null,
                ),
              ),
            ),
            const Icon(Icons.arrow_drop_down),
          ],
        ),
      ),
    );
  }
}

class MultiSelectFieldInt extends StatelessWidget {
  const MultiSelectFieldInt({
    super.key,
    required this.label,
    required this.options,
    required this.selected,
    required this.onChanged,
    this.emptyLabel = 'Sin seleccionar',
  });

  final String label;
  final Map<int, String> options;
  final Set<int> selected;
  final ValueChanged<Set<int>> onChanged;
  final String emptyLabel;

  String get _display {
    if (selected.isEmpty) return emptyLabel;
    return selected.map((key) => options[key] ?? '#$key').join(', ');
  }

  Future<void> _openPicker(BuildContext context) async {
    final temp = Set<int>.from(selected);
    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: Text(label),
              content: SizedBox(
                width: double.maxFinite,
                child: options.isEmpty
                    ? Text('No hay opciones disponibles', style: Theme.of(context).textTheme.bodySmall)
                    : ListView(
                        shrinkWrap: true,
                        children: options.entries.map((entry) {
                          return CheckboxListTile(
                            value: temp.contains(entry.key),
                            title: Text(entry.value),
                            onChanged: (checked) {
                              setDialogState(() {
                                if (checked == true) {
                                  temp.add(entry.key);
                                } else {
                                  temp.remove(entry.key);
                                }
                              });
                            },
                          );
                        }).toList(),
                      ),
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
                FilledButton(
                  onPressed: () {
                    onChanged(temp);
                    Navigator.pop(context);
                  },
                  child: const Text('Aplicar'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
      ),
      child: InkWell(
        onTap: () => _openPicker(context),
        child: Row(
          children: [
            Expanded(
              child: Text(
                _display,
                style: TextStyle(
                  color: selected.isEmpty ? Theme.of(context).hintColor : null,
                ),
              ),
            ),
            const Icon(Icons.arrow_drop_down),
          ],
        ),
      ),
    );
  }
}
