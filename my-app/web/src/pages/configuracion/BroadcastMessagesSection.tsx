import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../../api'
import AdminFormModal from '../../components/AdminFormModal'
import HorizontalSwitch from '../../components/HorizontalSwitch'
import { useFilteredList } from '../../hooks/useFilteredList'
import { useToast } from '../../toast'
import type { BroadcastChannelSettings, BroadcastMessageTemplate, BroadcastTemplatePurpose, CustomForm } from '../../types'
import { formLinkVariable, resolveFormLinks } from './forms/formBuilderConstants'

type BroadcastChannelTab = 'WHATSAPP'

type Props = {
  /** Si se define, muestra solo ese canal (p. ej. desde Configuración del perfil). */
  fixedChannel?: BroadcastChannelTab
}

const CHANNEL_TABS: { id: BroadcastChannelTab; label: string }[] = [
  { id: 'WHATSAPP', label: 'WhatsApp' },
]

const PURPOSE_LABELS: Record<BroadcastTemplatePurpose, string> = {
  GENERAL: 'General',
  WELCOME: 'Bienvenida',
}

const emptyTemplateForm = () => ({ name: '', body: '', purpose: 'GENERAL' as BroadcastTemplatePurpose })

export default function BroadcastMessagesSection({ fixedChannel }: Props) {
  const { showSuccess, showApiError } = useToast()
  const [channel, setChannel] = useState<BroadcastChannelTab>(fixedChannel ?? 'WHATSAPP')
  const [settings, setSettings] = useState<BroadcastChannelSettings | null>(null)
  const [senderPhone, setSenderPhone] = useState('')
  const [enabled, setEnabled] = useState(false)
  const [settingsLoading, setSettingsLoading] = useState(true)
  const [settingsSaving, setSettingsSaving] = useState(false)
  const [settingsDirty, setSettingsDirty] = useState(false)
  const [autoSaveStatus, setAutoSaveStatus] = useState<'idle' | 'saving' | 'saved'>('idle')
  const skipAutoSaveRef = useRef(true)
  const [templates, setTemplates] = useState<BroadcastMessageTemplate[]>([])
  const [templatesLoading, setTemplatesLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [form, setForm] = useState(emptyTemplateForm())
  const [templateSaving, setTemplateSaving] = useState(false)
  const [referenceForms, setReferenceForms] = useState<CustomForm[]>([])
  const [waWebModalOpen, setWaWebModalOpen] = useState(false)
  const [waWebConfirmed, setWaWebConfirmed] = useState(false)

  const buildSettingsPayload = useCallback((options?: {
    enabledOverride?: boolean
    whatsappWebSessionConfirmed?: boolean
  }) => {
    const nextEnabled = options?.enabledOverride ?? enabled
    const sessionConfirmed = options?.whatsappWebSessionConfirmed
      ?? (nextEnabled ? (settings?.whatsappWebSessionConfirmed ?? false) : false)

    return {
      senderPhone: senderPhone.trim() || null,
      enabled: nextEnabled,
      whatsappWebSessionConfirmed: sessionConfirmed,
    }
  }, [enabled, senderPhone, settings?.whatsappWebSessionConfirmed])

  const loadSettings = useCallback(async () => {
    setSettingsLoading(true)
    try {
      const data = await api.getBroadcastChannelSettings(channel)
      setSettings(data)
      setSenderPhone(data.senderPhone ?? '')
      setEnabled(data.enabled)
      setSettingsDirty(false)
      setAutoSaveStatus('idle')
      skipAutoSaveRef.current = true
    } catch (err) {
      showApiError(err, 'No se pudo cargar la configuración')
    } finally {
      setSettingsLoading(false)
    }
  }, [channel, showApiError])

  const loadTemplates = useCallback(async () => {
    setTemplatesLoading(true)
    try {
      const data = await api.getBroadcastTemplates(channel)
      setTemplates(data)
    } catch (err) {
      showApiError(err, 'No se pudieron cargar las plantillas')
      setTemplates([])
    } finally {
      setTemplatesLoading(false)
    }
  }, [channel, showApiError])

  useEffect(() => {
    loadSettings()
    loadTemplates()
    api.getForms().then(setReferenceForms).catch(() => setReferenceForms([]))
  }, [loadSettings, loadTemplates])

  const persistSettings = useCallback(async (options?: {
    showToast?: boolean
    enabledOverride?: boolean
    whatsappWebSessionConfirmed?: boolean
  }) => {
    const showToast = options?.showToast ?? false
    const payload = buildSettingsPayload(options)
    setSettingsSaving(true)
    setAutoSaveStatus('saving')
    try {
      const data = await api.updateBroadcastChannelSettings(channel, payload)
      setSettings(data)
      setSenderPhone(data.senderPhone ?? '')
      setEnabled(data.enabled)
      setSettingsDirty(false)
      setAutoSaveStatus('saved')
      if (showToast) {
        showSuccess('Configuración guardada')
      }
    } catch (err) {
      setAutoSaveStatus('idle')
      showApiError(err, 'No se pudo guardar la configuración')
      throw err
    } finally {
      setSettingsSaving(false)
    }
  }, [buildSettingsPayload, channel, showApiError, showSuccess])

  useEffect(() => {
    if (settingsLoading || !settings) return
    if (skipAutoSaveRef.current) {
      skipAutoSaveRef.current = false
      return
    }
    if (!settingsDirty) return

    const timer = window.setTimeout(() => {
      void persistSettings()
    }, 700)

    return () => window.clearTimeout(timer)
  }, [settingsDirty, settingsLoading, settings, persistSettings, senderPhone, enabled])

  const { filtered, filterInput } = useFilteredList(templates)

  const handleSaveSettings = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await persistSettings({ showToast: true })
    } catch {
      // error toast already shown
    }
  }

  const handleEnabledChange = (value: boolean) => {
    if (value) {
      if (!senderPhone.trim()) {
        showApiError(new Error('Indica el número de WhatsApp antes de activar los envíos automáticos'), 'Falta el número de WhatsApp')
        return
      }
      setWaWebConfirmed(false)
      setWaWebModalOpen(true)
      return
    }
    setEnabled(false)
    markSettingsDirty()
  }

  const closeWaWebModal = () => {
    setWaWebModalOpen(false)
    setWaWebConfirmed(false)
  }

  const handleWaWebConfirm = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!waWebConfirmed) return
    try {
      setEnabled(true)
      await persistSettings({
        showToast: true,
        enabledOverride: true,
        whatsappWebSessionConfirmed: true,
      })
      closeWaWebModal()
    } catch {
      setEnabled(false)
    }
  }

  const markSettingsDirty = () => {
    setSettingsDirty(true)
    setAutoSaveStatus('idle')
  }

  const openCreateTemplate = () => {
    setSelectedId(null)
    setForm(emptyTemplateForm())
    setModalOpen(true)
  }

  const openEditTemplate = (template: BroadcastMessageTemplate) => {
    setSelectedId(template.id)
    setForm({ name: template.name, body: template.body, purpose: template.purpose ?? 'GENERAL' })
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setSelectedId(null)
    setForm(emptyTemplateForm())
  }

  const handleTemplateSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setTemplateSaving(true)
    try {
      if (selectedId != null) {
        await api.updateBroadcastTemplate(channel, selectedId, form)
        showSuccess('Plantilla actualizada')
      } else {
        await api.createBroadcastTemplate(channel, form)
        showSuccess('Plantilla creada')
      }
      closeModal()
      loadTemplates()
    } catch (err) {
      showApiError(err, 'No se pudo guardar la plantilla')
    } finally {
      setTemplateSaving(false)
    }
  }

  const insertFormLink = (slug: string) => {
    const token = formLinkVariable(slug)
    setForm((prev) => ({
      ...prev,
      body: prev.body.trim() ? `${prev.body.trim()}\n${token}` : token,
    }))
  }

  const handleDeleteTemplate = async (id: number) => {
    if (!window.confirm('¿Eliminar esta plantilla?')) return
    try {
      await api.deleteBroadcastTemplate(channel, id)
      showSuccess('Plantilla eliminada')
      loadTemplates()
    } catch (err) {
      showApiError(err, 'No se pudo eliminar la plantilla')
    }
  }

  return (
    <div className="admin-section broadcast-config-section">
      {!fixedChannel && (
        <div className="broadcast-channel-tabs">
          {CHANNEL_TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={channel === tab.id ? 'btn-primary' : 'btn-secondary'}
              onClick={() => setChannel(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      )}

      {channel === 'WHATSAPP' && (
        <div className="broadcast-channel-panel">
          <section className="card broadcast-settings-card">
            <div className="broadcast-settings-card-head">
              <div>
                <h2>WhatsApp</h2>
                <p className="text-muted">
                  Indica el mismo número con el que iniciaste sesión en WhatsApp Web.
                  El panel no puede leerlo automáticamente por seguridad del navegador.
                </p>
              </div>
              <div className="broadcast-settings-card-badges">
                {settings?.enabled && (
                  <span className="badge badge-confirmed">Activo</span>
                )}
                {settings?.whatsappWebSessionConfirmed && (
                  <span className="badge badge-active">WhatsApp Web listo</span>
                )}
              </div>
            </div>

            {settingsLoading ? (
              <p className="text-muted">Cargando configuración…</p>
            ) : (
              <form className="broadcast-settings-form" onSubmit={handleSaveSettings}>
                <div className="form-group">
                  <label htmlFor="broadcast-whatsapp-phone">Número de WhatsApp</label>
                  <input
                    id="broadcast-whatsapp-phone"
                    type="tel"
                    placeholder="+50688887777"
                    value={senderPhone}
                    onChange={(e) => {
                      setSenderPhone(e.target.value)
                      markSettingsDirty()
                    }}
                    autoComplete="tel"
                  />
                  <p className="form-hint">
                    Mismo número con el que inicias sesión en WhatsApp Web o la app de Windows.
                    En WhatsApp Web: menú ⋮ → tu perfil.
                  </p>
                </div>

                <div className="form-group form-group--switch">
                  <HorizontalSwitch
                    label="Envíos por WhatsApp"
                    offLabel="Desactivado"
                    onLabel="Activado"
                    checked={enabled}
                    onChange={handleEnabledChange}
                  />
                </div>

                {enabled && (
                  <div className="broadcast-wa-flow card">
                    <h3>Flujo de envío (wa.me)</h3>
                    <ol className="broadcast-wa-web-steps">
                      <li>WhatsApp Web o la app de Windows debe estar con sesión iniciada en este equipo.</li>
                      <li>Al crear o reenviar un formulario se abre WhatsApp con el mensaje y el enlace listos.</li>
                      <li>Revisas el mensaje y pulsas <strong>Enviar</strong> en WhatsApp.</li>
                    </ol>
                    <p className="text-muted" style={{ fontSize: '0.85rem', margin: 0 }}>
                      Método oficial soportado por Meta. El envío automático sin abrir la app quedará para la API de Meta.
                    </p>
                  </div>
                )}

                <div className="broadcast-settings-actions">
                  {autoSaveStatus === 'saving' && (
                    <span className="text-muted" style={{ marginRight: '0.75rem' }}>Guardando…</span>
                  )}
                  {autoSaveStatus === 'saved' && !settingsDirty && (
                    <span className="text-muted" style={{ marginRight: '0.75rem' }}>Guardado</span>
                  )}
                  <button type="submit" className="btn-primary" disabled={settingsSaving}>
                    {settingsSaving ? 'Guardando…' : 'Guardar ahora'}
                  </button>
                </div>
              </form>
            )}
          </section>

          <section className="broadcast-templates-section">
            <div className="admin-list-toolbar">
              <div>
                <h2>Plantillas de mensaje</h2>
                <p className="text-muted">
                  Crea textos reutilizables para avisos, recordatorios, confirmaciones y mensajes de bienvenida.
                </p>
              </div>
              <button type="button" className="btn-primary" onClick={openCreateTemplate}>
                Nueva plantilla
              </button>
            </div>

            {filterInput}

            {templatesLoading ? (
              <p className="text-muted">Cargando plantillas…</p>
            ) : templates.length === 0 ? (
              <div className="empty-state card">
                Aún no hay plantillas. Crea la primera para usarla en otras secciones del sistema.
              </div>
            ) : filtered.length === 0 ? (
              <div className="empty-state card">Ningún resultado coincide con la búsqueda</div>
            ) : (
              <div className="grid grid-2 broadcast-template-grid">
                {filtered.map((template) => (
                  <div key={template.id} className="card card-selectable broadcast-template-card">
                    <div className="broadcast-template-card-head">
                      <h3>{template.name}</h3>
                      <div className="broadcast-template-badges">
                        <span className="badge badge-trial">WhatsApp</span>
                        <span className={`badge ${template.purpose === 'WELCOME' ? 'badge-recurring' : 'badge-active'}`}>
                          {PURPOSE_LABELS[template.purpose ?? 'GENERAL']}
                        </span>
                      </div>
                    </div>
                    <p className="broadcast-template-preview">{template.body}</p>
                    <div className="broadcast-template-actions">
                      <button type="button" className="btn-secondary" onClick={() => openEditTemplate(template)}>
                        Editar
                      </button>
                      <button type="button" className="btn-secondary btn-danger-outline" onClick={() => handleDeleteTemplate(template.id)}>
                        Eliminar
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>
      )}

      <AdminFormModal
        title={selectedId != null ? 'Editar plantilla' : 'Nueva plantilla'}
        open={modalOpen}
        onClose={closeModal}
        onSubmit={handleTemplateSubmit}
        saving={templateSaving}
        submitLabel={selectedId != null ? 'Guardar cambios' : 'Crear plantilla'}
        intro={(
          <p className="text-muted modal-subtitle">
            Puedes usar variables como {'{{nombre}}'}, {'{{actividad}}'} o {'{{fecha}}'}.
            Para enlazar un formulario usa {'{{form:slug}}'} o el selector de abajo.
          </p>
        )}
      >
        <div className="form-group">
          <label htmlFor="template-purpose">Tipo de plantilla</label>
          <select
            id="template-purpose"
            value={form.purpose}
            onChange={(e) => setForm((prev) => ({
              ...prev,
              purpose: e.target.value as BroadcastTemplatePurpose,
            }))}
          >
            <option value="GENERAL">General — avisos y recordatorios</option>
            <option value="WELCOME">Bienvenida — al crear usuarios</option>
          </select>
        </div>
        <div className="form-group">
          <label htmlFor="template-name">Nombre de la plantilla</label>
          <input
            id="template-name"
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="Recordatorio de clase"
            required
            maxLength={120}
          />
        </div>
        <div className="form-group">
          <label htmlFor="template-body">Mensaje</label>
          {referenceForms.length > 0 && (
            <div className="broadcast-form-link-picker">
              <label htmlFor="template-form-link" className="broadcast-form-link-picker-label">
                Insertar enlace de formulario
              </label>
              <div className="broadcast-form-link-picker-row">
                <select
                  id="template-form-link"
                  defaultValue=""
                  onChange={(e) => {
                    if (e.target.value) {
                      insertFormLink(e.target.value)
                      e.target.value = ''
                    }
                  }}
                >
                  <option value="" disabled>Seleccionar formulario…</option>
                  {referenceForms.map((item) => (
                    <option key={item.id} value={item.slug}>{item.title}</option>
                  ))}
                </select>
              </div>
            </div>
          )}
          <textarea
            id="template-body"
            value={form.body}
            onChange={(e) => setForm((prev) => ({ ...prev, body: e.target.value }))}
            placeholder="Hola {{nombre}}, completa tu registro aquí: {{form:encuesta-inicial}}"
            rows={6}
            required
            maxLength={4096}
          />
          {form.body.includes('{{form:') && referenceForms.length > 0 && (
            <p className="form-hint">
              Vista previa de enlaces: {resolveFormLinks(form.body, referenceForms)}
            </p>
          )}
        </div>
      </AdminFormModal>

      <AdminFormModal
        title="Activar envíos por WhatsApp"
        open={waWebModalOpen}
        onClose={closeWaWebModal}
        onSubmit={handleWaWebConfirm}
        saving={settingsSaving}
        submitLabel="Activar envíos por WhatsApp"
        submitDisabled={!waWebConfirmed}
        intro={(
          <p className="text-muted modal-subtitle">
            Activa el envío por <strong>wa.me</strong>: el sistema abrirá WhatsApp con el mensaje listo.
            Necesitas sesión activa en WhatsApp Web o la app de Windows en esta computadora,
            con el mismo número que configuraste ({senderPhone.trim() || 'sin configurar'}).
          </p>
        )}
      >
        <ol className="broadcast-wa-web-steps">
          <li>
            Abre{' '}
            <a href="https://web.whatsapp.com" target="_blank" rel="noopener noreferrer">
              web.whatsapp.com
            </a>{' '}
            en otra pestaña.
          </li>
          <li>Inicia sesión escaneando el código QR con tu teléfono, si aún no lo has hecho.</li>
          <li>
            Verifica que el número del campo de arriba ({senderPhone.trim() || 'sin configurar'}) sea el mismo
            que ves en WhatsApp Web → menú ⋮ → perfil.
          </li>
          <li>Deja WhatsApp Web abierto mientras usas el panel del gimnasio.</li>
          <li>Confirma abajo cuando la sesión esté lista y el número coincida.</li>
        </ol>
        <div className="form-group form-group--switch">
          <HorizontalSwitch
            label="Confirmo que WhatsApp Web está abierto y con sesión iniciada en este equipo"
            offLabel="No"
            onLabel="Sí"
            checked={waWebConfirmed}
            onChange={setWaWebConfirmed}
          />
        </div>
      </AdminFormModal>
    </div>
  )
}
