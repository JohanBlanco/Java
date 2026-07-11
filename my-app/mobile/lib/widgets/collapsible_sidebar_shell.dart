import 'package:flutter/material.dart';

class CollapsibleSidebarShell extends StatelessWidget {
  const CollapsibleSidebarShell({
    super.key,
    required this.sidebarVisible,
    required this.onToggleSidebar,
    required this.sidebar,
    required this.child,
  });

  final bool sidebarVisible;
  final VoidCallback onToggleSidebar;
  final Widget sidebar;
  final Widget child;

  static const _sidebarWidth = 260.0;
  static const _toggleSize = 28.0;

  @override
  Widget build(BuildContext context) {
    final edgeLeft = sidebarVisible ? _sidebarWidth : 0.0;

    return LayoutBuilder(
      builder: (context, constraints) {
        final midY = constraints.maxHeight.isFinite
            ? constraints.maxHeight / 2 - _toggleSize / 2
            : MediaQuery.sizeOf(context).height / 2 - _toggleSize / 2;

        return Stack(
          clipBehavior: Clip.none,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  curve: Curves.easeInOut,
                  width: sidebarVisible ? _sidebarWidth : 0,
                  child: ClipRect(
                    child: OverflowBox(
                      alignment: Alignment.centerLeft,
                      minWidth: _sidebarWidth,
                      maxWidth: _sidebarWidth,
                      child: sidebar,
                    ),
                  ),
                ),
                Expanded(child: child),
              ],
            ),
            AnimatedPositioned(
              duration: const Duration(milliseconds: 200),
              curve: Curves.easeInOut,
              left: edgeLeft - _toggleSize / 2,
              top: midY,
              child: _SidebarEdgeToggle(
                sidebarVisible: sidebarVisible,
                onPressed: onToggleSidebar,
              ),
            ),
          ],
        );
      },
    );
  }
}

class _SidebarEdgeToggle extends StatelessWidget {
  const _SidebarEdgeToggle({
    required this.sidebarVisible,
    required this.onPressed,
  });

  final bool sidebarVisible;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Material(
      elevation: 1,
      shadowColor: Colors.black26,
      shape: CircleBorder(
        side: BorderSide(color: colorScheme.outlineVariant),
      ),
      color: colorScheme.surface,
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: onPressed,
        child: SizedBox(
          width: CollapsibleSidebarShell._toggleSize,
          height: CollapsibleSidebarShell._toggleSize,
          child: Icon(
            sidebarVisible ? Icons.chevron_left : Icons.chevron_right,
            size: 18,
            color: colorScheme.onSurfaceVariant,
          ),
        ),
      ),
    );
  }
}
