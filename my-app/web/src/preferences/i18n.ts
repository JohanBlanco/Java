import { useCallback } from 'react'
import type { Language } from './types'

const messages = {
  es: {
    'settings.title': 'Configuración',
    'settings.back': 'Volver al menú',
    'settings.theme': 'Tema',
    'settings.themeDark': 'Oscuro',
    'settings.themeLight': 'Claro',
    'settings.appearance': 'Apariencia',
    'settings.color': 'Color',
    'settings.language': 'Idioma',
    'settings.themeDescription': 'Elige entre modo oscuro o claro para toda la interfaz.',
    'settings.colorDescription': 'Personaliza el color de acento de botones, enlaces y elementos destacados.',
    'settings.languageDescription': 'Selecciona el idioma del menú y la navegación.',
    'settings.broadcastMessages': 'Mensajes de difusión',
    'settings.broadcastWhatsapp': 'WhatsApp',
    'settings.broadcastWhatsappDescription': 'Número y plantillas para envíos automáticos por WhatsApp.',
    'settings.forms': 'Formularios',
    'settings.formsDescription': 'Diseña formularios personalizados para tu gimnasio.',
    'settings.forums': 'Foros',
    'settings.forumsDescription': 'Contenido de guías de ejercicios (texto en la base de datos; media referenciada).',
    'userMenu.settings': 'Configuración',
    'userMenu.profile': 'Ver perfil',
    'userMenu.logout': 'Cerrar sesión',
    'userMenu.currentProfile': 'Perfil actual',
    'nav.home': 'Inicio',
    'nav.clients': 'Clientes',
    'nav.services': 'Servicios',
    'nav.sales': 'Tienda',
    'nav.stats': 'Estadísticas',
    'nav.admin': 'Administración',
    'nav.training': 'Plan de entrenamiento',
    'nav.agenda': 'Agenda',
    'nav.expedientes': 'Expedientes',
  },
  en: {
    'settings.title': 'Settings',
    'settings.back': 'Back to menu',
    'settings.theme': 'Theme',
    'settings.themeDark': 'Dark',
    'settings.themeLight': 'Light',
    'settings.appearance': 'Appearance',
    'settings.color': 'Color',
    'settings.language': 'Language',
    'settings.themeDescription': 'Choose dark or light mode for the entire interface.',
    'settings.colorDescription': 'Customize the accent color for buttons, links, and highlights.',
    'settings.languageDescription': 'Select the language for the menu and navigation.',
    'settings.broadcastMessages': 'Broadcast messages',
    'settings.broadcastWhatsapp': 'WhatsApp',
    'settings.broadcastWhatsappDescription': 'Phone number and templates for automatic WhatsApp messages.',
    'settings.forms': 'Forms',
    'settings.formsDescription': 'Design custom forms for your gym.',
    'settings.forums': 'Forums',
    'settings.forumsDescription': 'Exercise guide content (text stored in the database; media referenced remotely).',
    'userMenu.settings': 'Settings',
    'userMenu.profile': 'View profile',
    'userMenu.logout': 'Log out',
    'userMenu.currentProfile': 'Current profile',
    'nav.home': 'Home',
    'nav.clients': 'Clients',
    'nav.services': 'Services',
    'nav.sales': 'Store',
    'nav.stats': 'Statistics',
    'nav.admin': 'Administration',
    'nav.training': 'Training plan',
    'nav.agenda': 'Schedule',
    'nav.expedientes': 'Member files',
  },
} as const

export type MessageKey = keyof typeof messages.es

export function translate(language: Language, key: MessageKey): string {
  return messages[language][key] ?? messages.es[key] ?? key
}

export function useTranslate(language: Language) {
  return useCallback((key: MessageKey) => translate(language, key), [language])
}
