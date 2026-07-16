import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api'
import type { Activity, AppointmentRequest, MembershipUsage, RoutineRequest, User } from '../types'
import { useAuth } from '../auth'
import { canViewReception, isMemberView, isStaffView, profileLabel } from '../roles'
import { bookedAppointmentsForDay, rangeToQuery } from '../utils/appointmentCalendarUtils'
import { toIsoDate } from '../utils/calendarUtils'
import { isRoutineRequestOpen } from '../utils/routineRequest'
import TodayActivitiesPanel from '../components/dashboard/TodayActivitiesPanel'
import RoutineRequestsPanel from '../components/dashboard/RoutineRequestsPanel'
import TodayAppointmentsPanel from '../components/dashboard/TodayAppointmentsPanel'

import PendingPaymentsPanel from '../components/dashboard/PendingPaymentsPanel'

type OpsPanel = 'actividades-hoy' | 'pendientes-de-pago' | 'solicitudes-rutina' | 'solicitudes-citas'

const PANEL_LABELS: Record<OpsPanel, string> = {
  'pendientes-de-pago': 'Pendientes de pago',
  'solicitudes-citas': 'Citas del día',
  'actividades-hoy': 'Actividades del día',
  'solicitudes-rutina': 'Solicitudes de rutina',
}

const OPS_PANEL_ORDER: OpsPanel[] = [
  'pendientes-de-pago',
  'solicitudes-citas',
  'actividades-hoy',
  'solicitudes-rutina',
]

export default function DashboardPage() {
  const { user, activeRole } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [activities, setActivities] = useState<Activity[]>([])
  const [membershipUsage, setMembershipUsage] = useState<MembershipUsage | null>(null)
  const [routinesCount, setRoutinesCount] = useState(0)
  const [reservationsCount, setReservationsCount] = useState(0)
  const [routineRequests, setRoutineRequests] = useState<RoutineRequest[]>([])
  const [appointmentRequests, setAppointmentRequests] = useState<AppointmentRequest[]>([])
  const [pendingPayments, setPendingPayments] = useState<User[]>([])
  const [loading, setLoading] = useState(true)

  const isMember = isMemberView(activeRole)
  const isStaff = isStaffView(activeRole)
  const showOpsHome = canViewReception(activeRole)

  const panelParam = searchParams.get('panel') as OpsPanel | null
  const defaultPanel: OpsPanel = showOpsHome ? 'pendientes-de-pago' : 'solicitudes-rutina'
  const activePanel: OpsPanel = panelParam && PANEL_LABELS[panelParam] ? panelParam : defaultPanel

  const setPanel = (panel: OpsPanel) => {
    setSearchParams(panel === defaultPanel ? {} : { panel }, { replace: true })
  }

  const refreshCounts = useCallback(() => {
    const todayIso = toIsoDate(new Date())
    const promises: Promise<void>[] = []

    if (showOpsHome) {
      promises.push(api.getActivities(todayIso, todayIso).then(setActivities).catch(() => {}))
      promises.push(api.getPendingMembershipPayment().then(setPendingPayments).catch(() => {}))
      promises.push(api.getRoutineRequests().then(setRoutineRequests).catch(() => {}))
      promises.push(
        api.getAppointmentRequests(rangeToQuery(new Date(), new Date())).then(setAppointmentRequests).catch(() => {}),
      )
    } else if (isStaff) {
      promises.push(api.getRoutineRequests().then(setRoutineRequests).catch(() => {}))
    }

    return Promise.all(promises)
  }, [showOpsHome, isStaff])

  useEffect(() => {
    const promises: Promise<void>[] = []
    const todayIso = toIsoDate(new Date())

    if (showOpsHome) {
      promises.push(api.getActivities(todayIso, todayIso).then(setActivities).catch(() => {}))
      promises.push(api.getPendingMembershipPayment().then(setPendingPayments).catch(() => {}))
      promises.push(api.getRoutineRequests().then(setRoutineRequests).catch(() => {}))
      promises.push(
        api.getAppointmentRequests(rangeToQuery(new Date(), new Date())).then(setAppointmentRequests).catch(() => {}),
      )
    } else if (isMember) {
      promises.push(api.getActivities().then(setActivities).catch(() => {}))
      promises.push(
        api.getMyReservations()
          .then((rows) => setReservationsCount(rows.filter((r) => r.status !== 'CANCELLED').length))
          .catch(() => {}),
      )
      promises.push(api.getMyMembershipUsage().then(setMembershipUsage).catch(() => {}))
      promises.push(api.getMyRoutines().then((rows) => setRoutinesCount(rows.length)).catch(() => {}))
    } else if (isStaff) {
      promises.push(api.getRoutineRequests().then(setRoutineRequests).catch(() => {}))
    }

    Promise.all(promises).finally(() => setLoading(false))
  }, [isMember, isStaff, showOpsHome])

  if (loading) return <p>Cargando...</p>

  const pendingRoutineCount = routineRequests.filter((r) => isRoutineRequestOpen(r.status)).length
  const todayAppointmentCount = bookedAppointmentsForDay(appointmentRequests, new Date()).length

  const todayActivityCount = activities.filter((a) => a.activityDate === toIsoDate(new Date()) && a.active !== false).length

  const statCard = (panel: OpsPanel, value: number, label: string) => (
    <button
      key={panel}
      type="button"
      className={`card stat-card stat-card-action${activePanel === panel ? ' active' : ''}`}
      onClick={() => setPanel(panel)}
    >
      <div className="value">{value}</div>
      <div className="label">{label}</div>
    </button>
  )

  return (
    <div>
      <div className="page-header">
        <h1>Hola, {user?.firstName}</h1>
        <p>Perfil: {profileLabel(activeRole)}</p>
      </div>

      {isMember && membershipUsage?.membershipName && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ marginBottom: '0.5rem' }}>Membresía: {membershipUsage.membershipName}</h3>
          {membershipUsage.unlimitedFreeActivities ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              Actividades gratuitas incluidas: ilimitadas
            </p>
          ) : (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              Actividades gratuitas: {membershipUsage.freeActivitiesUsed} usadas ·{' '}
              {membershipUsage.freeActivitiesRemaining ?? 0} restantes de {membershipUsage.freeActivityQuota}
            </p>
          )}
        </div>
      )}

      {showOpsHome && (
        <>
          <div className="grid grid-2 grid-ops-stats" style={{ marginBottom: '1.5rem' }}>
            {OPS_PANEL_ORDER.map((panel) => {
              const values: Record<OpsPanel, number> = {
                'pendientes-de-pago': pendingPayments.length,
                'solicitudes-citas': todayAppointmentCount,
                'actividades-hoy': todayActivityCount,
                'solicitudes-rutina': pendingRoutineCount,
              }
              return statCard(panel, values[panel], PANEL_LABELS[panel])
            })}
          </div>
          <div className="dashboard-panel card">
            <h2 className="section-title">{PANEL_LABELS[activePanel]}</h2>
            {activePanel === 'pendientes-de-pago' && <PendingPaymentsPanel />}
            {activePanel === 'solicitudes-citas' && (
              <TodayAppointmentsPanel onChanged={() => { refreshCounts() }} />
            )}
            {activePanel === 'actividades-hoy' && (
              <TodayActivitiesPanel onChanged={() => { refreshCounts() }} />
            )}
            {activePanel === 'solicitudes-rutina' && (
              <RoutineRequestsPanel onChanged={() => { refreshCounts() }} />
            )}
          </div>
        </>
      )}

      {isStaff && !showOpsHome && (
        <>
          <div className="grid grid-3" style={{ marginBottom: '1.5rem' }}>
            {statCard('solicitudes-rutina', pendingRoutineCount, 'Solicitudes pendientes')}
          </div>
          <div className="dashboard-panel card">
            <h2 className="section-title">{PANEL_LABELS['solicitudes-rutina']}</h2>
            <RoutineRequestsPanel onChanged={() => { refreshCounts() }} />
          </div>
        </>
      )}

      {isMember && (
        <div className="grid grid-3">
          <div className="card stat-card">
            <div className="value">{activities.length}</div>
            <div className="label">Actividades</div>
          </div>
          <div className="card stat-card">
            <div className="value">{reservationsCount}</div>
            <div className="label">Mis reservaciones</div>
          </div>
          <div className="card stat-card">
            <div className="value">{routinesCount}</div>
            <div className="label">Mis rutinas</div>
          </div>
        </div>
      )}
    </div>
  )
}
