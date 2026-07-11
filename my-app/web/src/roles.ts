export type GymRole = 'GYM_OWNER' | 'RECEPTIONIST' | 'INSTRUCTOR' | 'MEMBER'
export type PlatformRole = 'PLATFORM_OWNER'
export type AppRole = GymRole | PlatformRole

export const ROLE_LABELS: Record<AppRole, string> = {
  GYM_OWNER: 'Admin',
  RECEPTIONIST: 'Recepcionista',
  INSTRUCTOR: 'Instructor',
  MEMBER: 'Miembro',
  PLATFORM_OWNER: 'Plataforma',
}

export const GYM_ROLES: GymRole[] = ['GYM_OWNER', 'RECEPTIONIST', 'INSTRUCTOR', 'MEMBER']

const ROLE_PRIORITY: AppRole[] = ['PLATFORM_OWNER', 'GYM_OWNER', 'RECEPTIONIST', 'INSTRUCTOR', 'MEMBER']

export function normalizeRoles(user: { roles?: string[]; role?: string } | null | undefined): string[] {
  if (!user) return []
  if (user.roles?.length) return user.roles
  if (user.role) return [user.role]
  return []
}

export function getSwitchableRoles(user: { roles?: string[]; role?: string } | null | undefined): GymRole[] {
  return normalizeRoles(user).filter((role): role is GymRole => GYM_ROLES.includes(role as GymRole))
}

export function resolveDefaultActiveRole(roles: string[]): string | null {
  for (const role of ROLE_PRIORITY) {
    if (roles.includes(role)) return role
  }
  return roles[0] ?? null
}

export function resolveActiveRole(
  user: { roles?: string[]; role?: string } | null | undefined,
  storedActiveRole?: string | null,
): string | null {
  const roles = normalizeRoles(user)
  if (storedActiveRole && roles.includes(storedActiveRole)) return storedActiveRole
  return resolveDefaultActiveRole(roles)
}

export function hasRole(user: { roles?: string[]; role?: string } | null | undefined, role: string): boolean {
  return normalizeRoles(user).includes(role)
}

export function hasAnyRole(user: { roles?: string[]; role?: string } | null | undefined, roles: string[]): boolean {
  const userRoles = normalizeRoles(user)
  return roles.some((role) => userRoles.includes(role))
}

export function formatRoles(roles: string[]): string {
  return roles
    .filter((role) => role !== 'PLATFORM_OWNER')
    .map((role) => ROLE_LABELS[role as AppRole] ?? role)
    .join(', ')
}

export function canViewReception(activeRole: string | null | undefined): boolean {
  return activeRole != null && ['GYM_OWNER', 'RECEPTIONIST'].includes(activeRole)
}

export const canViewVentas = canViewReception
export const canViewEstadisticas = canViewReception

export function canViewProfile(activeRole: string | null | undefined): boolean {
  return activeRole != null && ['INSTRUCTOR', 'MEMBER', 'RECEPTIONIST'].includes(activeRole)
}

export function canViewAdmin(activeRole: string | null | undefined): boolean {
  return activeRole === 'GYM_OWNER'
}

export function isMemberView(activeRole: string | null | undefined): boolean {
  return activeRole === 'MEMBER'
}

export function isStaffView(activeRole: string | null | undefined): boolean {
  return activeRole != null && ['GYM_OWNER', 'INSTRUCTOR', 'RECEPTIONIST'].includes(activeRole)
}

export function profileLabel(activeRole: string | null | undefined): string {
  if (!activeRole) return 'Usuario'
  return ROLE_LABELS[activeRole as AppRole] ?? activeRole
}
