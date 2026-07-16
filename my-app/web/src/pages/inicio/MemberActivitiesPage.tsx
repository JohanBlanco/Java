import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api'
import ActivityCapacityDisplay from '../../components/ActivityCapacityDisplay'
import { useFilteredList } from '../../hooks/useFilteredList'
import { useDateFormat } from '../../preferences/useDateFormat'
import { useToast } from '../../toast'
import type { Activity } from '../../types'

export default function MemberActivitiesPage() {
  const { formatIsoDate } = useDateFormat()
  const { showSuccess, showApiError } = useToast()
  const [activities, setActivities] = useState<Activity[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.getActivities()
      .then(setActivities)
      .catch(() => setActivities([]))
      .finally(() => setLoading(false))
  }, [])

  const { filtered, filterInput } = useFilteredList(activities)

  const handleReserve = async (activity: Activity) => {
    const occurrenceDate = activity.activityDate
    try {
      await api.createReservation(activity.id, { occurrenceDate })
      showSuccess('Reservación creada')
    } catch (e) {
      if (e instanceof ApiError && e.message.includes('Debes pagar en recepción')) {
        const pay = window.confirm(`${e.message}\n\n¿Deseas reservar y pagar en recepción?`)
        if (pay) {
          await api.createReservation(activity.id, { payAtReception: true, occurrenceDate })
          showSuccess('Reservación creada. Paga en recepción para confirmar.')
        }
      } else {
        showApiError(e, 'Error al reservar')
      }
    }
  }

  if (loading) return <p>Cargando...</p>

  return (
    <>
      {filterInput}
      <div className="grid grid-2">
        {activities.length === 0 ? (
          <div className="empty-state card">No hay actividades programadas</div>
        ) : filtered.length === 0 ? (
          <div className="empty-state card">Ningún resultado coincide con la búsqueda</div>
        ) : filtered.map((a) => (
          <div key={`${a.id}-${a.activityDate}`} className="card">
            <h3>{a.name}</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              {formatIsoDate(a.activityDate)} · {a.startTime} - {a.endTime}
            </p>
            <p style={{ fontSize: '0.9rem' }}>{a.locationName}</p>
            {a.description && (
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>{a.description}</p>
            )}
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              <ActivityCapacityDisplay activity={a} />
            </p>
            {a.hasCapacity && (
              <button className="btn-primary" style={{ marginTop: '0.75rem' }} onClick={() => handleReserve(a)}>
                Reservar
              </button>
            )}
          </div>
        ))}
      </div>
    </>
  )
}
