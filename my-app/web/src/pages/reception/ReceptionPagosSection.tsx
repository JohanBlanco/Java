import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api'
import type { Reservation } from '../../types'

export default function ReceptionPagosSection() {
  const [pending, setPending] = useState<Reservation[]>([])
  const [loading, setLoading] = useState(true)
  const [markingId, setMarkingId] = useState<number | null>(null)

  const load = () => {
    setLoading(true)
    api.getPendingPaymentReservations()
      .then(setPending)
      .catch(() => setPending([]))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleMarkPaid = async (id: number) => {
    setMarkingId(id)
    try {
      await api.markReservationPaid(id)
      load()
    } catch (e) {
      alert(e instanceof ApiError ? e.message : 'Error al registrar pago')
    } finally {
      setMarkingId(null)
    }
  }

  if (loading) return <p>Cargando...</p>

  return (
    <div className="grid grid-2">
      {pending.length === 0 ? (
        <div className="empty-state card">No hay pagos pendientes</div>
      ) : pending.map((r) => (
        <div key={r.id} className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem' }}>
            <h3>{r.activityName}</h3>
            <span className="badge badge-pending">Pago pendiente</span>
          </div>
          <p style={{ fontSize: '0.9rem', marginTop: '0.5rem' }}>
            Miembro: <strong>{r.memberName}</strong>
          </p>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Estado: {r.status} · Reservado: {new Date(r.createdAt).toLocaleString('es-MX')}
          </p>
          <button
            className="btn-primary"
            style={{ marginTop: '0.75rem' }}
            disabled={markingId === r.id}
            onClick={() => handleMarkPaid(r.id)}
          >
            {markingId === r.id ? 'Registrando...' : 'Marcar como pagado'}
          </button>
        </div>
      ))}
    </div>
  )
}
