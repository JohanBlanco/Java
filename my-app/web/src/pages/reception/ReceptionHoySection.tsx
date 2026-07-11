import { useEffect, useMemo, useState } from 'react'
import { api } from '../../api'
import type { Activity } from '../../types'
import { toIsoDate } from '../../utils/calendarUtils'

export default function ReceptionHoySection() {
  const [activities, setActivities] = useState<Activity[]>([])
  const [loading, setLoading] = useState(true)

  const todayIso = toIsoDate(new Date())
  const todayActivities = useMemo(
    () => activities.filter((a) => a.activityDate === todayIso),
    [activities, todayIso],
  )

  useEffect(() => {
    api.getActivities(todayIso, todayIso)
      .then(setActivities)
      .catch(() => setActivities([]))
      .finally(() => setLoading(false))
  }, [todayIso])

  if (loading) return <p>Cargando...</p>

  return (
    <div className="grid grid-2">
      {todayActivities.length === 0 ? (
        <div className="empty-state card">No hay actividades programadas para hoy</div>
      ) : todayActivities.map((a) => (
        <div key={`${a.id}-${a.activityDate}`} className="card">
          <h3>{a.name}</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            {a.startTime?.slice(0, 5)} – {a.endTime?.slice(0, 5)} · {a.locationName}
          </p>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Cupo: {a.capacity ?? 'Ilimitado'} · {a.confirmedReservations} confirmados
          </p>
        </div>
      ))}
    </div>
  )
}
