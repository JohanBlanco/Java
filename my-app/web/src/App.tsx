import { useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate, NavLink, Outlet, useSearchParams } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import { PreferencesProvider, usePreferences } from './preferences'
import { ToastProvider } from './toast'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import PlatformPage from './pages/PlatformPage'
import ProfilePage from './pages/ProfilePage'
import UserMenu from './components/UserMenu'
import SettingsSidebar from './components/SettingsSidebar'
import SettingsPanel from './components/SettingsPanel'
import CollapsibleNavGroup from './components/CollapsibleNavGroup'
import NavSectionLayout from './components/NavSectionLayout'
import PackagesSection from './pages/gym-admin/PackagesSection'
import UsersSection from './pages/gym-admin/UsersSection'
import ProductsSection from './pages/reception/ProductsSection'
import ReceptionLayout from './pages/reception/ReceptionLayout'
import ReceptionCalendarioSection from './pages/reception/ReceptionCalendarioSection'
import VentasLayout from './pages/ventas/VentasLayout'
import TiendaPuntoDeVentaSection from './pages/ventas/TiendaPuntoDeVentaSection'
import TiendaHistorialSection from './pages/ventas/TiendaHistorialSection'
import EstadisticasLayout from './pages/estadisticas/EstadisticasLayout'
import EstadisticasResumenSection from './pages/estadisticas/EstadisticasResumenSection'
import OperacionesIndexRedirect from './pages/inicio/OperacionesIndexRedirect'
import AppointmentRequestsPage from './pages/inicio/AppointmentRequestsPage'
import MemberActivitiesPage from './pages/inicio/MemberActivitiesPage'
import MemberReservationsPage from './pages/inicio/MemberReservationsPage'
import MemberRoutinesPage from './pages/inicio/MemberRoutinesPage'
import MemberMeasurementsPage from './pages/inicio/MemberMeasurementsPage'
import MemberNutritionPage from './pages/inicio/MemberNutritionPage'
import TrainingLayout from './pages/training/TrainingLayout'
import RoutinesSection from './pages/training/RoutinesSection'
import AppointmentsSection from './pages/training/AppointmentsSection'
import MeasurementsSection from './pages/training/MeasurementsSection'
import NutritionSection from './pages/training/NutritionSection'
import AgendaLayout from './pages/agenda/AgendaLayout'
import AgendaIndexRedirect from './pages/agenda/AgendaIndexRedirect'
import {
  ESTADISTICAS_SECTIONS,
  MEMBER_SECTIONS,
  RECEPTION_SECTIONS,
  TRAINING_SECTIONS,
  VENTAS_SECTIONS,
  canViewAgenda,
} from './navigation/sections'
import {
  canViewProfile,
  canViewReception,
  canViewTrainingAdmin,
  canViewVentas,
  canViewEstadisticas,
  hasRole,
  isMemberView,
} from './roles'
import { useSidebar } from './useSidebar'
import type { SettingsSection } from './preferences'
import GlobalUIScale from './components/GlobalUIScale'
import PublicFormPage from './pages/PublicFormPage'
import MemberFilesPage from './pages/expedientes/MemberFilesPage'
import ExpedientesLegacyRedirect from './pages/expedientes/ExpedientesLegacyRedirect'

function ProtectedLayout() {
  const { user, activeRole, setActiveRole, logout } = useAuth()
  const { sidebarOpen, setSidebarOpen, toggleSidebar } = useSidebar()
  const { t } = usePreferences()
  const [sidebarView, setSidebarView] = useState<'nav' | 'settings'>('nav')
  const [settingsSection, setSettingsSection] = useState<SettingsSection>('theme')

  if (!user) return <Navigate to="/login" replace />

  const isPlatform = activeRole === 'PLATFORM_OWNER'
  const showVentas = canViewVentas(activeRole)
  const showEstadisticas = canViewEstadisticas(activeRole)
  const showReception = canViewReception(activeRole)
  const showTraining = canViewTrainingAdmin(activeRole)
  const showAgenda = canViewAgenda(activeRole)
  const showMemberNav = isMemberView(activeRole)

  const openSettings = () => {
    setSidebarView('settings')
    setSettingsSection('theme')
    setSidebarOpen(true)
  }

  const closeSettings = () => setSidebarView('nav')

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
            onOpenSettings={openSettings}
          />
        </div>
        <div className="sidebar-nav">
          {sidebarView === 'settings' ? (
            <SettingsSidebar
              activeSection={settingsSection}
              onSectionChange={setSettingsSection}
              onBack={closeSettings}
              showBroadcastSettings={showReception}
              showFormsSettings={showReception}
              showForumsSettings={showReception || showTraining}
              showCashSettings={showReception}
            />
          ) : isPlatform ? (
            <NavLink to="/platform" className={({ isActive }) => isActive ? 'active' : ''}>
              {t('nav.clients')}
            </NavLink>
          ) : (
            <>
              <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>
                {t('nav.home')}
              </NavLink>
              {showMemberNav && (
                <CollapsibleNavGroup
                  id="servicios"
                  label={t('nav.services')}
                  basePath="/servicios"
                  sections={MEMBER_SECTIONS}
                />
              )}
              {showAgenda && (
                <NavLink
                  to="/agenda"
                  className={({ isActive }) => (isActive ? 'active' : '')}
                >
                  {t('nav.agenda')}
                </NavLink>
              )}
              {showVentas && (
                <CollapsibleNavGroup
                  id="ventas"
                  label={t('nav.sales')}
                  basePath="/ventas"
                  sections={VENTAS_SECTIONS}
                />
              )}
              {showReception && (
                <CollapsibleNavGroup
                  id="reception"
                  label={t('nav.admin')}
                  basePath="/reception"
                  sections={RECEPTION_SECTIONS}
                />
              )}
              {showTraining && (
                <CollapsibleNavGroup
                  id="training"
                  label={t('nav.training')}
                  basePath="/training"
                  sections={TRAINING_SECTIONS}
                />
              )}
              {showEstadisticas && (
                <CollapsibleNavGroup
                  id="estadisticas"
                  label={t('nav.stats')}
                  basePath="/estadisticas"
                  sections={ESTADISTICAS_SECTIONS}
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
          {sidebarView === 'settings' ? (
            <SettingsPanel section={settingsSection} />
          ) : (
            <Outlet />
          )}
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

function MemberGuard() {
  const { user, activeRole } = useAuth()
  if (!user || !isMemberView(activeRole)) return <Navigate to="/" replace />
  return <Outlet />
}

function ProfileGuard() {
  const { user, activeRole } = useAuth()
  if (!user || !canViewProfile(activeRole)) return <Navigate to="/" replace />
  return <ProfilePage />
}

function TrainingGuard() {
  const { user, activeRole } = useAuth()
  if (!user || !canViewTrainingAdmin(activeRole)) return <Navigate to="/" replace />
  return <Outlet />
}

function AgendaGuard() {
  const { user, activeRole } = useAuth()
  if (!user || !canViewAgenda(activeRole)) return <Navigate to="/" replace />
  return <Outlet />
}

function AgendaCitasPage() {
  const { activeRole } = useAuth()
  if (!canViewTrainingAdmin(activeRole)) {
    return <Navigate to="/agenda/actividades" replace />
  }
  return <AppointmentsSection />
}

function AgendaActividadesPage() {
  const { activeRole } = useAuth()
  if (!canViewReception(activeRole)) {
    return <Navigate to="/agenda/citas" replace />
  }
  return <ReceptionCalendarioSection />
}

function LoginRoute() {
  const { user } = useAuth()
  const [searchParams] = useSearchParams()
  const returnTo = searchParams.get('return')
  if (user) {
    const safeReturn = returnTo && returnTo.startsWith('/') && !returnTo.startsWith('//')
      ? returnTo
      : '/'
    return <Navigate to={safeReturn} replace />
  }
  return <LoginPage />
}
function AppRoutes() {
  const { user, isLoading } = useAuth()

  if (isLoading) return <p style={{ padding: '2rem' }}>Cargando...</p>

  return (
    <Routes>
      <Route path="/login" element={<LoginRoute />} />
      <Route path="/f/:organizationSlug/:formSlug" element={<PublicFormPage />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={
          user?.roles && hasRole(user, 'PLATFORM_OWNER') ? <Navigate to="/platform" replace /> : <DashboardPage />
        } />
        <Route path="/operaciones" element={<Navigate to="/" replace />} />
        <Route path="/operaciones/:section" element={<OperacionesIndexRedirect />} />

        <Route path="/servicios" element={<MemberGuard />}>
          <Route element={
            <NavSectionLayout
              sections={MEMBER_SECTIONS}
              fallbackTitle="Servicios"
              fallbackDescription="Actividades, reservaciones y citas"
            />
          }>
            <Route index element={<Navigate to="actividades" replace />} />
            <Route path="actividades" element={<MemberActivitiesPage />} />
            <Route path="reservaciones" element={<MemberReservationsPage />} />
            <Route path="rutinas" element={<MemberRoutinesPage />} />
            <Route path="medidas" element={<MemberMeasurementsPage />} />
            <Route path="nutricion" element={<MemberNutritionPage />} />
            <Route path="solicitudes-citas" element={<AppointmentRequestsPage />} />
          </Route>
        </Route>

        <Route path="/inicio/pagos-pendientes" element={<Navigate to="/?panel=pendientes-de-pago" replace />} />
        <Route path="/inicio/pendientes-de-pago" element={<Navigate to="/?panel=pendientes-de-pago" replace />} />
        <Route path="/inicio/actividades-hoy" element={<Navigate to="/?panel=actividades-hoy" replace />} />
        <Route path="/inicio/solicitudes-rutina" element={<Navigate to="/?panel=solicitudes-rutina" replace />} />
        <Route path="/inicio/solicitudes-citas" element={<Navigate to="/?panel=solicitudes-citas" replace />} />
        <Route path="/inicio/actividades" element={<Navigate to="/servicios/actividades" replace />} />
        <Route path="/inicio/reservaciones" element={<Navigate to="/servicios/reservaciones" replace />} />
        <Route path="/inicio/rutinas" element={<Navigate to="/servicios/rutinas" replace />} />

        <Route path="/expedientes/*" element={<ExpedientesLegacyRedirect />} />

        <Route path="/platform" element={<PlatformPage />} />

        <Route path="/ventas" element={<OpsGuard />}>
          <Route element={<VentasLayout />}>
            <Route index element={<Navigate to="punto-de-venta" replace />} />
            <Route path="punto-de-venta" element={<TiendaPuntoDeVentaSection />} />
            <Route path="historial" element={<TiendaHistorialSection />} />
            <Route path="registro" element={<Navigate to="/ventas/historial" replace />} />
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
            <Route index element={<Navigate to="usuarios" replace />} />
            <Route path="pagos-pendientes" element={<Navigate to="/?panel=pendientes-de-pago" replace />} />
            <Route path="pendientes-de-pago" element={<Navigate to="/?panel=pendientes-de-pago" replace />} />
            <Route path="actividades" element={<Navigate to="/agenda/actividades" replace />} />
            <Route path="actividades-hoy" element={<Navigate to="/?panel=actividades-hoy" replace />} />
            <Route path="calendario" element={<Navigate to="/agenda/actividades" replace />} />
            <Route path="membresias" element={<PackagesSection />} />
            <Route path="usuarios" element={<UsersSection />} />
            <Route path="productos" element={<ProductsSection />} />
            <Route path="expedientes" element={<MemberFilesPage />} />
            <Route path="expedientes/usuario/:userId" element={<MemberFilesPage />} />
            <Route path="expedientes/usuario/:userId/archivo/:submissionId" element={<MemberFilesPage />} />
          </Route>
        </Route>

        <Route path="/agenda" element={<AgendaGuard />}>
          <Route element={<AgendaLayout />}>
            <Route index element={<AgendaIndexRedirect />} />
            <Route path="citas" element={<AgendaCitasPage />} />
            <Route path="actividades" element={<AgendaActividadesPage />} />
          </Route>
        </Route>

        <Route path="/training" element={<TrainingGuard />}>
          <Route element={<TrainingLayout />}>
            <Route index element={<Navigate to="rutinas" replace />} />
            <Route path="rutinas" element={<RoutinesSection />} />
            <Route path="medidas" element={<MeasurementsSection />} />
            <Route path="nutricion" element={<NutritionSection />} />
            <Route path="citas" element={<Navigate to="/agenda/citas" replace />} />
          </Route>
        </Route>

        <Route path="/admin" element={<Navigate to="/reception/usuarios" replace />} />
        <Route path="/admin/membresias" element={<Navigate to="/reception/membresias" replace />} />
        <Route path="/admin/usuarios" element={<Navigate to="/reception/usuarios" replace />} />
        <Route path="/admin/actividades" element={<Navigate to="/agenda/actividades" replace />} />
        <Route path="/admin/plan-entrenamiento" element={<Navigate to="/training/rutinas" replace />} />
        <Route path="/admin/paquetes" element={<Navigate to="/reception/membresias" replace />} />

        <Route path="/profile" element={<ProfileGuard />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <PreferencesProvider>
      <GlobalUIScale />
      <AuthProvider>
        <BrowserRouter>
          <ToastProvider>
            <AppRoutes />
          </ToastProvider>
        </BrowserRouter>
      </AuthProvider>
    </PreferencesProvider>
  )
}
