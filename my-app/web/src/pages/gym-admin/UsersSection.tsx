import { useEffect, useState } from 'react'
import { api } from '../../api'
import type { MembershipPackage, User } from '../../types'
import MultiSelect from '../../components/MultiSelect'
import { DEFAULT_PASSWORD } from './constants'
import { formatRoles, GYM_ROLES, ROLE_LABELS, type GymRole } from '../../roles'

const emptyForm = () => ({
  firstName: '',
  lastName: '',
  email: '',
  password: DEFAULT_PASSWORD,
  roles: ['INSTRUCTOR'] as GymRole[],
  membershipPackageId: '' as string | number,
})

export default function UsersSection() {
  const [users, setUsers] = useState<User[]>([])
  const [packages, setPackages] = useState<MembershipPackage[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [form, setForm] = useState(emptyForm())
  const [saving, setSaving] = useState(false)

  const isEditing = selectedId !== null

  const load = () => {
    api.getUsers().then(setUsers).catch(() => {})
    api.getPackages().then(setPackages).catch(() => {})
  }

  useEffect(() => { load() }, [])

  const selectUser = (user: User) => {
    setSelectedId(user.id)
    setForm({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      password: '',
      roles: user.roles.filter((r): r is GymRole => GYM_ROLES.includes(r as GymRole)),
      membershipPackageId: '',
    })
  }

  const resetForm = () => {
    setSelectedId(null)
    setForm(emptyForm())
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (form.roles.length === 0) return
    setSaving(true)
    try {
      const payload: Record<string, unknown> = {
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        roles: form.roles,
      }
      if (!isEditing || form.password) {
        payload.password = form.password || DEFAULT_PASSWORD
      }
      if (form.roles.includes('MEMBER') && form.membershipPackageId) {
        payload.membershipPackageId = Number(form.membershipPackageId)
      }
      if (isEditing) {
        await api.updateUser(selectedId, payload)
      } else {
        await api.createUser(payload)
      }
      resetForm()
      load()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="platform-layout admin-section">
      <div className="platform-list">
        {users.length === 0 ? (
          <div className="empty-state card">No hay usuarios registrados.</div>
        ) : (
          <div className="grid grid-2">
            {users.map((u) => (
              <div
                key={u.id}
                className={`card card-selectable${selectedId === u.id ? ' card-selected' : ''}`}
                onClick={() => selectUser(u)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && selectUser(u)}
              >
                <h3>{u.firstName} {u.lastName}</h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>{u.email}</p>
                <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                  {u.roles.map((role) => (
                    <span key={role} className="badge badge-active">
                      {ROLE_LABELS[role as GymRole] ?? role}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="platform-form-panel">
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3>{isEditing ? 'Editar usuario' : 'Nuevo usuario'}</h3>
            {isEditing && (
              <button type="button" className="btn-secondary" onClick={resetForm}>
                Nuevo
              </button>
            )}
          </div>
          {!isEditing && (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1rem' }}>
              Asigna uno o más roles. Cada perfil habilita funciones distintas en la app.
            </p>
          )}
          {isEditing && form.roles.length > 0 && (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
              Perfiles: {formatRoles(form.roles)}
            </p>
          )}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Nombre</label>
              <input
                value={form.firstName}
                onChange={(e) => setForm((prev) => ({ ...prev, firstName: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>Apellido</label>
              <input
                value={form.lastName}
                onChange={(e) => setForm((prev) => ({ ...prev, lastName: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>Correo de acceso</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm((prev) => ({ ...prev, email: e.target.value }))}
                required
              />
            </div>
            <div className="form-group">
              <label>Contraseña</label>
              <input
                type="text"
                value={form.password}
                onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
                placeholder={isEditing ? 'Dejar vacío para no cambiar' : undefined}
                required={!isEditing}
              />
            </div>
            <div className="form-group">
              <label>Roles</label>
              <MultiSelect
                options={GYM_ROLES.map((role) => ({ value: role, label: ROLE_LABELS[role] }))}
                value={form.roles}
                onChange={(roles) => setForm((prev) => ({ ...prev, roles: roles as GymRole[] }))}
                placeholder="No hay roles disponibles"
                emptyLabel="Seleccionar roles..."
              />
              {form.roles.length === 0 && (
                <p style={{ color: 'var(--danger)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
                  Selecciona al menos un rol
                </p>
              )}
            </div>
            {form.roles.includes('MEMBER') && (
              <div className="form-group">
                <label>Membresía (miembros)</label>
                <select
                  value={form.membershipPackageId}
                  onChange={(e) => setForm((prev) => ({ ...prev, membershipPackageId: e.target.value }))}
                >
                  <option value="">Sin asignar</option>
                  {packages.map((p) => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>
              </div>
            )}
            <button
              type="submit"
              className="btn-primary"
              disabled={saving || form.roles.length === 0}
              style={{ width: '100%' }}
            >
              {saving ? 'Guardando...' : isEditing ? 'Guardar cambios' : 'Crear usuario'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
