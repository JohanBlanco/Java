import { useMemo, useState } from 'react'
import type { Activity } from '../types'
import {
  type CalendarView,
  WEEKDAY_LABELS,
  MONTH_LABELS,
  activitiesForDay,
  activitiesForRange,
  addDays,
  formatPeriodLabel,
  getRangeForView,
  isSameDay,
  shiftAnchor,
  startOfWeek,
  toIsoDate,
} from '../utils/calendarUtils'

type Props = {
  activities: Activity[]
  editable?: boolean
  onActivityEdit?: (activity: Activity) => void
}

const VIEWS: { id: CalendarView; label: string }[] = [
  { id: 'day', label: 'Día' },
  { id: 'week', label: 'Semana' },
  { id: 'month', label: 'Mes' },
  { id: 'year', label: 'Año' },
]

function activityKey(a: Activity): string {
  return `${a.id}-${a.activityDate}`
}

function ActivityChip({
  activity,
  editable,
  onEdit,
}: {
  activity: Activity
  editable?: boolean
  onEdit?: (activity: Activity) => void
}) {
  const handleClick = () => {
    if (editable && onEdit) onEdit(activity)
  }

  return (
    <div
      className={`calendar-activity${editable ? ' calendar-activity--editable' : ''}${activity.hasOccurrenceOverride ? ' calendar-activity--override' : ''}`}
      onClick={handleClick}
      onKeyDown={(e) => editable && e.key === 'Enter' && handleClick()}
      role={editable ? 'button' : undefined}
      tabIndex={editable ? 0 : undefined}
    >
      <strong>
        {activity.name}
        {activity.hasOccurrenceOverride && (
          <span className="calendar-override-dot" title="Horario modificado" />
        )}
      </strong>
      <span>{activity.startTime?.slice(0, 5)} · {activity.locationName}</span>
    </div>
  )
}

function MonthPill({
  activity,
  editable,
  onEdit,
}: {
  activity: Activity
  editable?: boolean
  onEdit?: (activity: Activity) => void
}) {
  const label = activity.hasOccurrenceOverride ? `${activity.name}*` : activity.name
  if (!editable || !onEdit) {
    return <span className="calendar-month-pill">{label}</span>
  }
  return (
    <button
      type="button"
      className={`calendar-month-pill calendar-month-pill--btn${activity.hasOccurrenceOverride ? ' override' : ''}`}
      onClick={() => onEdit(activity)}
    >
      {label}
    </button>
  )
}

export default function ActivityCalendar({ activities, editable = false, onActivityEdit }: Props) {
  const [view, setView] = useState<CalendarView>('month')
  const [anchor, setAnchor] = useState(() => new Date())

  const range = useMemo(() => getRangeForView(view, anchor), [view, anchor])
  const inRange = useMemo(
    () => activitiesForRange(activities, range.from, range.to),
    [activities, range.from, range.to],
  )

  const goToday = () => setAnchor(new Date())

  return (
    <div className="activity-calendar card">
      <div className="calendar-toolbar">
        <div className="calendar-view-tabs">
          {VIEWS.map((v) => (
            <button
              key={v.id}
              type="button"
              className={`calendar-tab${view === v.id ? ' active' : ''}`}
              onClick={() => setView(v.id)}
            >
              {v.label}
            </button>
          ))}
        </div>
        <div className="calendar-nav">
          <button type="button" className="btn-secondary" onClick={() => setAnchor((d) => shiftAnchor(view, d, -1))}>‹</button>
          <span className="calendar-period">{formatPeriodLabel(view, anchor)}</span>
          <button type="button" className="btn-secondary" onClick={() => setAnchor((d) => shiftAnchor(view, d, 1))}>›</button>
          <button type="button" className="btn-secondary" onClick={goToday}>Hoy</button>
        </div>
      </div>

      {editable && (
        <p className="calendar-hint">Haz clic en una actividad para editarla.</p>
      )}

      {view === 'day' && (
        <div className="calendar-day-view">
          {activitiesForDay(activities, anchor).length === 0 ? (
            <p className="calendar-empty">Sin actividades este día</p>
          ) : activitiesForDay(activities, anchor).map((a) => (
            <ActivityChip
              key={activityKey(a)}
              activity={a}
              editable={editable}
              onEdit={onActivityEdit}
            />
          ))}
        </div>
      )}

      {view === 'week' && (
        <div className="calendar-week-grid">
          {Array.from({ length: 7 }, (_, i) => {
            const day = addDays(startOfWeek(anchor), i)
            const dayActs = activitiesForDay(activities, day)
            return (
              <div key={toIsoDate(day)} className={`calendar-week-col${isSameDay(day, new Date()) ? ' today' : ''}`}>
                <div className="calendar-week-head">
                  <span>{WEEKDAY_LABELS[i]}</span>
                  <strong>{day.getDate()}</strong>
                </div>
                {dayActs.length === 0 ? (
                  <p className="calendar-empty-sm">—</p>
                ) : dayActs.map((a) => (
                  <ActivityChip
                    key={activityKey(a)}
                    activity={a}
                    editable={editable}
                    onEdit={onActivityEdit}
                  />
                ))}
              </div>
            )
          })}
        </div>
      )}

      {view === 'month' && (
        <div className="calendar-month">
          <div className="calendar-month-head">
            {WEEKDAY_LABELS.map((label) => (
              <span key={label}>{label}</span>
            ))}
          </div>
          <div className="calendar-month-grid">
            {(() => {
              const first = new Date(anchor.getFullYear(), anchor.getMonth(), 1)
              const start = startOfWeek(first)
              const cells = []
              for (let i = 0; i < 42; i++) {
                const day = addDays(start, i)
                const inMonth = day.getMonth() === anchor.getMonth()
                const dayActs = activitiesForDay(activities, day)
                cells.push(
                  <div
                    key={toIsoDate(day)}
                    className={`calendar-month-cell${inMonth ? '' : ' muted'}${isSameDay(day, new Date()) ? ' today' : ''}`}
                  >
                    <span className="calendar-day-num">{day.getDate()}</span>
                    {dayActs.slice(0, 2).map((a) => (
                      <MonthPill
                        key={activityKey(a)}
                        activity={a}
                        editable={editable}
                        onEdit={onActivityEdit}
                      />
                    ))}
                    {dayActs.length > 2 && (
                      <span className="calendar-more">+{dayActs.length - 2}</span>
                    )}
                  </div>,
                )
              }
              return cells
            })()}
          </div>
        </div>
      )}

      {view === 'year' && (
        <div className="calendar-year-grid">
          {MONTH_LABELS.map((label, monthIndex) => {
            const monthStart = new Date(anchor.getFullYear(), monthIndex, 1)
            const monthEnd = new Date(anchor.getFullYear(), monthIndex + 1, 0)
            const monthActs = activitiesForRange(activities, monthStart, monthEnd)
            return (
              <button
                key={label}
                type="button"
                className="calendar-year-month"
                onClick={() => {
                  setAnchor(monthStart)
                  setView('month')
                }}
              >
                <strong>{label}</strong>
                <span>{monthActs.length} actividad{monthActs.length === 1 ? '' : 'es'}</span>
              </button>
            )
          })}
        </div>
      )}

      {inRange.length > 0 && view !== 'year' && (
        <p className="calendar-summary">{inRange.length} actividad{inRange.length === 1 ? '' : 'es'} en el periodo</p>
      )}
    </div>
  )
}
