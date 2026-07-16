import 'package:flutter/material.dart';

class ListFilterField extends StatefulWidget {
  final ValueChanged<String> onChanged;
  final int? resultCount;
  final int? totalCount;
  final String placeholder;

  const ListFilterField({
    super.key,
    required this.onChanged,
    this.resultCount,
    this.totalCount,
    this.placeholder = 'Buscar en la lista…',
  });

  @override
  State<ListFilterField> createState() => _ListFilterFieldState();
}

class _ListFilterFieldState extends State<ListFilterField> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final showCount = widget.resultCount != null &&
        widget.totalCount != null &&
        _controller.text.trim().isNotEmpty;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _controller,
              onChanged: widget.onChanged,
              decoration: InputDecoration(
                hintText: widget.placeholder,
                prefixIcon: const Icon(Icons.search, size: 20),
                isDense: true,
                border: const OutlineInputBorder(),
              ),
            ),
          ),
          if (showCount) ...[
            const SizedBox(width: 12),
            Text(
              '${widget.resultCount} de ${widget.totalCount}',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ],
      ),
    );
  }
}
