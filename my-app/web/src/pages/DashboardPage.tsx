import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'
import type { Activity, MembershipUsage, Reservation, Routine, RoutineRequest } from '../types'
import { useAuth } from '../auth'
import { isMemberView, isStaffView, profileLabel } from '../roles'

export default function DashboardPage() {
  const { user, activeRole } = useAuth()
  const [activities, setActivities] = useState<Activity[]>([])
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [membershipUsage, setMembershipUsage] = useState<MembershipUsage | null>(null)
  const [routines, setRoutines] = useState<Routine[]>([])
  const [requests, setRequests] = useState<RoutineRequest[]>([])
  const [loading, setLoading] = useState(true)

  const isMember = isMemberView(activeRole)
  const isStaff = isStaffView(activeRole)

  const refreshMemberData = async () => {
    const [updatedReservations, usage] = await Promise.all([
      api.getMyReservations(),
      api.getMyMembershipUsage(),
    ])
    setReservations(updatedReservations)
    setMembershipUsage(usage)
  }

  useEffect(() => {
    const promises: Promise<void>[] = []

    promises.push(api.getActivities().then(setActivities).catch(() => {}))
    if (isMember) {
      promises.push(refreshMemberData().catch(() => {}))
      promises.push(api.getMyRoutines().then(setRoutines).catch(() => {}))
    }
    if (isStaff) {
      promises.push(api.getRoutineRequests().then(setRequests).catch(() => {}))
    }

    Promise.all(promises).finally(() => setLoading(false))
  }, [isMember, isStaff])

  const handleReserve = async (activity: Activity) => {
    const occurrenceDate = activity.activityDate
    try {
      await api.createReservation(activity.id, { occurrenceDate })
      await refreshMemberData()
    } catch (e) {
      if (e instanceof ApiError && e.message.includes('Debes pagar en recepción')) {
        const pay = window.confirm(`${e.message}\n\n¿Deseas reservar y pagar en recepción?`)
        if (pay) {
          await api.createReservation(activity.id, { payAtReception: true, occurrenceDate })
          await refreshMemberData()
        }
      } else {
        alert(e instanceof ApiError ? e.message : 'Error al reservar')
      }
    }
  }

  const handleConfirm = async (id: number) => {
    try {
      await api.confirmReservation(id)
      await refreshMemberData()
    } catch (e) {
      alert(e instanceof ApiError ? e.message : 'Error al confirmar')
    }
  }

  const handleCancel = async (id: number) => {
    await api.cancelReservation(id)
    await refreshMemberData()
  }

  const handleRequestRoutine = async () => {
    await api.createRoutineRequest({
      description: 'Necesito una rutina personalizada',
      goals: 'Mejorar fuerza y resistencia',
    })
    if (isStaff) setRequests(await api.getRoutineRequests())
  }

  if (loading) return <p>Cargando...</p>

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

      <div className="grid grid-3" style={{ marginBottom: '2rem' }}>
        <div className="card stat-card">
          <div className="value">{activities.length}</div>
          <div className="label">Actividades</div>
        </div>
        {isMember && (
          <>
            <div className="card stat-card">
              <div className="value">{reservations.filter(r => r.status !== 'CANCELLED').length}</div>
              <div className="label">Mis reservaciones</div>
            </div>
            <div className="card stat-card">
              <div className="value">{routines.length}</div>
              <div className="label">Mis rutinas</div>
            </div>
          </>
        )}
        {isStaff && (
          <div className="card stat-card">
            <div className="value">{requests.filter(r => r.status === 'PENDING').length}</div>
            <div className="label">Solicitudes pendientes</div>
          </div>
        )}
      </div>

      <h2 style={{ marginBottom: '1rem' }}>Actividades</h2>
      <div className="grid grid-2" style={{ marginBottom: '2rem' }}>
        {activities.length === 0 ? (
          <div className="empty-state card">No hay actividades programadas</div>
        ) : activities.map((a) => (
          <div key={`${a.id}-${a.activityDate}`} className="card">
            <h3>{a.name}</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              {a.activityDate} · {a.startTime} - {a.endTime}
            </p>
            <p style={{ fontSize: '0.9rem' }}>{a.locationName}</p>
            {a.description && (
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>{a.description}</p>
            )}
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Cupo: {a.capacity ?? 'Ilimitado'} · {a.confirmedReservations} confirmados
            </p>
            {isMember && a.hasCapacity && (
              <button className="btn-primary" style={{ marginTop: '0.75rem' }} onClick={() => handleReserve(a)}>
                Reservar
              </button>
            )}
          </div>
        ))}
      </div>

      {isMember && (
        <>
          <h2 style={{ marginBottom: '1rem' }}>Mis reservaciones</h2>
          <div className="grid grid-2" style={{ marginBottom: '2rem' }}>
            {reservations.length === 0 ? (
              <div className="empty-state card">Sin reservaciones</div>
            ) : reservations.map((r) => (
              <div key={r.id} className="card">
                <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <h3>{r.activityName}</h3>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{r.occurrenceDate}</p>
                  <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                    <span className={`badge badge-${r.status.toLowerCase()}`}>{r.status}</span>
                    {r.freeSlot && r.status !== 'CANCELLED' && (
                      <span className="badge badge-confirmed">Gratis</span>
                    )}
                    {r.paymentRequired && !r.paid && r.status !== 'CANCELLED' && (
                      <span className="badge badge-pending">Pago pendiente</span>
                    )}
                  </div>
                </div>
                {r.status === 'PENDING' && (
                  <div className="actions">
                    {r.paymentRequired && !r.paid ? (
                      <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                        Paga en recepción para confirmar tu cupo.
                      </p>
                    ) : (
                      <button className="btn-primary" onClick={() => handleConfirm(r.id)}>Confirmar</button>
                    )}
                    <button className="btn-danger" onClick={() => handleCancel(r.id)}>Cancelar</button>
                  </div>
                )}
                {r.status === 'CONFIRMED' && (
                  <button className="btn-danger" style={{ marginTop: '0.75rem' }} onClick={() => handleCancel(r.id)}>
                    Cancelar
                  </button>
                )}
              </div>
            ))}
          </div>

          <h2 style={{ marginBottom: '1rem' }}>Mis rutinas</h2>
          <div className="grid grid-2" style={{ marginBottom: '2rem' }}>
            {routines.length === 0 ? (
              <div className="empty-state card">
                <p>No tienes rutinas asignadas</p>
                <button className="btn-primary" style={{ marginTop: '1rem' }} onClick={handleRequestRoutine}>
                  Solicitar rutina
                </button>
              </div>
            ) : routines.map((r) => (
              <div key={r.id} className="card">
                <h3>{r.name}</h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                  Instructor: {r.instructorName} {r.temporary && '(Temporal)'}
                </p>
                <ul style={{ marginTop: '0.75rem', paddingLeft: '1.25rem', fontSize: '0.9rem' }}>
                  {r.exercises.map((ex) => (
                    <li key={ex.id}>{ex.exerciseName} — {ex.sets}x{ex.reps}</li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </>
      )}

      {isStaff && (
        <>
          <h2 style={{ marginBottom: '1rem' }}>Solicitudes de rutina</h2>
          <div className="grid grid-2">
            {requests.length === 0 ? (
              <div className="empty-state card">Sin solicitudes</div>
            ) : requests.map((r) => (
              <div key={r.id} className="card">
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <h3>{r.memberName}</h3>
                  <span className={`badge badge-${r.status === 'PENDING' ? 'pending' : 'confirmed'}`}>{r.status}</span>
                </div>
                <p style={{ fontSize: '0.9rem', marginTop: '0.5rem' }}>{r.description}</p>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Objetivos: {r.goals}</p>
                {r.status === 'PENDING' && (
                  <button
                    className="btn-primary"
                    style={{ marginTop: '0.75rem' }}
                    onClick={async () => {
                      await api.updateRoutineRequestStatus(r.id, 'IN_PROGRESS')
                      setRequests(await api.getRoutineRequests())
                    }}
                  >
                    Tomar solicitud
                  </button>
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
