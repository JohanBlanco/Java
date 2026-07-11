import { useEffect, useState } from 'react'
import { api } from '../../api'
import type { MembershipPackage } from '../../types'

const emptyForm = () => ({
  name: '',
  price: '',
  description: '',
  addonName: '',
  addonPrice: '',
  unlimitedFreeActivities: true,
  freeActivityQuota: '',
})

export default function PackagesSection() {
  const [packages, setPackages] = useState<MembershipPackage[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [form, setForm] = useState(emptyForm())
  const [saving, setSaving] = useState(false)

  const isEditing = selectedId !== null

  const load = () => {
    api.getPackages().then(setPackages).catch(() => {})
  }

  useEffect(() => { load() }, [])

  const selectPackage = (pkg: MembershipPackage) => {
    const firstAddon = pkg.addons[0]
    setSelectedId(pkg.id)
    setForm({
      name: pkg.name,
      price: String(pkg.price),
      description: pkg.description ?? '',
      addonName: firstAddon?.name ?? '',
      addonPrice: firstAddon ? String(firstAddon.price) : '',
      unlimitedFreeActivities: pkg.freeActivityQuota == null,
      freeActivityQuota: pkg.freeActivityQuota != null ? String(pkg.freeActivityQuota) : '',
    })
  }

  const resetForm = () => {
    setSelectedId(null)
    setForm(emptyForm())
  }

  const buildPayload = () => {
    const addons = form.addonName
      ? [{ name: form.addonName, description: '', price: parseFloat(form.addonPrice) || 0 }]
      : []
    return {
      name: form.name,
      description: form.description,
      price: parseFloat(form.price),
      durationMonths: 1,
      freeActivityQuota: form.unlimitedFreeActivities ? null : parseInt(form.freeActivityQuota, 10) || 0,
      addons,
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      const payload = buildPayload()
      if (isEditing) {
        await api.updatePackage(selectedId, payload)
      } else {
        await api.createPackage(payload)
      }
      resetForm()
      load()
    } finally {
      setSaving(false)
    }
  }

  const freeLabel = (p: MembershipPackage) =>
    p.freeActivityQuota == null ? 'Actividades gratis: ilimitadas' : `Actividades gratis: ${p.freeActivityQuota}/mes`

  return (
    <div className="platform-layout admin-section">
      <div className="platform-list">
        {packages.length === 0 ? (
          <div className="empty-state card">No hay membresías registradas.</div>
        ) : (
          <div className="grid grid-2">
            {packages.map((p) => (
              <div
                key={p.id}
                className={`card card-selectable${selectedId === p.id ? ' card-selected' : ''}`}
                onClick={() => selectPackage(p)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && selectPackage(p)}
              >
                <h3>{p.name}</h3>
                <p style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--primary)' }}>${p.price}/mes</p>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>{p.description}</p>
                <p style={{ fontSize: '0.85rem', marginTop: '0.5rem' }}>{freeLabel(p)}</p>
                {p.addons.length > 0 && (
                  <div style={{ marginTop: '0.75rem' }}>
                    <p style={{ fontSize: '0.85rem', fontWeight: 600 }}>Complementos:</p>
                    {p.addons.map((a) => (
                      <p key={a.id} style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                        + {a.name} (${a.price})
                      </p>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="platform-form-panel">
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3>{isEditing ? 'Editar membresía' : 'Nueva membresía'}</h3>
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
              <label>Precio mensual</label>
              <input
                type="number"
                step="0.01"
                value={form.price}
                onChange={(e) => setForm((prev) => ({ ...prev, price: e.target.value }))}
                required
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
                  checked={form.unlimitedFreeActivities}
                  onChange={(e) => setForm((prev) => ({ ...prev, unlimitedFreeActivities: e.target.checked }))}
                />
                Actividades gratuitas ilimitadas
              </label>
            </div>
            {!form.unlimitedFreeActivities && (
              <div className="form-group">
                <label>Actividades gratuitas incluidas (por mes)</label>
                <input
                  type="number"
                  min={0}
                  value={form.freeActivityQuota}
                  onChange={(e) => setForm((prev) => ({ ...prev, freeActivityQuota: e.target.value }))}
                  required
                />
              </div>
            )}
            <div className="form-group">
              <label>Complemento (opcional)</label>
              <input
                value={form.addonName}
                onChange={(e) => setForm((prev) => ({ ...prev, addonName: e.target.value }))}
                placeholder="Ej: Clases grupales"
              />
            </div>
            <div className="form-group">
              <label>Precio del complemento</label>
              <input
                type="number"
                step="0.01"
                value={form.addonPrice}
                onChange={(e) => setForm((prev) => ({ ...prev, addonPrice: e.target.value }))}
              />
            </div>
            <button type="submit" className="btn-primary" disabled={saving} style={{ width: '100%' }}>
              {saving ? 'Guardando...' : isEditing ? 'Guardar cambios' : 'Crear membresía'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
