import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Organization } from '../types'
import { useFilteredList } from '../hooks/useFilteredList'

const DEFAULT_PASSWORD = '12345678'

const emptyForm = () => ({
  name: '',
  slug: '',
  phone: '',
  ownerFirstName: '',
  ownerLastName: '',
  ownerEmail: '',
  ownerNationalId: '',
  password: DEFAULT_PASSWORD,
  subscriptionStatus: 'ACTIVE' as string,
})

export default function PlatformPage() {
  const [orgs, setOrgs] = useState<Organization[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [form, setForm] = useState(emptyForm())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const isEditing = selectedId !== null

  const load = () => {
    api.getPlatformOrganizations().then(setOrgs).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const { filtered, filterInput } = useFilteredList(orgs)

  const populateForm = (org: Organization) => {
    setForm({
      name: org.name,
      slug: org.slug,
      phone: org.contactPhone ?? '',
      ownerFirstName: org.ownerFirstName ?? '',
      ownerLastName: org.ownerLastName ?? '',
      ownerEmail: org.ownerEmail ?? org.contactEmail,
      ownerNationalId: '',
      password: DEFAULT_PASSWORD,
      subscriptionStatus: org.subscriptionStatus,
    })
  }

  const selectOrg = async (org: Organization) => {
    setSelectedId(org.id)
    try {
      const fresh = await api.getPlatformOrganization(org.id)
      populateForm(fresh)
    } catch {
      populateForm(org)
    }
  }

  const resetForm = () => {
    setSelectedId(null)
    setForm(emptyForm())
  }

  const buildPayload = () => ({
    name: form.name,
    slug: form.slug,
    contactPhone: form.phone,
    contactEmail: form.ownerEmail,
    ownerFirstName: form.ownerFirstName,
    ownerLastName: form.ownerLastName,
    ownerEmail: form.ownerEmail,
    ownerNationalId: form.ownerNationalId,
    ownerPassword: form.password,
    subscriptionStatus: form.subscriptionStatus,
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      const payload = buildPayload()
      if (isEditing) {
        await api.updateOrganization(selectedId, payload)
      } else {
        await api.createOrganization(payload)
      }
      resetForm()
      load()
    } finally {
      setSaving(false)
    }
  }

  const toggleStatus = async (e: React.MouseEvent, org: Organization) => {
    e.stopPropagation()
    const newStatus = org.subscriptionStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    await api.updateOrganization(org.id, {
      name: org.name,
      slug: org.slug,
      contactPhone: org.contactPhone,
      contactEmail: org.contactEmail,
      ownerFirstName: org.ownerFirstName ?? 'Administrador',
      ownerLastName: org.ownerLastName ?? org.name,
      ownerEmail: org.ownerEmail ?? org.contactEmail,
      subscriptionStatus: newStatus,
    })
    if (selectedId === org.id) {
      setForm((prev) => ({ ...prev, subscriptionStatus: newStatus }))
    }
    load()
  }

  const setField = (field: keyof typeof form, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  if (loading) return <p>Cargando...</p>

  return (
    <div>
      <div className="page-header">
        <h1>Clientes (Gimnasios)</h1>
        <p>Cada gimnasio tiene su propio administrador y usuarios aislados</p>
      </div>

      <div className="platform-layout">
        <div className="platform-list">
          {orgs.length > 0 && filterInput}
          <div className="grid grid-2">
            {orgs.length === 0 ? (
              <div className="empty-state card">No hay clientes registrados.</div>
            ) : filtered.length === 0 ? (
              <div className="empty-state card">Ningún resultado coincide con la búsqueda</div>
            ) : filtered.map((org) => (
              <div
                key={org.id}
                className={`card card-selectable${selectedId === org.id ? ' card-selected' : ''}`}
                onClick={() => selectOrg(org)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && selectOrg(org)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                  <div>
                    <h3>{org.name}</h3>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>{org.slug}</p>
                  </div>
                  <span className={`badge badge-${org.subscriptionStatus === 'ACTIVE' ? 'active' : 'cancelled'}`}>
                    {org.subscriptionStatus}
                  </span>
                </div>
                <p style={{ marginTop: '0.75rem', fontSize: '0.9rem' }}>
                  Admin: {org.ownerEmail ?? org.contactEmail}
                </p>
                <div className="actions">
                  <button className="btn-secondary" onClick={(e) => toggleStatus(e, org)}>
                    {org.subscriptionStatus === 'ACTIVE' ? 'Suspender' : 'Activar'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="platform-form-panel">
          <div className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3>{isEditing ? 'Editar cliente' : 'Nuevo cliente'}</h3>
              {isEditing && (
                <button type="button" className="btn-secondary" onClick={resetForm}>
                  Nuevo
                </button>
              )}
            </div>
            <form onSubmit={handleSubmit}>
              <p style={{ fontWeight: 600, marginBottom: '0.75rem', fontSize: '0.9rem' }}>Datos del gimnasio</p>
              <div className="form-group">
                <label>Nombre del gimnasio</label>
                <input value={form.name} onChange={(e) => setField('name', e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Slug (identificador)</label>
                <input value={form.slug} onChange={(e) => setField('slug', e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Teléfono</label>
                <input value={form.phone} onChange={(e) => setField('phone', e.target.value)} />
              </div>

              <p style={{ fontWeight: 600, margin: '1.25rem 0 0.75rem', fontSize: '0.9rem' }}>
                Cuenta del administrador (login)
              </p>
              <div className="form-group">
                <label>Nombre</label>
                <input value={form.ownerFirstName} onChange={(e) => setField('ownerFirstName', e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Apellido</label>
                <input value={form.ownerLastName} onChange={(e) => setField('ownerLastName', e.target.value)} required />
              </div>
              <div className="form-group">
                <label>Correo de acceso</label>
                <input type="email" value={form.ownerEmail} onChange={(e) => setField('ownerEmail', e.target.value)} required />
              </div>
              {!isEditing && (
                <div className="form-group">
                  <label>Cédula del administrador</label>
                  <input
                    inputMode="numeric"
                    value={form.ownerNationalId}
                    onChange={(e) => setField('ownerNationalId', e.target.value.replace(/\D/g, '').slice(0, 9))}
                    placeholder="9 dígitos"
                    required
                    maxLength={9}
                  />
                </div>
              )}
              <div className="form-group">
                <label>Contraseña</label>
                <input
                  type="text"
                  value={form.password}
                  onChange={(e) => setField('password', e.target.value)}
                  required
                />
              </div>

              {isEditing && (
                <div className="form-group">
                  <label>Estado de suscripción</label>
                  <select
                    value={form.subscriptionStatus}
                    onChange={(e) => setField('subscriptionStatus', e.target.value)}
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="TRIAL">TRIAL</option>
                    <option value="SUSPENDED">SUSPENDED</option>
                    <option value="INACTIVE">INACTIVE</option>
                  </select>
                </div>
              )}

              {!isEditing && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
                  El administrador podrá iniciar sesión en la app con este correo y contraseña.
                </p>
              )}

              <button type="submit" className="btn-primary" disabled={saving} style={{ width: '100%' }}>
                {saving ? 'Guardando...' : isEditing ? 'Guardar cambios' : 'Crear cliente'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
