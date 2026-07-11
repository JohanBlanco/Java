import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api'
import ActivityCalendar from '../../components/ActivityCalendar'
import ActivityOccurrenceEditModal from '../../components/ActivityOccurrenceEditModal'
import type { Activity } from '../../types'

export default function ReceptionCalendarioSection() {
  const [activities, setActivities] = useState<Activity[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Activity | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    api.getActivities()
      .then(setActivities)
      .catch(() => setActivities([]))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  if (loading) return <p>Cargando...</p>

  return (
    <>
      <ActivityCalendar
        activities={activities}
        editable
        onActivityEdit={setEditing}
      />
      {editing && (
        <ActivityOccurrenceEditModal
          activity={editing}
          onClose={() => setEditing(null)}
          onSaved={load}
        />
      )}
    </>
  )
}
