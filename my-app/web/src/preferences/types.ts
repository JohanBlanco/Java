export type ThemeMode = 'dark' | 'light'

export type AccentId = 'indigo' | 'emerald' | 'rose' | 'amber' | 'sky'

export type Language = 'es' | 'en'

export type SettingsSection = 'theme' | 'color' | 'language' | 'broadcast-whatsapp' | 'forms' | 'forums' | 'cash'

export type UserPreferences = {
  theme: ThemeMode
  accent: AccentId
  language: Language
}

export const DEFAULT_PREFERENCES: UserPreferences = {
  theme: 'dark',
  accent: 'indigo',
  language: 'es',
}

export const ACCENT_OPTIONS: { id: AccentId; label: Record<Language, string>; swatch: string }[] = [
  { id: 'indigo', label: { es: 'Índigo', en: 'Indigo' }, swatch: '#6366f1' },
  { id: 'emerald', label: { es: 'Esmeralda', en: 'Emerald' }, swatch: '#10b981' },
  { id: 'rose', label: { es: 'Rosa', en: 'Rose' }, swatch: '#f43f5e' },
  { id: 'amber', label: { es: 'Ámbar', en: 'Amber' }, swatch: '#f59e0b' },
  { id: 'sky', label: { es: 'Cielo', en: 'Sky' }, swatch: '#0ea5e9' },
]

export const LANGUAGE_OPTIONS: { id: Language; label: string }[] = [
  { id: 'es', label: 'Español' },
  { id: 'en', label: 'English' },
]
