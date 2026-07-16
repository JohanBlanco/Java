export type NavSection = {
  path: string
  label: string
  description?: string
  badge?: string
  /** Si es true, se muestra como submenú colapsable (como Administración). */
  collapsible?: boolean
  children?: NavSection[]
}

export const VENTAS_SECTIONS: NavSection[] = [
  {
    path: 'punto-de-venta',
    label: 'Punto de venta',
    description: 'Cobra productos y membresías en recepción',
  },
  {
    path: 'historial',
    label: 'Ventas',
    description: 'Historial del día, mes y año; ingresos y gastos',
  },
]

export const ESTADISTICAS_SECTIONS: NavSection[] = [
  { path: 'resumen', label: 'Resumen general', description: 'Indicadores operativos del gimnasio' },
]

export const MEMBER_SECTIONS: NavSection[] = [
  { path: 'actividades', label: 'Actividades', description: 'Clases y eventos disponibles' },
  { path: 'reservaciones', label: 'Mis reservaciones', description: 'Tus cupos en actividades' },
  { path: 'rutinas', label: 'Mis rutinas', description: 'Rutinas asignadas por tu instructor' },
  { path: 'medidas', label: 'Mis medidas', description: 'Composición corporal y recomendaciones', badge: 'Beta' },
  { path: 'nutricion', label: 'Mi nutrición', description: 'Plan alimenticio asignado por tu instructor', badge: 'Beta' },
  { path: 'solicitudes-citas', label: 'Citas', description: 'Consulta tus citas agendadas' },
]

export const RECEPTION_SECTIONS: NavSection[] = [
  {
    path: 'usuarios',
    label: 'Usuarios',
    collapsible: true,
    children: [
      {
        path: 'usuarios',
        label: 'Personal y miembros',
        description: 'Personal y miembros del gimnasio',
      },
      {
        path: 'expedientes',
        label: 'Expedientes',
        description: 'Formularios completados por cada miembro. Visualiza y descarga en PDF.',
      },
    ],
  },
  { path: 'productos', label: 'Productos', description: 'Inventario y catálogo de la tienda' },
  { path: 'membresias', label: 'Membresías', description: 'Planes de acceso y actividades incluidas' },
]

/** Secciones de recepción en lista plana (incluye anidadas; omite nodos solo-colapso). */
export function flattenNavSections(sections: NavSection[]): NavSection[] {
  const out: NavSection[] = []
  for (const section of sections) {
    if (section.collapsible && section.children?.length) {
      out.push(...flattenNavSections(section.children))
      continue
    }
    out.push(section)
    if (section.children?.length) {
      out.push(...flattenNavSections(section.children))
    }
  }
  return out
}

export const AGENDA_CITAS_SECTION: NavSection = {
  path: 'citas',
  label: 'Citas',
  description: 'Agenda, disponibilidad y solicitudes de cita',
}

export const AGENDA_ACTIVIDADES_SECTION: NavSection = {
  path: 'actividades',
  label: 'Actividades',
  description: 'Clases y eventos por periodo',
}

export function getAgendaSections(activeRole: string | null | undefined): NavSection[] {
  const sections: NavSection[] = []
  if (activeRole != null && ['GYM_OWNER', 'RECEPTIONIST', 'INSTRUCTOR'].includes(activeRole)) {
    sections.push(AGENDA_CITAS_SECTION)
  }
  if (activeRole != null && ['GYM_OWNER', 'RECEPTIONIST'].includes(activeRole)) {
    sections.push(AGENDA_ACTIVIDADES_SECTION)
  }
  return sections
}

export function canViewAgenda(activeRole: string | null | undefined): boolean {
  return getAgendaSections(activeRole).length > 0
}

export const TRAINING_SECTIONS: NavSection[] = [
  { path: 'rutinas', label: 'Rutinas', description: 'Solicitudes y rutinas personalizadas' },
  { path: 'medidas', label: 'Medidas', description: 'Mediciones corporales y análisis', badge: 'Beta' },
  { path: 'nutricion', label: 'Nutrición', description: 'Planes alimenticios personalizados', badge: 'Beta' },
]

export const DEFAULT_PASSWORD = '12345678'
