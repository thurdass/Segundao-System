import { ArrowRight, LockKeyhole, UserRound } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { AuthShell } from '../components/AuthShell'
import { getApiErrorMessage } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import { LoadingScreen } from '../components/LoadingScreen'

export function LoginPage() {
  const { isAuthenticated, isLoading, login, user } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isLoading) {
    return <LoadingScreen />
  }

  if (isAuthenticated && user) {
    return <Navigate to={user.mustChangePassword ? '/change-password' : '/dashboard'} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')

    if (!username.trim() || !password) {
      setErrorMessage('Informe seu usuário e sua senha.')
      return
    }

    setIsSubmitting(true)

    try {
      const response = await login({ username: username.trim(), password })
      navigate(response.mustChangePassword ? '/change-password' : '/dashboard', {
        replace: true,
      })
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível entrar. Confira suas credenciais.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthShell
      eyebrow="Acesso da turma"
      title="Bem-vindo de volta"
      description="Entre com as credenciais recebidas do administrador."
    >
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="username">
          Usuário
        </label>
        <div className="input-wrap">
          <UserRound size={18} aria-hidden="true" />
          <input
            id="username"
            name="username"
            type="text"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            placeholder="seu usuário"
            autoComplete="username"
            disabled={isSubmitting}
          />
        </div>

        <label className="field-label" htmlFor="password">
          Senha
        </label>
        <div className="input-wrap">
          <LockKeyhole size={18} aria-hidden="true" />
          <input
            id="password"
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="sua senha"
            autoComplete="current-password"
            disabled={isSubmitting}
          />
        </div>

        {errorMessage && (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        )}

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Entrando...' : 'Entrar'}
          <ArrowRight size={18} aria-hidden="true" />
        </button>
      </form>
    </AuthShell>
  )
}
