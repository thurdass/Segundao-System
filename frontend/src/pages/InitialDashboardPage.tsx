import { LogOut } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function InitialDashboardPage() {
  const { logout, user } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <main className="placeholder-page">
      <div className="placeholder-card">
        <p className="eyebrow">Segundão System</p>
        <h1>Olá, {user?.displayName}</h1>
        <p>A dashboard da turma será carregada nesta área.</p>
        <button className="secondary-button" type="button" onClick={handleLogout}>
          <LogOut size={17} aria-hidden="true" />
          Sair
        </button>
      </div>
    </main>
  )
}
