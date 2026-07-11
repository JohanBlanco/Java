import { useEffect, useState } from 'react'
import { api } from '../../api'
import type { GymStats } from '../../types'

export default function EstadisticasResumenSection() {
  const [stats, setStats] = useState<GymStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.getStatsSummary()
      .then(setStats)
      .catch(() => setStats(null))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Cargando...</p>
  if (!stats) return <div className="empty-state card">No se pudieron cargar las estadísticas</div>

  const bars = [
    { label: 'Miembros', value: stats.memberCount, max: Math.max(stats.memberCount, 1) },
    { label: 'Actividades programadas', value: stats.activitiesScheduled, max: Math.max(stats.activitiesScheduled, 1) },
    { label: 'Reservaciones confirmadas', value: stats.confirmedReservations, max: Math.max(stats.confirmedReservations, 1) },
    { label: 'Asistencias del mes', value: stats.attendancesThisMonth, max: Math.max(stats.attendancesThisMonth, 1) },
  ]

  return (
    <>
      <div className="grid grid-3" style={{ marginBottom: '2rem' }}>
        <div className="card stat-card">
          <div className="value">{stats.memberCount}</div>
          <div className="label">Miembros</div>
        </div>
        <div className="card stat-card">
          <div className="value">{stats.activitiesToday}</div>
          <div className="label">Actividades hoy</div>
        </div>
        <div className="card stat-card">
          <div className="value">{stats.pendingPayments}</div>
          <div className="label">Pagos pendientes</div>
        </div>
        <div className="card stat-card">
          <div className="value">{stats.reservationsToday}</div>
          <div className="label">Reservaciones hoy</div>
        </div>
        <div className="card stat-card">
          <div className="value">{stats.salesToday}</div>
          <div className="label">Ventas hoy</div>
        </div>
        <div className="card stat-card">
          <div className="value">{stats.salesThisMonth}</div>
          <div className="label">Ventas del mes</div>
        </div>
      </div>
      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>Comparativa</h3>
        <div className="stats-bars">
          {bars.map((bar) => (
            <div key={bar.label} className="stats-bar-row">
              <span className="stats-bar-label">{bar.label}</span>
              <div className="stats-bar-track">
                <div className="stats-bar-fill" style={{ width: `${(bar.value / bar.max) * 100}%` }} />
              </div>
              <span className="stats-bar-value">{bar.value}</span>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
