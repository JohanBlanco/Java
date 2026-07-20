const STORAGE_KEY = 'gymplatform.whatsappProspectContacts'

export type SavedWhatsappContact = {
  phoneLocal: string
  firstName: string
  savedAt: string
}

function readAll(): SavedWhatsappContact[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as SavedWhatsappContact[]
    if (!Array.isArray(parsed)) return []
    return parsed.filter(
      (c) => typeof c?.phoneLocal === 'string' && /^\d{8}$/.test(c.phoneLocal),
    )
  } catch {
    return []
  }
}

function writeAll(contacts: SavedWhatsappContact[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(contacts.slice(0, 40)))
}

export function listSavedWhatsappContacts(): SavedWhatsappContact[] {
  return readAll().sort((a, b) => b.savedAt.localeCompare(a.savedAt))
}

export function saveWhatsappContact(phoneLocal: string, firstName: string) {
  const digits = phoneLocal.replace(/\D/g, '').slice(0, 8)
  if (!/^\d{8}$/.test(digits)) return
  const name = firstName.trim()
  const next = readAll().filter((c) => c.phoneLocal !== digits)
  next.unshift({
    phoneLocal: digits,
    firstName: name,
    savedAt: new Date().toISOString(),
  })
  writeAll(next)
}

export function removeSavedWhatsappContact(phoneLocal: string) {
  const digits = phoneLocal.replace(/\D/g, '')
  writeAll(readAll().filter((c) => c.phoneLocal !== digits))
}
