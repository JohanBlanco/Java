import { useEffect, useMemo, useState } from 'react'
import { api } from '../../api'
import type { Sale } from '../../types'

export default function VentasRegistroSection() {
  const [sales, setSales] = useState<Sale[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.getSales()
      .then(setSales)
      .catch(() => setSales([]))
      .finally(() => setLoading(false))
  }, [])

  const todayTotal = useMemo(() => {
    const today = new Date().toDateString()
    return sales
      .filter((s) => new Date(s.paidAt).toDateString() === today)
      .reduce((sum, s) => sum + s.amount, 0)
  }, [sales])

  const monthTotal = useMemo(() => {
    const now = new Date()
    return sales
      .filter((s) => {
        const d = new Date(s.paidAt)
        return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear()
      })
      .reduce((sum, s) => sum + s.amount, 0)
  }, [sales])

  if (loading) return <p>Cargando...</p>

  return (
    <>
      <div className="grid grid-3" style={{ marginBottom: '2rem' }}>
        <div className="card stat-card">
          <div className="value">{sales.length}</div>
          <div className="label">Ventas totales</div>
        </div>
        <div className="card stat-card">
          <div className="value">${todayTotal.toFixed(0)}</div>
          <div className="label">Hoy</div>
        </div>
        <div className="card stat-card">
          <div className="value">${monthTotal.toFixed(0)}</div>
          <div className="label">Este mes</div>
        </div>
      </div>
      <div className="grid grid-2">
        {sales.length === 0 ? (
          <div className="empty-state card">Aún no hay ventas registradas</div>
        ) : sales.map((s) => (
          <div key={s.id} className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.5rem' }}>
              <h3>{s.concept}</h3>
              <span style={{ fontWeight: 700, color: 'var(--primary)' }}>${s.amount.toFixed(2)}</span>
            </div>
            <p style={{ fontSize: '0.9rem', marginTop: '0.5rem' }}>
              {s.activityName} · <strong>{s.memberName}</strong>
            </p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              {new Date(s.paidAt).toLocaleString('es-MX')}
            </p>
          </div>
        ))}
      </div>
    </>
  )
}
