import type { CSSProperties } from 'react'
import BroadcastMessagesSection from '../pages/configuracion/BroadcastMessagesSection'
import CashSettingsSection from '../pages/configuracion/CashSettingsSection'
import FormsHub from '../pages/configuracion/FormsHub'
import ForumsSection from '../pages/configuracion/ForumsSection'
import {
  ACCENT_OPTIONS,
  LANGUAGE_OPTIONS,
  usePreferences,
  type SettingsSection,
} from '../preferences'

type Props = {
  section: SettingsSection
}

const USER_SECTION_META: Record<'theme' | 'color' | 'language', 'settings.theme' | 'settings.color' | 'settings.language'> = {
  theme: 'settings.theme',
  color: 'settings.color',
  language: 'settings.language',
}

const USER_DESCRIPTION_META: Record<'theme' | 'color' | 'language', 'settings.themeDescription' | 'settings.colorDescription' | 'settings.languageDescription'> = {
  theme: 'settings.themeDescription',
  color: 'settings.colorDescription',
  language: 'settings.languageDescription',
}

export default function SettingsPanel({ section }: Props) {
  const { theme, accent, language, setTheme, setAccent, setLanguage, t } = usePreferences()

  if (section === 'broadcast-whatsapp') {
    return (
      <div className="settings-panel">
        <div className="page-header">
          <h1>{t('settings.broadcastWhatsapp')}</h1>
          <p>{t('settings.broadcastWhatsappDescription')}</p>
        </div>
        <BroadcastMessagesSection fixedChannel="WHATSAPP" />
      </div>
    )
  }

  if (section === 'forms') {
    return (
      <div className="settings-panel">
        <div className="page-header">
          <h1>{t('settings.forms')}</h1>
          <p>{t('settings.formsDescription')}</p>
        </div>
        <FormsHub />
      </div>
    )
  }

  if (section === 'forums') {
    return (
      <div className="settings-panel">
        <div className="page-header">
          <h1>{t('settings.forums')}</h1>
          <p>{t('settings.forumsDescription')}</p>
        </div>
        <ForumsSection />
      </div>
    )
  }

  if (section === 'cash') {
    return (
      <div className="settings-panel">
        <div className="page-header">
          <h1>Caja</h1>
          <p>Fondo de apertura, monedas y billetes para el conteo diario.</p>
        </div>
        <CashSettingsSection />
      </div>
    )
  }

  const title = t(USER_SECTION_META[section])
  const description = t(USER_DESCRIPTION_META[section])

  return (
    <div className="settings-panel">
      <div className="page-header">
        <h1>{title}</h1>
        <p>{description}</p>
      </div>

      {section === 'theme' && (
        <div className="settings-panel-options" role="radiogroup" aria-label={title}>
          <button
            type="button"
            role="radio"
            aria-checked={theme === 'dark'}
            className={`settings-panel-option${theme === 'dark' ? ' active' : ''}`}
            onClick={() => setTheme('dark')}
          >
            <span className="settings-panel-option-preview settings-panel-option-preview--dark" aria-hidden="true" />
            <span className="settings-panel-option-label">{t('settings.themeDark')}</span>
            {theme === 'dark' && <span className="settings-panel-option-check">✓</span>}
          </button>
          <button
            type="button"
            role="radio"
            aria-checked={theme === 'light'}
            className={`settings-panel-option${theme === 'light' ? ' active' : ''}`}
            onClick={() => setTheme('light')}
          >
            <span className="settings-panel-option-preview settings-panel-option-preview--light" aria-hidden="true" />
            <span className="settings-panel-option-label">{t('settings.themeLight')}</span>
            {theme === 'light' && <span className="settings-panel-option-check">✓</span>}
          </button>
        </div>
      )}

      {section === 'color' && (
        <div className="settings-panel-options settings-panel-options--accents" role="radiogroup" aria-label={title}>
          {ACCENT_OPTIONS.map((option) => (
            <button
              key={option.id}
              type="button"
              role="radio"
              aria-checked={accent === option.id}
              aria-label={option.label[language]}
              className={`settings-panel-option settings-panel-option--accent${accent === option.id ? ' active' : ''}`}
              style={{ '--swatch-color': option.swatch } as CSSProperties}
              onClick={() => setAccent(option.id)}
            >
              <span className="settings-panel-accent-swatch" aria-hidden="true" />
              <span className="settings-panel-option-label">{option.label[language]}</span>
              {accent === option.id && <span className="settings-panel-option-check">✓</span>}
            </button>
          ))}
        </div>
      )}

      {section === 'language' && (
        <div className="settings-panel-options" role="radiogroup" aria-label={title}>
          {LANGUAGE_OPTIONS.map((option) => (
            <button
              key={option.id}
              type="button"
              role="radio"
              aria-checked={language === option.id}
              className={`settings-panel-option${language === option.id ? ' active' : ''}`}
              onClick={() => setLanguage(option.id)}
            >
              <span className="settings-panel-option-label settings-panel-option-label--large">{option.label}</span>
              {language === option.id && <span className="settings-panel-option-check">✓</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
