import { Outlet, useLocation } from 'react-router-dom'
import { ADMIN_SECTIONS } from '../../navigation/sections'

export default function GymAdminLayout() {
  const { pathname } = useLocation()
  const active = ADMIN_SECTIONS.find((s) => pathname.endsWith(`/${s.path}`))

  return (
    <div>
      <div className="page-header">
        <h1>{active?.label ?? 'Administración del gimnasio'}</h1>
        <p>{active?.description ?? 'Gestiona membresías, actividades y usuarios de tu gimnasio'}</p>
      </div>
      <Outlet />
    </div>
  )
}
