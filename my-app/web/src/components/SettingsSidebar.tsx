import { useEffect, useState } from 'react'
import type { SettingsSection } from '../preferences'
import { usePreferences } from '../preferences'

type Props = {
  activeSection: SettingsSection
  onSectionChange: (section: SettingsSection) => void
  onBack: () => void
  showBroadcastSettings?: boolean
  showFormsSettings?: boolean
  showForumsSettings?: boolean
  showCashSettings?: boolean
}

function isAppearanceSection(section: SettingsSection) {
  return section === 'theme' || section === 'color'
}

function isBroadcastSection(section: SettingsSection) {
  return section === 'broadcast-whatsapp'
}

export default function SettingsSidebar({
  activeSection,
  onSectionChange,
  onBack,
  showBroadcastSettings = false,
  showFormsSettings = false,
  showForumsSettings = false,
  showCashSettings = false,
}: Props) {
  const { t } = usePreferences()
  const [appearanceOpen, setAppearanceOpen] = useState(() => isAppearanceSection(activeSection))
  const [broadcastOpen, setBroadcastOpen] = useState(() => isBroadcastSection(activeSection))

  useEffect(() => {
    if (isAppearanceSection(activeSection)) {
      setAppearanceOpen(true)
    }
  }, [activeSection])

  useEffect(() => {
    if (isBroadcastSection(activeSection)) {
      setBroadcastOpen(true)
    }
  }, [activeSection])

  return (
    <>
      <button type="button" className="settings-nav-link settings-nav-back" onClick={onBack}>
        ← {t('settings.back')}
      </button>

      <div className="sidebar-group">
        <button
          type="button"
          className={`sidebar-group-header${isAppearanceSection(activeSection) ? ' active' : ''}`}
          onClick={() => setAppearanceOpen((open) => !open)}
          aria-expanded={appearanceOpen}
        >
          <span>{t('settings.appearance')}</span>
          <span className="sidebar-group-chevron" aria-hidden>{appearanceOpen ? '▾' : '▸'}</span>
        </button>
        {appearanceOpen && (
          <div className="sidebar-subnav">
            <button
              type="button"
              className={`sidebar-sub-link${activeSection === 'theme' ? ' active' : ''}`}
              aria-current={activeSection === 'theme' ? 'page' : undefined}
              onClick={() => onSectionChange('theme')}
            >
              {t('settings.theme')}
            </button>
            <button
              type="button"
              className={`sidebar-sub-link${activeSection === 'color' ? ' active' : ''}`}
              aria-current={activeSection === 'color' ? 'page' : undefined}
              onClick={() => onSectionChange('color')}
            >
              {t('settings.color')}
            </button>
          </div>
        )}
      </div>

      <button
        type="button"
        className={`settings-nav-link${activeSection === 'language' ? ' active' : ''}`}
        aria-current={activeSection === 'language' ? 'page' : undefined}
        onClick={() => onSectionChange('language')}
      >
        {t('settings.language')}
      </button>

      {showFormsSettings && (
        <button
          type="button"
          className={`settings-nav-link${activeSection === 'forms' ? ' active' : ''}`}
          aria-current={activeSection === 'forms' ? 'page' : undefined}
          onClick={() => onSectionChange('forms')}
        >
          {t('settings.forms')}
        </button>
      )}

      {showForumsSettings && (
        <button
          type="button"
          className={`settings-nav-link${activeSection === 'forums' ? ' active' : ''}`}
          aria-current={activeSection === 'forums' ? 'page' : undefined}
          onClick={() => onSectionChange('forums')}
        >
          {t('settings.forums')}
        </button>
      )}

      {showCashSettings && (
        <button
          type="button"
          className={`settings-nav-link${activeSection === 'cash' ? ' active' : ''}`}
          aria-current={activeSection === 'cash' ? 'page' : undefined}
          onClick={() => onSectionChange('cash')}
        >
          Caja
        </button>
      )}

      {showBroadcastSettings && (
        <div className="sidebar-group">
          <button
            type="button"
            className={`sidebar-group-header${isBroadcastSection(activeSection) ? ' active' : ''}`}
            onClick={() => setBroadcastOpen((open) => !open)}
            aria-expanded={broadcastOpen}
          >
            <span>{t('settings.broadcastMessages')}</span>
            <span className="sidebar-group-chevron" aria-hidden>{broadcastOpen ? '▾' : '▸'}</span>
          </button>
          {broadcastOpen && (
            <div className="sidebar-subnav">
              <button
                type="button"
                className={`sidebar-sub-link${activeSection === 'broadcast-whatsapp' ? ' active' : ''}`}
                aria-current={activeSection === 'broadcast-whatsapp' ? 'page' : undefined}
                onClick={() => onSectionChange('broadcast-whatsapp')}
              >
                {t('settings.broadcastWhatsapp')}
              </button>
            </div>
          )}
        </div>
      )}
    </>
  )
}
