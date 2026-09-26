/** ISO timestamp → "Sep 3 14:31:04" (UTC), matching mock `19-audit-log.html`'s mono time column. */
export function formatAuditTimestamp(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  const datePart = date.toLocaleDateString('en-US', { timeZone: 'UTC', month: 'short', day: 'numeric' })
  const timePart = date.toLocaleTimeString('en-GB', { timeZone: 'UTC', hour12: false })
  return `${datePart} ${timePart}`
}
