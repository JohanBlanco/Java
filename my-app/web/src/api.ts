import type { AuthResponse } from './types'

class ApiError extends Error {
  constructor(message: string, public status: number) {
    super(message)
  }
}

function getToken(): string | null {
  return localStorage.getItem('token')
}

const API_BASE = import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? '/api' : 'http://localhost:8080/api')

function parseErrorMessage(status: number, text: string): string {
  if (!text.trim()) {
    if (status === 401) return 'Sesión expirada. Vuelve a iniciar sesión.'
    if (status === 403) return 'No tienes permiso o la sesión expiró.'
    return `Error en la solicitud (${status})`
  }

  try {
    const body = JSON.parse(text) as Record<string, unknown>
    if (typeof body.message === 'string' && body.message !== 'Error de validación') {
      return body.message
    }
    if (body.errors && typeof body.errors === 'object') {
      const details = Object.entries(body.errors as Record<string, string>)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join('\n')
      if (details) return `Revisa los datos:\n${details}`
    }
    if (typeof body.detail === 'string') return body.detail
    if (typeof body.error === 'string') return body.error
    if (typeof body.message === 'string') return body.message
  } catch {
    // respuesta no JSON (HTML, texto plano)
  }

  return text.length > 240 ? `${text.slice(0, 240)}…` : text
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  } catch {
    throw new ApiError(
      `No se pudo conectar con el servidor (${API_BASE}). Verifica que el backend esté en ejecución.`,
      0,
    )
  }

  if (!response.ok) {
    const text = await response.text()
    throw new ApiError(parseErrorMessage(response.status, text), response.status)
  }

  if (response.status === 204) return undefined as T
  return response.json()
}

export const api = {
  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  register: (organizationId: number, data: Record<string, unknown>) =>
    request('/auth/register/' + organizationId, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getPublicOrganizations: () => request<import('./types').Organization[]>('/public/organizations'),

  getPlatformOrganizations: () => request<import('./types').Organization[]>('/platform/organizations'),

  getPlatformOrganization: (id: number) =>
    request<import('./types').Organization>(`/platform/organizations/${id}`),

  createOrganization: (data: Record<string, unknown>) =>
    request('/platform/organizations', { method: 'POST', body: JSON.stringify(data) }),

  updateOrganization: (id: number, data: Record<string, unknown>) =>
    request(`/platform/organizations/${id}`, { method: 'PUT', body: JSON.stringify(data) }),

  getMe: () => request<import('./types').User>('/users/me'),

  updateProfile: (data: Record<string, unknown>) =>
    request('/users/me/profile', { method: 'PUT', body: JSON.stringify(data) }),

  getPackages: () => request<import('./types').MembershipPackage[]>('/packages'),

  createPackage: (data: Record<string, unknown>) =>
    request('/packages', { method: 'POST', body: JSON.stringify(data) }),

  updatePackage: (id: number, data: Record<string, unknown>) =>
    request(`/packages/${id}`, { method: 'PUT', body: JSON.stringify(data) }),

  createActivity: (data: Record<string, unknown>) =>
    request('/activities', { method: 'POST', body: JSON.stringify(data) }),

  updateActivity: (id: number, data: Record<string, unknown>) =>
    request(`/activities/${id}`, { method: 'PUT', body: JSON.stringify(data) }),

  getActivities: (from?: string, to?: string) => {
    const params = new URLSearchParams()
    if (from) params.set('from', from)
    if (to) params.set('to', to)
    const q = params.toString()
    return request<import('./types').Activity[]>(`/activities${q ? `?${q}` : ''}`)
  },

  getActivitySeries: () =>
    request<import('./types').Activity[]>('/activities?series=true'),

  getActivityDeleteImpact: (id: number) =>
    request<import('./types').ActivityReservationImpact>(`/activities/${id}/reservation-impact`),

  previewActivityUpdateImpact: (id: number, data: Record<string, unknown>) =>
    request<import('./types').ActivityReservationImpact>(`/activities/${id}/reservation-impact/preview`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  deleteActivity: (id: number, cancelReservations = false) =>
    request(`/activities/${id}?cancelReservations=${cancelReservations}`, { method: 'DELETE' }),

  editActivityOccurrence: (id: number, data: Record<string, unknown>) =>
    request<import('./types').Activity>(`/activities/${id}/occurrence-edit`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  getSales: () => request<import('./types').Sale[]>('/sales'),

  getStatsSummary: () => request<import('./types').GymStats>('/stats/summary'),

  getMyMembershipUsage: () => request<import('./types').MembershipUsage>('/users/me/membership-usage'),

  getMyReservations: () => request<import('./types').Reservation[]>('/reservations/me'),

  getPendingPaymentReservations: () =>
    request<import('./types').Reservation[]>('/reservations/pending-payment'),

  createReservation: (
    activityId: number,
    options?: { payAtReception?: boolean; occurrenceDate?: string },
  ) =>
    request(`/activities/${activityId}/reservations`, {
      method: 'POST',
      body: JSON.stringify({
        payAtReception: options?.payAtReception ?? false,
        occurrenceDate: options?.occurrenceDate,
      }),
    }),

  markReservationPaid: (id: number) =>
    request(`/reservations/${id}/mark-paid`, { method: 'POST' }),

  confirmReservation: (id: number) =>
    request(`/reservations/${id}/confirm`, { method: 'POST' }),

  cancelReservation: (id: number) =>
    request(`/reservations/${id}/cancel`, { method: 'POST' }),

  getMyRoutines: () => request<import('./types').Routine[]>('/routines/me'),

  createRoutineRequest: (data: { description: string; goals: string }) =>
    request('/routine-requests', { method: 'POST', body: JSON.stringify(data) }),

  getRoutineRequests: () => request<import('./types').RoutineRequest[]>('/routine-requests'),

  updateRoutineRequestStatus: (id: number, status: string) =>
    request(`/routine-requests/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    }),

  assignMembership: (userId: number, membershipPackageId: number) =>
    request(`/users/${userId}/membership`, {
      method: 'POST',
      body: JSON.stringify({ membershipPackageId }),
    }),

  getUsers: () => request<import('./types').User[]>('/users'),

  createUser: (data: Record<string, unknown>) =>
    request('/users', { method: 'POST', body: JSON.stringify(data) }),

  updateUser: (id: number, data: Record<string, unknown>) =>
    request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
}

export { ApiError }
