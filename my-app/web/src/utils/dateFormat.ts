import type { Language } from '../preferences/types'
import type { CalendarView } from './calendarUtils'
import { addDays, parseDate, startOfWeek } from './calendarUtils'

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

export function getDateLocale(language: Language): string {
  return language === 'es' ? 'es-MX' : 'en-US'
}

function toDate(value: Date | string): Date {
  if (value instanceof Date) return value
  if (ISO_DATE.test(value)) return parseDate(value)
  return new Date(value)
}

/** Fecha corta: dd/mm/yyyy (es) o mm/dd/yyyy (en). */
export function formatDate(
  value: Date | string,
  language: Language,
  options?: Intl.DateTimeFormatOptions,
): string {
  return toDate(value).toLocaleDateString(getDateLocale(language), {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    ...options,
  })
}

export function formatDateTime(value: Date | string, language: Language): string {
  return toDate(value).toLocaleString(getDateLocale(language), {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatIsoDate(isoDate: string, language: Language): string {
  return formatDate(parseDate(isoDate), language)
}

export function formatDateRangeLabel(
  startDate: string,
  endDate: string | undefined,
  language: Language,
): string {
  if (!endDate || endDate === startDate) return formatIsoDate(startDate, language)
  return `${formatIsoDate(startDate, language)} → ${formatIsoDate(endDate, language)}`
}

export function formatPeriodLabel(
  view: CalendarView,
  anchor: Date,
  language: Language,
): string {
  const locale = getDateLocale(language)
  if (view === 'day') {
    return anchor.toLocaleDateString(locale, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    })
  }
  if (view === 'week') {
    const from = startOfWeek(anchor)
    const to = addDays(from, 6)
    return `${from.toLocaleDateString(locale, { day: 'numeric', month: 'short' })} – ${to.toLocaleDateString(locale, { day: 'numeric', month: 'short', year: 'numeric' })}`
  }
  if (view === 'month') {
    return anchor.toLocaleDateString(locale, { month: 'long', year: 'numeric' })
  }
  return String(anchor.getFullYear())
}

export function dateSearchTerms(value: Date | string): string[] {
  const date = toDate(value)
  const iso = ISO_DATE.test(String(value)) ? String(value) : undefined
  const terms = [
    date.toISOString(),
    formatDate(date, 'es'),
    formatDate(date, 'en'),
    date.toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' }),
    date.toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' }),
  ]
  if (iso) {
    terms.push(iso, formatIsoDate(iso, 'es'), formatIsoDate(iso, 'en'))
  }
  return terms
}
