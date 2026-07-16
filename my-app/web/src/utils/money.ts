export function formatMoney(value: number) {
  return new Intl.NumberFormat('es-CR', {
    style: 'currency',
    currency: 'CRC',
    maximumFractionDigits: 0,
  }).format(value || 0)
}

/**
 * Monto seguro para jsPDF (Helvetica no incluye ₡ ni espacios tipográficos de es-CR).
 */
export function formatMoneyPdf(value: number) {
  const n = new Intl.NumberFormat('es-CR', {
    maximumFractionDigits: 0,
    useGrouping: true,
  })
    .format(value || 0)
    .replace(/[\u00A0\u202F\u2009]/g, ' ')
  return `CRC ${n}`
}
