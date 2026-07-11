import { BrowserRouter, Routes, Route, Navigate, NavLink, Outlet } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import { ThemeProvider } from './theme'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import PlatformPage from './pages/PlatformPage'
import ProfilePage from './pages/ProfilePage'
import UserMenu from './components/UserMenu'
import CollapsibleNavGroup from './components/CollapsibleNavGroup'
import GymAdminLayout from './pages/gym-admin/GymAdminLayout'
import PackagesSection from './pages/gym-admin/PackagesSection'
import UsersSection from './pages/gym-admin/UsersSection'
import ReceptionLayout from './pages/reception/ReceptionLayout'
import ReceptionPagosSection from './pages/reception/ReceptionPagosSection'
import ReceptionActividadesSection from './pages/reception/ReceptionActividadesSection'
import ReceptionHoySection from './pages/reception/ReceptionHoySection'
import ReceptionCalendarioSection from './pages/reception/ReceptionCalendarioSection'
import VentasLayout from './pages/ventas/VentasLayout'
import VentasRegistroSection from './pages/ventas/VentasRegistroSection'
import EstadisticasLayout from './pages/estadisticas/EstadisticasLayout'
import EstadisticasResumenSection from './pages/estadisticas/EstadisticasResumenSection'
import {
  ADMIN_SECTIONS,
  ESTADISTICAS_SECTIONS,
  RECEPTION_SECTIONS,
  VENTAS_SECTIONS,
} from './navigation/sections'
import { canViewAdmin, canViewProfile, canViewReception, canViewVentas, canViewEstadisticas, hasRole } from './roles'
import { useSidebar } from './useSidebar'

function ProtectedLayout() {
  const { user, activeRole, setActiveRole, logout } = useAuth()
  const { sidebarOpen, toggleSidebar } = useSidebar()

  if (!user) return <Navigate to="/login" replace />

  const isPlatform = activeRole === 'PLATFORM_OWNER'
  const showAdmin = canViewAdmin(activeRole)
  const showVentas = canViewVentas(activeRole)
  const showEstadisticas = canViewEstadisticas(activeRole)
  const showReception = canViewReception(activeRole)

  return (
    <div className={`layout${sidebarOpen ? '' : ' layout--sidebar-collapsed'}`}>
      <nav className="sidebar" aria-hidden={!sidebarOpen}>
        <h2>GymPlatform</h2>
        <div className="sidebar-header">
          <UserMenu
            user={user}
            activeRole={activeRole}
            onActiveRoleChange={setActiveRole}
            logout={logout}
            placement="sidebar"
          />
        </div>
        <div className="sidebar-nav">
          {isPlatform ? (
            <NavLink to="/platform" className={({ isActive }) => isActive ? 'active' : ''}>
              Clientes
            </NavLink>
          ) : (
            <>
              <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>
                Inicio
              </NavLink>
              {showVentas && (
                <CollapsibleNavGroup
                  id="ventas"
                  label="Ventas"
                  basePath="/ventas"
                  sections={VENTAS_SECTIONS}
                />
              )}
              {showEstadisticas && (
                <CollapsibleNavGroup
                  id="estadisticas"
                  label="Estadísticas"
                  basePath="/estadisticas"
                  sections={ESTADISTICAS_SECTIONS}
                />
              )}
              {showReception && (
                <CollapsibleNavGroup
                  id="reception"
                  label="Recepción"
                  basePath="/reception"
                  sections={RECEPTION_SECTIONS}
                />
              )}
              {showAdmin && (
                <CollapsibleNavGroup
                  id="admin"
                  label="Administración"
                  basePath="/admin"
                  sections={ADMIN_SECTIONS}
                />
              )}
            </>
          )}
        </div>
      </nav>
      <button
        type="button"
        className="sidebar-edge-toggle"
        onClick={toggleSidebar}
        aria-label={sidebarOpen ? 'Ocultar menú lateral' : 'Mostrar menú lateral'}
        aria-expanded={sidebarOpen}
      >
        {sidebarOpen ? '‹' : '›'}
      </button>
      <main className="main">
        <div className="main-content">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

function OpsGuard() {
  const { user, activeRole } = useAuth()
  if (!user || !canViewReception(activeRole)) return <Navigate to="/" replace />
  return <Outlet />
}

function AdminGuard() {
  const { user, activeRole } = useAuth()
  if (!user || !canViewAdmin(activeRole)) return <Navigate to="/" replace />
  return <Outlet />
}

function ProfileGuard() {
  const { user, activeRole } = useAuth()
  if (!user || !canViewProfile(activeRole)) return <Navigate to="/" replace />
  return <ProfilePage />
}

function AppRoutes() {
  const { user, isLoading } = useAuth()

  if (isLoading) return <p style={{ padding: '2rem' }}>Cargando...</p>

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={
          user?.roles && hasRole(user, 'PLATFORM_OWNER') ? <Navigate to="/platform" replace /> : <DashboardPage />
        } />
        <Route path="/platform" element={<PlatformPage />} />

        <Route path="/ventas" element={<OpsGuard />}>
          <Route element={<VentasLayout />}>
            <Route index element={<Navigate to="registro" replace />} />
            <Route path="registro" element={<VentasRegistroSection />} />
          </Route>
        </Route>

        <Route path="/estadisticas" element={<OpsGuard />}>
          <Route element={<EstadisticasLayout />}>
            <Route index element={<Navigate to="resumen" replace />} />
            <Route path="resumen" element={<EstadisticasResumenSection />} />
          </Route>
        </Route>

        <Route path="/reception" element={<OpsGuard />}>
          <Route element={<ReceptionLayout />}>
            <Route index element={<Navigate to="pagos-pendientes" replace />} />
            <Route path="pagos-pendientes" element={<ReceptionPagosSection />} />
            <Route path="actividades" element={<ReceptionActividadesSection />} />
            <Route path="actividades-hoy" element={<ReceptionHoySection />} />
            <Route path="calendario" element={<ReceptionCalendarioSection />} />
          </Route>
        </Route>

        <Route path="/admin" element={<AdminGuard />}>
          <Route element={<GymAdminLayout />}>
            <Route index element={<Navigate to="membresias" replace />} />
            <Route path="membresias" element={<PackagesSection />} />
            <Route path="actividades" element={<Navigate to="/reception/actividades" replace />} />
            <Route path="plan-entrenamiento" element={<Navigate to="/admin/membresias" replace />} />
            <Route path="paquetes" element={<Navigate to="/admin/membresias" replace />} />
            <Route path="usuarios" element={<UsersSection />} />
          </Route>
        </Route>

        <Route path="/profile" element={<ProfileGuard />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}
