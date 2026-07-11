import { useState } from 'react'
import { api, ApiError } from '../api'
import type { Activity } from '../types'

type Scope = 'OCCURRENCE' | 'SERIES'

type Props = {
  activity: Activity
  onClose: () => void
  onSaved: () => void
}

export default function ActivityOccurrenceEditModal({ activity, onClose, onSaved }: Props) {
  const [startTime, setStartTime] = useState(activity.startTime?.slice(0, 5) ?? '')
  const [endTime, setEndTime] = useState(activity.endTime?.slice(0, 5) ?? '')
  const [locationName, setLocationName] = useState(activity.locationName ?? '')
  const [unlimitedCapacity, setUnlimitedCapacity] = useState(activity.capacity == null)
  const [capacity, setCapacity] = useState(activity.capacity != null ? String(activity.capacity) : '')
  const [scope, setScope] = useState<Scope>('OCCURRENCE')
  const [saving, setSaving] = useState(false)

  const showScopeChoice = activity.recurring

  const buildPayload = (confirmAffectedReservations = false) => ({
    occurrenceDate: activity.activityDate,
    startTime,
    endTime: endTime || null,
    locationName,
    capacity: unlimitedCapacity ? null : parseInt(capacity, 10) || null,
    scope: showScopeChoice ? scope : 'SERIES',
    confirmAffectedReservations,
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      await api.editActivityOccurrence(activity.id, buildPayload())
      onSaved()
      onClose()
    } catch (err) {
      if (err instanceof ApiError) {
        alert(err.message)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose} role="presentation">
      <div className="modal card" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
          <div>
            <h3 style={{ marginBottom: '0.25rem' }}>{activity.name}</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>
              {activity.activityDate}
              {activity.hasOccurrenceOverride && (
                <span className="badge badge-pending" style={{ marginLeft: '0.5rem' }}>Modificada</span>
              )}
            </p>
          </div>
          <button type="button" className="btn-secondary" onClick={onClose} aria-label="Cerrar">✕</button>
        </div>

        <form onSubmit={handleSubmit}>
          {showScopeChoice && (
            <div className="form-group">
              <label>Aplicar cambios a</label>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '0.35rem' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="scope"
                    checked={scope === 'OCCURRENCE'}
                    onChange={() => setScope('OCCURRENCE')}
                  />
                  Solo esta clase ({activity.activityDate})
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="scope"
                    checked={scope === 'SERIES'}
                    onChange={() => setScope('SERIES')}
                  />
                  Toda la serie (todas las fechas futuras con el horario base)
                </label>
              </div>
            </div>
          )}

          <div className="form-group">
            <label>Hora inicio</label>
            <input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Hora fin (opcional)</label>
            <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Ubicación</label>
            <input value={locationName} onChange={(e) => setLocationName(e.target.value)} />
          </div>
          <div className="form-group">
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                checked={unlimitedCapacity}
                onChange={(e) => setUnlimitedCapacity(e.target.checked)}
              />
              Sin límite de cupo
            </label>
          </div>
          {!unlimitedCapacity && (
            <div className="form-group">
              <label>Cupo</label>
              <input
                type="number"
                min={1}
                value={capacity}
                onChange={(e) => setCapacity(e.target.value)}
                required
              />
            </div>
          )}

          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
            <button type="button" className="btn-secondary" onClick={onClose} style={{ flex: 1 }}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={saving} style={{ flex: 1 }}>
              {saving ? 'Guardando...' : 'Guardar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
