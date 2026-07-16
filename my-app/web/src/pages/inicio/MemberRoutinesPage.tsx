import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api'
import RoutineDisplay from '../../components/RoutineDisplay'
import { useFilteredList } from '../../hooks/useFilteredList'
import { useToast } from '../../toast'
import type { Routine, User } from '../../types'

const emptyForm = () => ({
  description: '',
  goals: '',
  additionalNotes: '',
  preferredInstructorId: '',
})

export default function MemberRoutinesPage() {
  const { showSuccess, showApiError } = useToast()
  const [routines, setRoutines] = useState<Routine[]>([])
  const [instructors, setInstructors] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(emptyForm())
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    Promise.all([
      api.getMyRoutines().then(setRoutines).catch(() => setRoutines([])),
      api.getUsers()
        .then((users) => setInstructors(
          users.filter((u) => u.active && (
            u.roles.includes('INSTRUCTOR') || u.roles.includes('GYM_OWNER')
          )),
        ))
        .catch(() => setInstructors([])),
    ]).finally(() => setLoading(false))
  }, [])

  const { filtered, filterInput } = useFilteredList(routines)

  const openForm = () => {
    setForm(emptyForm())
    setShowForm(true)
  }

  const submitRequest = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      await api.createRoutineRequest({
        description: form.description.trim(),
        goals: form.goals.trim(),
        additionalNotes: form.additionalNotes.trim() || undefined,
        preferredInstructorId: form.preferredInstructorId
          ? Number(form.preferredInstructorId)
          : undefined,
      })
      setShowForm(false)
      setForm(emptyForm())
      showSuccess('Solicitud enviada. Tu instructor te asignará un plan pronto.')
    } catch (err) {
      showApiError(err, 'No se pudo enviar la solicitud')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <p>Cargando...</p>

  return (
    <>
      {filterInput}
      <div className="grid grid-2">
        {routines.length === 0 ? (
          <div className="empty-state card">
            <p>No tienes rutinas asignadas</p>
            <button type="button" className="btn-primary" style={{ marginTop: '1rem' }} onClick={openForm}>
              Solicitar rutina
            </button>
          </div>
        ) : filtered.length === 0 ? (
          <div className="empty-state card">Ningún resultado coincide con la búsqueda</div>
        ) : filtered.map((r) => (
          <div key={r.id} className="card">
            <RoutineDisplay routine={r} />
          </div>
        ))}
      </div>
      {routines.length > 0 && (
        <p style={{ marginTop: '1rem', fontSize: '0.9rem', color: 'var(--text-muted)' }}>
          ¿Necesitas otra rutina?{' '}
          <button type="button" className="btn-secondary" style={{ marginLeft: '0.35rem' }} onClick={openForm}>
            Solicitar rutina
          </button>
        </p>
      )}

      {showForm && (
        <div className="modal-overlay" role="presentation" onClick={() => setShowForm(false)}>
          <div
            className="card modal-card"
            role="dialog"
            aria-labelledby="routine-request-title"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 id="routine-request-title">Solicitar rutina</h3>
            <form onSubmit={submitRequest}>
              <div className="form-group">
                <label>¿Qué necesitas?</label>
                <textarea
                  value={form.description}
                  onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
                  rows={3}
                  required
                  placeholder="Ej: Rutina de fuerza para 3 días por semana"
                />
              </div>
              <div className="form-group">
                <label>Objetivos</label>
                <textarea
                  value={form.goals}
                  onChange={(e) => setForm((p) => ({ ...p, goals: e.target.value }))}
                  rows={2}
                  required
                  placeholder="Ej: Hipertrofia, perder grasa, mejorar resistencia..."
                />
              </div>
              <div className="form-group">
                <label>Notas adicionales (opcional)</label>
                <textarea
                  value={form.additionalNotes}
                  onChange={(e) => setForm((p) => ({ ...p, additionalNotes: e.target.value }))}
                  rows={2}
                  placeholder="Lesiones, horarios, equipamiento disponible..."
                />
              </div>
              <div className="form-group">
                <label>Instructor de preferencia (opcional)</label>
                <select
                  value={form.preferredInstructorId}
                  onChange={(e) => setForm((p) => ({ ...p, preferredInstructorId: e.target.value }))}
                >
                  <option value="">Cualquier instructor</option>
                  {instructors.map((i) => (
                    <option key={i.id} value={i.id}>{i.firstName} {i.lastName}</option>
                  ))}
                </select>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                <button type="button" className="btn-secondary" style={{ flex: 1 }} onClick={() => setShowForm(false)}>
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  style={{ flex: 1 }}
                  disabled={submitting}
                >
                  {submitting ? 'Enviando...' : 'Enviar solicitud'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
