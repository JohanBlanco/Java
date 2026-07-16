import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../auth'
import {
  AGENDA_ACTIVIDADES_SECTION,
  AGENDA_CITAS_SECTION,
  getAgendaSections,
} from '../../navigation/sections'

export default function AgendaLayout() {
  const { pathname } = useLocation()
  const { activeRole } = useAuth()
  const sections = getAgendaSections(activeRole)
  const active = sections.find((s) => pathname.endsWith(`/${s.path}`)) ?? sections[0]
  const showSwitcher = sections.length > 1

  return (
    <div className="agenda-page">
      <div className="page-header agenda-page-header">
        <div className="agenda-page-header-text">
          <h1>Agenda</h1>
          <p>{active?.description ?? 'Citas y actividades del gimnasio'}</p>
        </div>
        {showSwitcher && (
          <div className="agenda-mode-switch" role="tablist" aria-label="Tipo de agenda">
            {sections.some((s) => s.path === AGENDA_CITAS_SECTION.path) && (
              <NavLink
                to="/agenda/citas"
                role="tab"
                className={({ isActive }) =>
                  `agenda-mode-switch-btn${isActive ? ' active' : ''}`
                }
              >
                Citas
              </NavLink>
            )}
            {sections.some((s) => s.path === AGENDA_ACTIVIDADES_SECTION.path) && (
              <NavLink
                to="/agenda/actividades"
                role="tab"
                className={({ isActive }) =>
                  `agenda-mode-switch-btn${isActive ? ' active' : ''}`
                }
              >
                Actividades
              </NavLink>
            )}
          </div>
        )}
      </div>
      <div key={pathname} className="agenda-outlet">
        <Outlet />
      </div>
    </div>
  )
}
