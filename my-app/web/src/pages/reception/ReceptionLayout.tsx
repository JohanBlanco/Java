import { Outlet, useLocation } from 'react-router-dom'
import { RECEPTION_SECTIONS } from '../../navigation/sections'

export default function ReceptionLayout() {
  const { pathname } = useLocation()
  const active = RECEPTION_SECTIONS.find((s) => pathname.endsWith(`/${s.path}`))

  return (
    <div>
      <div className="page-header">
        <h1>{active?.label ?? 'Recepción'}</h1>
        <p>{active?.description ?? 'Operaciones de recepción'}</p>
      </div>
      <Outlet />
    </div>
  )
}
