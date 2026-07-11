export type NavSection = {
  path: string
  label: string
  description?: string
}

export const VENTAS_SECTIONS: NavSection[] = [
  { path: 'registro', label: 'Registro de ventas', description: 'Pagos de actividades en recepción' },
]

export const ESTADISTICAS_SECTIONS: NavSection[] = [
  { path: 'resumen', label: 'Resumen general', description: 'Indicadores operativos del gimnasio' },
]

export const RECEPTION_SECTIONS: NavSection[] = [
  { path: 'pagos-pendientes', label: 'Pagos pendientes', description: 'Cobros por confirmar en recepción' },
  { path: 'actividades', label: 'Actividades', description: 'Clases y eventos con cupo y recurrencia' },
  { path: 'actividades-hoy', label: 'Actividades del día', description: 'Clases programadas para hoy' },
  { path: 'calendario', label: 'Calendario', description: 'Vista de actividades por periodo' },
]

export const ADMIN_SECTIONS: NavSection[] = [
  { path: 'membresias', label: 'Membresías', description: 'Planes de acceso y actividades incluidas' },
  { path: 'usuarios', label: 'Usuarios', description: 'Personal y miembros del gimnasio' },
]

export const DEFAULT_PASSWORD = '12345678'
