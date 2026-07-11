import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api'
import type { Activity, ActivityReservationImpact } from '../../types'

const WEEKDAYS = [
  { value: 'MONDAY', label: 'Lun' },
  { value: 'TUESDAY', label: 'Mar' },
  { value: 'WEDNESDAY', label: 'Mié' },
  { value: 'THURSDAY', label: 'Jue' },
  { value: 'FRIDAY', label: 'Vie' },
  { value: 'SATURDAY', label: 'Sáb' },
  { value: 'SUNDAY', label: 'Dom' },
] as const

const emptyForm = () => ({
  name: '',
  description: '',
  locationName: '',
  startDate: '',
  endDate: '',
  startTime: '',
  endTime: '',
  recurring: false,
  repeatDays: [] as string[],
  unlimitedCapacity: true,
  capacity: '',
})

function formatSeriesDates(a: Activity): string {
  if (a.recurring) {
    const days = a.repeatDays
      .map((d) => WEEKDAYS.find((w) => w.value === d)?.label ?? d)
      .join(', ')
    return `${a.startDate} → ${a.endDate} · ${days}`
  }
  return a.startDate
}

function formatImpactMessage(impact: ActivityReservationImpact, action: 'edit' | 'delete'): string {
  const lines = impact.items.slice(0, 5).map(
    (item) => `· ${item.occurrenceDate} — ${item.memberName} (${item.status})`,
  )
  if (impact.items.length > 5) {
    lines.push(`· … y ${impact.items.length - 5} más`)
  }

  const intro = action === 'delete'
    ? `Esta actividad tiene ${impact.activeReservations} reservaciones activas.`
    : `Este cambio afectará ${impact.affectedReservations} reservaciones activas.`

  return `${intro}\n\n${lines.join('\n')}\n\n¿Cancelar esas reservaciones y continuar?`
}

export default function ReceptionActividadesSection() {
  const [activities, setActivities] = useState<Activity[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [form, setForm] = useState(emptyForm())
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const isEditing = selectedId !== null

  const load = () => {
    api.getActivitySeries().then(setActivities).catch(() => {})
  }

  useEffect(() => { load() }, [])

  const selectActivity = (a: Activity) => {
    setSelectedId(a.id)
    setForm({
      name: a.name,
      description: a.description ?? '',
      locationName: a.locationName ?? '',
      startDate: a.startDate,
      endDate: a.endDate,
      startTime: a.startTime?.slice(0, 5) ?? '',
      endTime: a.endTime?.slice(0, 5) ?? '',
      recurring: a.recurring,
      repeatDays: [...(a.repeatDays ?? [])],
      unlimitedCapacity: a.capacity == null,
      capacity: a.capacity != null ? String(a.capacity) : '',
    })
  }

  const resetForm = () => {
    setSelectedId(null)
    setForm(emptyForm())
  }

  const toggleDay = (day: string) => {
    setForm((prev) => ({
      ...prev,
      repeatDays: prev.repeatDays.includes(day)
        ? prev.repeatDays.filter((d) => d !== day)
        : [...prev.repeatDays, day],
    }))
  }

  const buildPayload = (confirmAffectedReservations = false) => {
    const payload: Record<string, unknown> = {
      name: form.name,
      description: form.description,
      locationName: form.locationName,
      startDate: form.startDate,
      endDate: form.recurring ? (form.endDate || form.startDate) : form.startDate,
      startTime: form.startTime,
      endTime: form.endTime || null,
      capacity: form.unlimitedCapacity ? null : parseInt(form.capacity, 10) || null,
      recurring: form.recurring,
      repeatDays: form.recurring ? form.repeatDays : [],
    }
    if (confirmAffectedReservations) {
      payload.confirmAffectedReservations = true
    }
    return payload
  }

  const saveActivity = async (confirmAffectedReservations = false) => {
    const payload = buildPayload(confirmAffectedReservations)
    if (isEditing) {
      await api.updateActivity(selectedId, payload)
    } else {
      await api.createActivity(payload)
    }
    resetForm()
    load()
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (form.recurring && !form.endDate) {
      alert('Indica la fecha de fin para actividades recurrentes')
      return
    }
    if (form.recurring && form.repeatDays.length === 0) {
      alert('Selecciona al menos un día de la semana')
      return
    }

    setSaving(true)
    try {
      const payload = buildPayload()
      if (isEditing) {
        const impact = await api.previewActivityUpdateImpact(selectedId, payload)
        if (impact.affectedReservations > 0) {
          const ok = window.confirm(formatImpactMessage(impact, 'edit'))
          if (!ok) return
          await saveActivity(true)
          return
        }
      }
      await saveActivity(false)
    } catch (err) {
      if (err instanceof ApiError) {
        alert(err.message)
      }
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!selectedId) return

    setDeleting(true)
    try {
      const impact = await api.getActivityDeleteImpact(selectedId)
      if (impact.activeReservations > 0) {
        const ok = window.confirm(formatImpactMessage(impact, 'delete'))
        if (!ok) return
        await api.deleteActivity(selectedId, true)
      } else {
        const ok = window.confirm('¿Eliminar esta actividad?')
        if (!ok) return
        await api.deleteActivity(selectedId, false)
      }
      resetForm()
      load()
    } catch (err) {
      if (err instanceof ApiError) {
        alert(err.message)
      }
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="platform-layout admin-section">
      <div className="platform-list">
        {activities.length === 0 ? (
          <div className="empty-state card">No hay actividades registradas.</div>
        ) : (
          <div className="grid grid-2">
            {activities.map((a) => {
              const activeReservations = a.confirmedReservations + a.pendingReservations
              return (
                <div
                  key={a.id}
                  className={`card card-selectable${selectedId === a.id ? ' card-selected' : ''}`}
                  onClick={() => selectActivity(a)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && selectActivity(a)}
                >
                  <h3>
                    {a.name}
                    {a.recurring && (
                      <span className="badge badge-confirmed" style={{ marginLeft: '0.5rem', fontSize: '0.75rem' }}>
                        Recurrente
                      </span>
                    )}
                  </h3>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                    {formatSeriesDates(a)} · {a.startTime}{a.endTime ? ` - ${a.endTime}` : ''}
                  </p>
                  <p style={{ fontSize: '0.9rem' }}>{a.locationName}</p>
                  {a.description && (
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{a.description}</p>
                  )}
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
                    Cupo: {a.capacity ?? 'Sin límite'} · {activeReservations} reservaciones activas
                  </p>
                </div>
              )
            })}
          </div>
        )}
      </div>

      <div className="platform-form-panel">
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3>{isEditing ? 'Editar actividad' : 'Nueva actividad'}</h3>
            {isEditing && (
              <button type="button" className="btn-secondary" onClick={resetForm}>
                Nuevo
              </button>
            )}
          </div>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Nombre</label>
              <input
                value={form.name}
                onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>Ubicación</label>
              <input
                value={form.locationName}
                onChange={(e) => setForm((prev) => ({ ...prev, locationName: e.target.value }))}
                placeholder="Ej: Sala 1, Terraza"
              />
            </div>
            <div className="form-group">
              <label>Fecha de inicio</label>
              <input
                type="date"
                value={form.startDate}
                onChange={(e) => setForm((prev) => ({
                  ...prev,
                  startDate: e.target.value,
                  endDate: prev.recurring ? prev.endDate : e.target.value,
                }))}
                required
              />
            </div>
            <div className="form-group">
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input
                  type="checkbox"
                  checked={form.recurring}
                  onChange={(e) => setForm((prev) => ({
                    ...prev,
                    recurring: e.target.checked,
                    endDate: e.target.checked ? prev.endDate || prev.startDate : prev.startDate,
                    repeatDays: e.target.checked ? prev.repeatDays : [],
                  }))}
                />
                Actividad recurrente
              </label>
            </div>
            {form.recurring && (
              <>
                <div className="form-group">
                  <label>Fecha de fin</label>
                  <input
                    type="date"
                    value={form.endDate}
                    min={form.startDate}
                    onChange={(e) => setForm((prev) => ({ ...prev, endDate: e.target.value }))}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Días de la semana</label>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                    {WEEKDAYS.map((day) => (
                      <button
                        key={day.value}
                        type="button"
                        className={form.repeatDays.includes(day.value) ? 'btn-primary' : 'btn-secondary'}
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.85rem' }}
                        onClick={() => toggleDay(day.value)}
                      >
                        {day.label}
                      </button>
                    ))}
                  </div>
                </div>
              </>
            )}
            <div className="form-group">
              <label>Hora inicio</label>
              <input
                type="time"
                value={form.startTime}
                onChange={(e) => setForm((prev) => ({ ...prev, startTime: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>Hora fin (opcional)</label>
              <input
                type="time"
                value={form.endTime}
                onChange={(e) => setForm((prev) => ({ ...prev, endTime: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label>Descripción</label>
              <textarea
                value={form.description}
                onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                rows={2}
              />
            </div>
            <div className="form-group">
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input
                  type="checkbox"
                  checked={form.unlimitedCapacity}
                  onChange={(e) => setForm((prev) => ({ ...prev, unlimitedCapacity: e.target.checked }))}
                />
                Sin límite de cupo
              </label>
            </div>
            {!form.unlimitedCapacity && (
              <div className="form-group">
                <label>Límite de cupos</label>
                <input
                  type="number"
                  min={1}
                  value={form.capacity}
                  onChange={(e) => setForm((prev) => ({ ...prev, capacity: e.target.value }))}
                  required
                />
              </div>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <button type="submit" className="btn-primary" disabled={saving || deleting} style={{ width: '100%' }}>
                {saving ? 'Guardando...' : isEditing ? 'Guardar cambios' : 'Crear actividad'}
              </button>
              {isEditing && (
                <button
                  type="button"
                  className="btn-secondary"
                  disabled={saving || deleting}
                  style={{ width: '100%', color: 'var(--danger, #c0392b)' }}
                  onClick={handleDelete}
                >
                  {deleting ? 'Eliminando...' : 'Eliminar actividad'}
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
