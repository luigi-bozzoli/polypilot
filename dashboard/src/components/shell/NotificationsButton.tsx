import { Bell } from 'lucide-react'

/**
 * Topbar notifications control (mock `.icon-btn` + `.badge-dot`).
 *
 * API-DEPENDENT — rendered as a disabled icon button for now.
 *
 * Data it will receive once an endpoint exists:
 *   GET /api/alerts?status=unread → { unreadCount: number, items: Alert[] }
 *   (schema: `alerts` table in orchestrator 001_schema.sql).
 *
 * Behaviour to implement when wired:
 *   - show `unreadCount` as a red badge on the bell; hide the badge when 0.
 *   - click opens the notifications drawer (mock 05-notifications.html) or
 *     routes to /alerts (mock 22-alerts.html).
 *   - poll or subscribe so the count stays fresh.
 *
 * No count is shown until the endpoint is real.
 */
export function NotificationsButton() {
  return (
    <button
      type="button"
      disabled
      aria-label="Notifications (unavailable)"
      className="flex h-[30px] w-[30px] items-center justify-center rounded-md border border-border bg-bg2 text-text-secondary disabled:cursor-not-allowed disabled:opacity-60"
    >
      <Bell className="h-4 w-4" aria-hidden="true" />
    </button>
  )
}
