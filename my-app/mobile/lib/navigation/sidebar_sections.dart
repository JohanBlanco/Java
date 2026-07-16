import 'package:flutter/material.dart';

class SidebarNavItem {
  const SidebarNavItem({required this.id, required this.label, required this.icon});
  final String id;
  final String label;
  final IconData icon;
}

class SidebarNavGroup {
  const SidebarNavGroup({
    required this.id,
    required this.label,
    required this.items,
  });
  final String id;
  final String label;
  final List<SidebarNavItem> items;
}

class SidebarSections {
  static const ventas = SidebarNavGroup(
    id: 'ventas',
    label: 'Ventas',
    items: [
      SidebarNavItem(id: 'ventas-registro', label: 'Registro de ventas', icon: Icons.receipt_long),
    ],
  );

  static const estadisticas = SidebarNavGroup(
    id: 'estadisticas',
    label: 'Estadísticas',
    items: [
      SidebarNavItem(id: 'estadisticas-resumen', label: 'Resumen general', icon: Icons.bar_chart),
    ],
  );

  static const servicios = SidebarNavGroup(
    id: 'servicios',
    label: 'Servicios',
    items: [
      SidebarNavItem(id: 'serv-actividades', label: 'Actividades', icon: Icons.event),
      SidebarNavItem(id: 'serv-reservaciones', label: 'Mis reservaciones', icon: Icons.bookmark),
      SidebarNavItem(id: 'serv-rutinas', label: 'Mis rutinas', icon: Icons.fitness_center),
      SidebarNavItem(id: 'serv-solicitudes-citas', label: 'Solicitar cita', icon: Icons.event_note_outlined),
    ],
  );

  static const administracion = SidebarNavGroup(
    id: 'reception',
    label: 'Administración',
    items: [
      SidebarNavItem(id: 'reception-actividades', label: 'Actividades', icon: Icons.event_outlined),
      SidebarNavItem(id: 'reception-calendario', label: 'Calendario', icon: Icons.calendar_month),
      SidebarNavItem(id: 'admin-membresias', label: 'Membresías', icon: Icons.card_membership_outlined),
      SidebarNavItem(id: 'admin-usuarios', label: 'Usuarios', icon: Icons.people_outline),
    ],
  );
}
