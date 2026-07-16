import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api'
import AdminFormModal from '../../components/AdminFormModal'
import HorizontalSwitch from '../../components/HorizontalSwitch'
import type { MembershipPackage, User } from '../../types'
import MultiSelect from '../../components/MultiSelect'
import { useFilteredList } from '../../hooks/useFilteredList'
import { useDateFormat } from '../../preferences/useDateFormat'
import { useToast } from '../../toast'
import { formatNationalIdInput, isValidNationalId } from '../../utils/nationalId'
import { isValidEmail } from '../../utils/emailValidation'
import {
  COSTA_RICA_WHATSAPP_CODE,
  formatWhatsappLocalInput,
  isValidWhatsappLocal,
  whatsappPhoneToLocalDisplay,
} from '../../utils/whatsappPhone'
import { DEFAULT_PASSWORD } from './constants'
import { formatRoles, GYM_ROLES, MEMBERSHIP_STATUS_LABELS, membershipStatusBadgeClass, ROLE_LABELS, type GymRole } from '../../roles'

const emptyForm = () => ({
  firstName: '',
  lastName: '',
  email: '',
  password: DEFAULT_PASSWORD,
  nationalId: '',
  whatsappPhone: '',
  roles: ['MEMBER'] as GymRole[],
  membershipPackageId: '' as string | number,
  sendRegistrationForm: true,
})

export default function UsersSection() {
  const { formatDate } = useDateFormat()
  const { showApiError, showSuccess } = useToast()
  const [users, setUsers] = useState<User[]>([])
  const [packages, setPackages] = useState<MembershipPackage[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [form, setForm] = useState(emptyForm())
  const [saving, setSaving] = useState(false)
  const [resendingForm, setResendingForm] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)

  const isEditing = selectedId !== null

  const load = () => {
    api.getUsers().then(setUsers).catch(() => {})
    api.getPackages().then(setPackages).catch(() => {})
  }

  useEffect(() => { load() }, [])

  const userSearchExtras = useCallback(
    (u: User) => [
      ...u.roles.map((role) => ROLE_LABELS[role as GymRole] ?? role),
      ...(u.membershipStatus ? [MEMBERSHIP_STATUS_LABELS[u.membershipStatus] ?? u.membershipStatus] : []),
      ...(u.membershipPackageName ? [u.membershipPackageName] : []),
      ...(u.nationalId ? [u.nationalId] : []),
      ...(u.profile?.nationalId ? [u.profile.nationalId] : []),
      ...(u.whatsappPhone ? [u.whatsappPhone] : []),
    ],
    [],
  )
  const { filtered, filterInput } = useFilteredList(users, userSearchExtras)

  const closeModal = () => {
    setModalOpen(false)
    setSelectedId(null)
    setForm(emptyForm())
  }

  const openCreate = () => {
    setSelectedId(null)
    setForm(emptyForm())
    setModalOpen(true)
  }

  const openEdit = (user: User) => {
    const matchedPackage = packages.find((p) => p.name === user.membershipPackageName)
    setSelectedId(user.id)
    setForm({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      password: '',
      nationalId: user.nationalId ?? user.profile?.nationalId ?? '',
      whatsappPhone: whatsappPhoneToLocalDisplay(user.whatsappPhone),
      roles: user.roles.filter((r): r is GymRole => GYM_ROLES.includes(r as GymRole)),
      membershipPackageId: matchedPackage?.id ?? '',
      sendRegistrationForm: true,
    })
    setModalOpen(true)
  }

  const requiresNationalId = true
  const requiresMembership = form.roles.includes('MEMBER')
  const emailValid = isValidEmail(form.email)
  const nationalIdValid = isValidNationalId(form.nationalId)
  const membershipValid = !requiresMembership || form.membershipPackageId !== ''
  const namesValid = form.firstName.trim().length > 0 && form.lastName.trim().length > 0
  const passwordValid = isEditing || form.password.trim().length > 0
  const rolesValid = form.roles.length > 0
  const whatsappValid = isEditing
    ? form.whatsappPhone.length === 0 || isValidWhatsappLocal(form.whatsappPhone)
    : isValidWhatsappLocal(form.whatsappPhone)
  const formValid = namesValid && emailValid && nationalIdValid && membershipValid
    && passwordValid && rolesValid && whatsappValid

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formValid) return

    setSaving(true)
    try {
      const payload: Record<string, unknown> = {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        roles: form.roles,
      }
      if (!isEditing || form.password.trim()) {
        payload.password = form.password.trim() || DEFAULT_PASSWORD
      }
      payload.nationalId = formatNationalIdInput(form.nationalId)
      if (requiresMembership) {
        payload.membershipPackageId = Number(form.membershipPackageId)
      }
      if (form.whatsappPhone.trim()) {
        payload.whatsappPhone = form.whatsappPhone.trim()
      }
      if (!isEditing && form.roles.includes('MEMBER')) {
        payload.sendRegistrationForm = form.sendRegistrationForm
      }
      if (isEditing) {
        await api.updateUser(selectedId, payload)
      } else {
        const result = await api.createUser(payload)
        if (result.registrationFormWhatsappUrl) {
          window.open(result.registrationFormWhatsappUrl, '_blank', 'noopener,noreferrer')
          showSuccess('Usuario creado. Se abrió WhatsApp para enviar el formulario de registro.')
        } else if (form.roles.includes('MEMBER') && form.sendRegistrationForm) {
          if (!form.whatsappPhone.trim()) {
            showSuccess('Usuario creado. Indica un número de WhatsApp para enviar el formulario.')
          } else {
            showSuccess('Usuario creado. Activa WhatsApp en Configuración para enviar el formulario.')
          }
        } else {
          showSuccess('Usuario creado')
        }
      }
      closeModal()
      load()
    } catch (err) {
      showApiError(err, 'No se pudo guardar el usuario')
    } finally {
      setSaving(false)
    }
  }

  const handleResendRegistrationForm = async () => {
    if (selectedId == null) return
    setResendingForm(true)
    try {
      const result = await api.resendRegistrationForm(selectedId)
      window.open(result.whatsappUrl, '_blank', 'noopener,noreferrer')
      showSuccess('Se abrió WhatsApp con el formulario de registro.')
    } catch (err) {
      showApiError(err, 'No se pudo preparar el envío por WhatsApp')
    } finally {
      setResendingForm(false)
    }
  }

  return (
    <div className="admin-section">
      <div className="admin-list-toolbar">
        <div className="list-filter">{filterInput}</div>
        <button type="button" className="btn-primary admin-list-create-btn" onClick={openCreate}>
          Crear Usuario
        </button>
      </div>

      {users.length === 0 ? (
        <div className="empty-state card">No hay usuarios registrados.</div>
      ) : (
        <div className="grid grid-2">
          {filtered.length === 0 ? (
            <div className="empty-state card">Ningún resultado coincide con la búsqueda</div>
          ) : filtered.map((u) => (
            <div
              key={u.id}
              className="card card-selectable"
              onClick={() => openEdit(u)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => e.key === 'Enter' && openEdit(u)}
            >
              <h3>{u.firstName} {u.lastName}</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>{u.email}</p>
              {u.whatsappPhone && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
                  WhatsApp: {u.whatsappPhone}
                </p>
              )}
              {u.nationalId && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.35rem', marginBottom: 0 }}>
                  Cédula: {u.nationalId}
                </p>
              )}
              {!u.nationalId && u.profile?.nationalId && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
                  Cédula: {u.profile.nationalId}
                </p>
              )}
              <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                {u.roles.map((role) => (
                  <span key={role} className="badge badge-active">
                    {ROLE_LABELS[role as GymRole] ?? role}
                  </span>
                ))}
                {u.roles.includes('MEMBER') && u.membershipStatus && (
                  <span className={`badge ${membershipStatusBadgeClass(u.membershipStatus)}`}>
                    {MEMBERSHIP_STATUS_LABELS[u.membershipStatus] ?? u.membershipStatus}
                  </span>
                )}
              </div>
              {u.roles.includes('MEMBER') && u.membershipStatus === 'PAYMENT_PENDING' && (
                <div style={{ marginTop: '0.5rem' }}>
                  {u.nextPaymentDate && (
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: '0 0 0.35rem' }}>
                      Venció el {formatDate(u.nextPaymentDate)}
                    </p>
                  )}
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
                    Plan: {u.membershipPackageName ?? 'Sin plan asignado'}
                  </p>
                </div>
              )}
              {u.roles.includes('MEMBER') && u.membershipStatus === 'ACTIVE' && u.nextPaymentDate && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.5rem', marginBottom: 0 }}>
                  Próximo pago: {formatDate(u.nextPaymentDate)}
                </p>
              )}
              {u.roles.includes('MEMBER') && u.membershipStatus === 'ACTIVE' && u.membershipPackageName && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.35rem', marginBottom: 0 }}>
                  Plan: {u.membershipPackageName}
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      <AdminFormModal
        title={isEditing ? 'Editar usuario' : 'Nuevo usuario'}
        open={modalOpen}
        onClose={closeModal}
        onSubmit={handleSubmit}
        saving={saving}
        submitLabel={isEditing ? 'Guardar cambios' : 'Crear usuario'}
        submitDisabled={!formValid}
        intro={!isEditing ? (
          <p className="admin-form-intro">
            Completa todos los campos. La contraseña por defecto es <strong>{DEFAULT_PASSWORD}</strong>.
            Asigna uno o más roles; cada perfil habilita funciones distintas en la app.
          </p>
        ) : form.roles.length > 0 ? (
          <p className="admin-form-intro">
            Perfiles: {formatRoles(form.roles)}
          </p>
        ) : undefined}
      >
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
        {requiresNationalId && (
          <div className="form-group">
            <label>Cédula de identidad</label>
            <input
              inputMode="numeric"
              autoComplete="off"
              value={form.nationalId}
              onChange={(e) => setForm((prev) => ({
                ...prev,
                nationalId: formatNationalIdInput(e.target.value),
              }))}
              placeholder="9 dígitos"
              maxLength={9}
              pattern="\d{9}"
              title="La cédula debe tener 9 dígitos numéricos"
              required
            />
            {form.nationalId.length > 0 && !isValidNationalId(form.nationalId) && (
              <p style={{ color: 'var(--danger)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
                La cédula debe tener exactamente 9 dígitos numéricos
              </p>
            )}
          </div>
        )}
        <div className="form-group">
          <label>Correo de acceso</label>
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm((prev) => ({ ...prev, email: e.target.value }))}
            autoComplete="email"
            required
          />
          {form.email.trim().length > 0 && !emailValid && (
            <p style={{ color: 'var(--danger)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
              Ingresa un correo válido (ejemplo: usuario@dominio.com)
            </p>
          )}
        </div>
        <div className="form-group">
          <label>WhatsApp</label>
          <div className="phone-input-group">
            <input
              type="text"
              className="phone-input-prefix"
              value={COSTA_RICA_WHATSAPP_CODE}
              disabled
              readOnly
              aria-label="Código de país Costa Rica"
            />
            <input
              type="tel"
              inputMode="numeric"
              autoComplete="tel-national"
              value={form.whatsappPhone}
              onChange={(e) => setForm((prev) => ({
                ...prev,
                whatsappPhone: formatWhatsappLocalInput(e.target.value),
              }))}
              placeholder="88887777"
              maxLength={8}
              required={!isEditing}
            />
          </div>
          <p className="form-hint">
            Número local de 8 dígitos. El código de Costa Rica ({COSTA_RICA_WHATSAPP_CODE}) se agrega automáticamente.
          </p>
          {form.whatsappPhone.length > 0 && !whatsappValid && (
            <p style={{ color: 'var(--danger)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
              El número debe tener exactamente 8 dígitos
            </p>
          )}
        </div>
        <div className="form-group">
          <label>Contraseña</label>
          <input
            type="text"
            value={form.password}
            onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
            placeholder={isEditing ? 'Dejar vacío para no cambiar' : DEFAULT_PASSWORD}
            required={!isEditing}
          />
          <p className="form-hint">
            {isEditing
              ? 'Dejar vacío para conservar la contraseña actual.'
              : `La contraseña por defecto es ${DEFAULT_PASSWORD}. Puedes cambiarla antes de crear el usuario.`}
          </p>
        </div>
        <div className="form-group">
          <label>Roles</label>
          <MultiSelect
            options={GYM_ROLES.map((role) => ({ value: role, label: ROLE_LABELS[role] }))}
            value={form.roles}
            onChange={(roles) => setForm((prev) => ({ ...prev, roles: roles as GymRole[] }))}
            placeholder="Escribe un rol y pulsa Enter…"
          />
          {form.roles.length === 0 && (
            <p style={{ color: 'var(--danger)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
              Selecciona al menos un rol
            </p>
          )}
        </div>
        {form.roles.includes('MEMBER') && (
          <div className="form-group">
            <label>Membresía</label>
            <select
              value={form.membershipPackageId}
              onChange={(e) => setForm((prev) => ({ ...prev, membershipPackageId: e.target.value }))}
              required
            >
              <option value="" disabled>Seleccionar plan…</option>
              {packages.map((p) => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
            {!membershipValid && (
              <p style={{ color: 'var(--danger)', fontSize: '0.85rem', marginTop: '0.35rem' }}>
                Selecciona una membresía
              </p>
            )}
          </div>
        )}
        {!isEditing && form.roles.includes('MEMBER') && (
          <div className="user-registration-form-section">
            <div className="form-group form-group--switch">
              <HorizontalSwitch
                label="Enviar formulario de registro por WhatsApp"
                offLabel="No"
                onLabel="Sí"
                checked={form.sendRegistrationForm}
                onChange={(checked) => setForm((prev) => ({
                  ...prev,
                  sendRegistrationForm: checked,
                }))}
              />
            </div>
            <p className="text-muted" style={{ fontSize: '0.85rem', marginTop: '0.35rem' }}>
              Se abrirá WhatsApp con el mensaje y el enlace al formulario de ingreso del gimnasio.
            </p>
          </div>
        )}
        {isEditing && form.roles.includes('MEMBER') && (
          <div className="user-registration-form-section">
            <button
              type="button"
              className="btn-secondary"
              disabled={resendingForm || !form.whatsappPhone.trim()}
              onClick={handleResendRegistrationForm}
            >
              {resendingForm ? 'Preparando…' : 'Volver a enviar formulario de registro'}
            </button>
            <p className="text-muted" style={{ fontSize: '0.85rem', marginTop: '0.35rem' }}>
              Se abrirá WhatsApp (Web o app de Windows) con el enlace al formulario de registro.
              {!form.whatsappPhone.trim() && ' Indica un número de WhatsApp para habilitar el envío.'}
            </p>
          </div>
        )}
      </AdminFormModal>
    </div>
  )
}
