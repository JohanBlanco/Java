import { useEffect, useState } from 'react'
import { api } from '../../api'
import { useFilteredList } from '../../hooks/useFilteredList'
import { useToast } from '../../toast'
import type { Reservation } from '../../types'

const STATUS_LABELS: Record<Reservation['status'], string> = {
  CONFIRMED: 'Confirmada',
  CANCELLED: 'Cancelada',
}

export default function MemberReservationsPage() {
  const { showSuccess, showApiError } = useToast()
  const [reservations, setReservations] = useState<Reservation[]>([])
  const [loading, setLoading] = useState(true)

  const load = () => {
    api.getMyReservations()
      .then(setReservations)
      .catch(() => setReservations([]))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const { filtered, filterInput } = useFilteredList(reservations)

  const handleCancel = async (id: number) => {
    try {
      await api.cancelReservation(id)
      showSuccess('Reservación cancelada')
      load()
    } catch (e) {
      showApiError(e, 'Error al cancelar')
    }
  }

  if (loading) return <p>Cargando...</p>

  return (
    <>
      {filterInput}
      <div className="grid grid-2">
        {reservations.length === 0 ? (
          <div className="empty-state card">Sin reservaciones</div>
        ) : filtered.length === 0 ? (
          <div className="empty-state card">Ningún resultado coincide con la búsqueda</div>
        ) : filtered.map((r) => (
          <div key={r.id} className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem' }}>
              <h3>{r.activityName}</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{r.occurrenceDate}</p>
              <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                <span className={`badge badge-${r.status === 'CONFIRMED' ? 'confirmed' : 'cancelled'}`}>
                  {STATUS_LABELS[r.status]}
                </span>
                {r.freeSlot && r.status === 'CONFIRMED' && (
                  <span className="badge badge-confirmed">Gratis</span>
                )}
                {r.paymentRequired && !r.paid && r.status === 'CONFIRMED' && (
                  <span className="badge badge-pending">Pago pendiente</span>
                )}
              </div>
            </div>
            {r.status === 'CONFIRMED' && r.paymentRequired && !r.paid && (
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.75rem' }}>
                Paga en recepción para conservar tu cupo.
              </p>
            )}
            {r.status === 'CONFIRMED' && (
              <button className="btn-danger" style={{ marginTop: '0.75rem' }} onClick={() => handleCancel(r.id)}>
                Cancelar
              </button>
            )}
          </div>
        ))}
      </div>
    </>
  )
}
