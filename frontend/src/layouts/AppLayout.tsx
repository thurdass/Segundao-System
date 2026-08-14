import {
  Bell,
  CalendarDays,
  ClipboardCheck,
  GraduationCap,
  House,
  LayoutDashboard,
  LogOut,
  Menu,
  School,
  Settings2,
  UserRound,
  X,
} from 'lucide-react'
import { useState, type ComponentType } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

interface NavigationItem {
  label: string
  path: string
  icon: ComponentType<{ size?: number; strokeWidth?: number; 'aria-hidden'?: boolean }>
}

const navigationItems: NavigationItem[] = [
  { label: 'Início', path: '/dashboard', icon: House },
  { label: 'Atividades', path: '/activities', icon: ClipboardCheck },
  { label: 'Avisos', path: '/announcements', icon: Bell },
  { label: 'Horários', path: '/schedule', icon: CalendarDays },
  { label: 'Disciplinas', path: '/subjects', icon: School },
  { label: 'Professores', path: '/teachers', icon: GraduationCap },
]

function initials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')
}

export function AppLayout() {
  const { logout, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [isSidebarOpen, setSidebarOpen] = useState(false)

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  function closeSidebar() {
    setSidebarOpen(false)
  }

  const pageTitle = location.pathname === '/dashboard'
    ? 'Início'
    : location.pathname === '/profile'
      ? 'Perfil'
      : location.pathname.startsWith('/admin')
        ? 'Administração'
        : navigationItems.find((item) => location.pathname.startsWith(item.path))?.label ?? 'Segundão System'

  return (
    <div className="app-shell">
      <button
        className={`sidebar-overlay ${isSidebarOpen ? 'is-visible' : ''}`}
        type="button"
        aria-label="Fechar menu"
        onClick={closeSidebar}
      />

      <aside className={`app-sidebar ${isSidebarOpen ? 'is-open' : ''}`}>
        <div className="sidebar-top">
          <div className="sidebar-brand">
            <img src="/img.png" alt="" />
            <div>
              <strong>Segundão</strong>
              <span>2º Informática A</span>
            </div>
          </div>
          <button className="icon-button sidebar-close" type="button" aria-label="Fechar menu" onClick={closeSidebar}>
            <X size={20} aria-hidden="true" />
          </button>
        </div>

        <nav className="sidebar-navigation" aria-label="Navegação principal">
          <p className="sidebar-section-label">Workspace</p>
          {navigationItems.map((item) => {
            const Icon = item.icon

            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={closeSidebar}
              >
                <Icon size={18} strokeWidth={1.9} aria-hidden={true} />
                <span>{item.label}</span>
              </NavLink>
            )
          })}

          {user?.role === 'ADMIN' && (
            <>
              <p className="sidebar-section-label sidebar-section-spaced">Gestão</p>
              <NavLink
                to="/admin"
                className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                onClick={closeSidebar}
              >
                <LayoutDashboard size={18} strokeWidth={1.9} aria-hidden="true" />
                <span>Administração</span>
              </NavLink>
            </>
          )}
        </nav>

        <div className="sidebar-footer">
          <NavLink to="/profile" className="sidebar-user" onClick={closeSidebar}>
            <span className="avatar avatar-small">{user ? initials(user.displayName) : '?'}</span>
            <span className="sidebar-user-copy">
              <strong>{user?.displayName}</strong>
              <small>2º Informática A</small>
            </span>
            <UserRound size={16} aria-hidden="true" />
          </NavLink>
          <button className="sidebar-logout" type="button" onClick={handleLogout}>
            <LogOut size={17} aria-hidden="true" />
            Sair
          </button>
        </div>
      </aside>

      <div className="app-content">
        <header className="app-topbar">
          <button className="icon-button menu-button" type="button" aria-label="Abrir menu" onClick={() => setSidebarOpen(true)}>
            <Menu size={21} aria-hidden="true" />
          </button>
          <div>
            <p className="topbar-kicker">Segundão System</p>
            <h1>{pageTitle}</h1>
          </div>
          <div className="topbar-user">
            <span className="avatar">{user ? initials(user.displayName) : '?'}</span>
            <span>{user?.displayName}</span>
            {user?.role === 'ADMIN' && <Settings2 size={16} aria-label="Administrador" />}
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
