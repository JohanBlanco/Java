import { useEffect, useState } from 'react'
import { api } from '../api'
import type { User } from '../types'

export default function ProfilePage() {
  const [user, setUser] = useState<User | null>(null)
  const [birthYear, setBirthYear] = useState('')
  const [age, setAge] = useState('')
  const [goals, setGoals] = useState('')
  const [phone, setPhone] = useState('')
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    api.getMe().then((u) => {
      setUser(u)
      if (u.profile) {
        setBirthYear(u.profile.birthYear?.toString() ?? '')
        setAge(u.profile.age?.toString() ?? '')
        setGoals(u.profile.goals ?? '')
        setPhone(u.profile.phone ?? '')
      }
    })
  }, [])

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.updateProfile({
      birthYear: birthYear ? parseInt(birthYear) : null,
      age: age ? parseInt(age) : null,
      goals, phone,
    })
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  if (!user) return <p>Cargando...</p>

  return (
    <div>
      <div className="page-header">
        <h1>Mi perfil</h1>
        <p>{user.firstName} {user.lastName} · {user.email}</p>
      </div>

      <div className="card" style={{ maxWidth: 600 }}>
        {saved && <div style={{ color: 'var(--success)', marginBottom: '1rem' }}>Perfil actualizado</div>}
        <form onSubmit={handleSave}>
          <div className="form-group">
            <label>Año de nacimiento</label>
            <input type="number" value={birthYear} onChange={(e) => setBirthYear(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Edad</label>
            <input type="number" value={age} onChange={(e) => setAge(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Teléfono</label>
            <input value={phone} onChange={(e) => setPhone(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Objetivos</label>
            <textarea value={goals} onChange={(e) => setGoals(e.target.value)} rows={3} />
          </div>
          <button type="submit" className="btn-primary">Guardar</button>
        </form>
      </div>
    </div>
  )
}
