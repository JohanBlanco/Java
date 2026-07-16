import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../providers/layout_provider.dart';
import 'dashboard_screen.dart';
import 'activities_screen.dart';
import 'reservations_screen.dart';
import 'ventas_screen.dart';
import 'estadisticas_screen.dart';
import 'reception_actividades_screen.dart';
import 'reception_calendario_screen.dart';
import 'routines_screen.dart';
import 'profile_screen.dart';
import 'gym_admin_screen.dart';
import 'appointment_requests_screen.dart';
import '../config/role_helper.dart';
import '../navigation/sidebar_sections.dart';
import '../widgets/app_sidebar.dart';
import '../widgets/collapsible_sidebar_shell.dart';
import '../widgets/expandable_nav_group.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String _routeId = 'dashboard';
  final _scaffoldKey = GlobalKey<ScaffoldState>();

  Widget _screenFor(String id, {required bool isStaff}) {
    switch (id) {
      case 'dashboard':
        return const DashboardScreen();
      case 'serv-actividades':
        return ActivitiesScreen(isStaff: false);
      case 'serv-reservaciones':
        return const ReservationsScreen();
      case 'serv-rutinas':
        return RoutinesScreen(isStaff: false);
      case 'serv-solicitudes-citas':
        return const AppointmentRequestsScreen(isStaff: false);
      case 'activities':
        return ActivitiesScreen(isStaff: isStaff);
      case 'reservations':
        return const ReservationsScreen();
      case 'ventas-registro':
        return const VentasScreen();
      case 'estadisticas-resumen':
        return const EstadisticasScreen();
      case 'reception-actividades':
        return const ReceptionActividadesScreen();
      case 'reception-calendario':
        return const ReceptionCalendarioScreen();
      case 'admin-membresias':
        return const GymAdminScreen(initialSection: AdminSection.membresias);
      case 'admin-usuarios':
        return const GymAdminScreen(initialSection: AdminSection.usuarios);
      case 'routines':
      default:
        return RoutinesScreen(isStaff: isStaff);
    }
  }

  bool _groupActive(SidebarNavGroup group) =>
      group.items.any((item) => item.id == _routeId);

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final layout = context.watch<LayoutProvider>();
    final user = auth.user!;
    final activeRole = auth.resolvedActiveRole;
    final isStaff = RoleHelper.isStaffView(activeRole);
    final isMember = RoleHelper.isMemberView(activeRole);
    final showOps = RoleHelper.canViewReception(activeRole);
    final showProfile = RoleHelper.canViewProfile(activeRole);

    void selectRoute(String id) {
      setState(() => _routeId = id);
      _scaffoldKey.currentState?.closeDrawer();
    }

    void openProfile() {
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
    }

    final sidebarChildren = <Widget>[
      ListTile(
        leading: const Icon(Icons.home_outlined),
        title: const Text('Inicio'),
        selected: _routeId == 'dashboard',
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        onTap: () => selectRoute('dashboard'),
      ),
      if (isMember)
        ExpandableNavGroup(
          groupId: SidebarSections.servicios.id,
          title: SidebarSections.servicios.label,
          autoExpand: _groupActive(SidebarSections.servicios),
          selectedId: _routeId,
          onSelect: selectRoute,
          items: SidebarSections.servicios.items
              .map((i) => (id: i.id, label: i.label, icon: i.icon))
              .toList(),
        ),
      if (showOps) ...[
        ExpandableNavGroup(
          groupId: SidebarSections.ventas.id,
          title: SidebarSections.ventas.label,
          autoExpand: _groupActive(SidebarSections.ventas),
          selectedId: _routeId,
          onSelect: selectRoute,
          items: SidebarSections.ventas.items
              .map((i) => (id: i.id, label: i.label, icon: i.icon))
              .toList(),
        ),
        ExpandableNavGroup(
          groupId: SidebarSections.estadisticas.id,
          title: SidebarSections.estadisticas.label,
          autoExpand: _groupActive(SidebarSections.estadisticas),
          selectedId: _routeId,
          onSelect: selectRoute,
          items: SidebarSections.estadisticas.items
              .map((i) => (id: i.id, label: i.label, icon: i.icon))
              .toList(),
        ),
        ExpandableNavGroup(
          groupId: SidebarSections.administracion.id,
          title: SidebarSections.administracion.label,
          autoExpand: _groupActive(SidebarSections.administracion),
          selectedId: _routeId,
          onSelect: selectRoute,
          items: SidebarSections.administracion.items
              .map((i) => (id: i.id, label: i.label, icon: i.icon))
              .toList(),
        ),
      ],
      ListTile(
        leading: const Icon(Icons.fitness_center),
        title: const Text('Rutinas'),
        selected: _routeId == 'routines',
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        onTap: () => selectRoute('routines'),
      ),
    ];

    final sidebar = AppSidebar(
      user: user,
      activeRole: activeRole,
      onActiveRoleChange: auth.setActiveRole,
      onLogout: auth.logout,
      onProfile: showProfile ? openProfile : null,
      children: sidebarChildren,
    );

    final content = _screenFor(_routeId, isStaff: isStaff);

    final bottomDestinations = <NavigationDestination>[
      const NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Inicio'),
      if (isMember) const NavigationDestination(icon: Icon(Icons.bookmark), label: 'Reservas'),
      const NavigationDestination(icon: Icon(Icons.fitness_center), label: 'Rutinas'),
    ];

    final bottomIndex = switch (_routeId) {
      'serv-reservaciones' || 'reservations' when isMember => 1,
      'routines' || 'serv-rutinas' => isMember ? 2 : 1,
      _ => 0,
    };

    return LayoutBuilder(
      builder: (context, constraints) {
        final wide = constraints.maxWidth >= 768;

        if (wide) {
          return CollapsibleSidebarShell(
            sidebarVisible: layout.sidebarVisible,
            onToggleSidebar: layout.toggleSidebar,
            sidebar: sidebar,
            child: content,
          );
        }

        return Scaffold(
          key: _scaffoldKey,
          drawer: Drawer(child: sidebar),
          body: Stack(
            children: [
              content,
              Positioned(
                top: MediaQuery.paddingOf(context).top + 8,
                left: 8,
                child: IconButton(
                  tooltip: 'Menú',
                  icon: const Icon(Icons.menu),
                  onPressed: () => _scaffoldKey.currentState?.openDrawer(),
                ),
              ),
            ],
          ),
          bottomNavigationBar: NavigationBar(
            selectedIndex: bottomIndex.clamp(0, bottomDestinations.length - 1),
            onDestinationSelected: (i) {
              if (i == 0) selectRoute('dashboard');
              else if (isMember && i == 1) selectRoute('serv-reservaciones');
              else selectRoute('routines');
            },
            destinations: bottomDestinations,
          ),
        );
      },
    );
  }
}
