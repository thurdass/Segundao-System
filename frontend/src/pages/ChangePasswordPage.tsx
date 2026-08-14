import { ArrowRight, KeyRound } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/client'
import { AuthShell } from '../components/AuthShell'
import { useAuth } from '../hooks/useAuth'

export function ChangePasswordPage() {
  const { changePassword, user } = useAuth()
  const navigate = useNavigate()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!user.mustChangePassword) {
    return <Navigate to="/dashboard" replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')

    if (!currentPassword || !newPassword || !confirmation) {
      setErrorMessage('Preencha todos os campos.')
      return
    }

    if (newPassword.length < 8) {
      setErrorMessage('A nova senha deve ter pelo menos 8 caracteres.')
      return
    }

    if (newPassword !== confirmation) {
      setErrorMessage('A confirmação não corresponde à nova senha.')
      return
    }

    setIsSubmitting(true)

    try {
      const updatedUser = await changePassword({ currentPassword, newPassword })

      if (updatedUser.mustChangePassword) {
        setErrorMessage('A senha ainda precisa ser atualizada. Tente novamente.')
        return
      }

      navigate('/dashboard', { replace: true })
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível alterar a senha.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthShell
      eyebrow="Primeiro acesso"
      title="Crie sua nova senha"
      description="Por segurança, troque a senha inicial antes de continuar."
    >
      <div className="notice-box">
        <KeyRound size={18} aria-hidden="true" />
        <span>A nova senha deve ter pelo menos 8 caracteres.</span>
      </div>

      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="current-password">
          Senha atual
        </label>
        <input
          id="current-password"
          name="currentPassword"
          type="password"
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
          autoComplete="current-password"
          disabled={isSubmitting}
        />

        <label className="field-label" htmlFor="new-password">
          Nova senha
        </label>
        <input
          id="new-password"
          name="newPassword"
          type="password"
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
          autoComplete="new-password"
          disabled={isSubmitting}
        />

        <label className="field-label" htmlFor="confirm-password">
          Confirmar nova senha
        </label>
        <input
          id="confirm-password"
          name="confirmation"
          type="password"
          value={confirmation}
          onChange={(event) => setConfirmation(event.target.value)}
          autoComplete="new-password"
          disabled={isSubmitting}
        />

        {errorMessage && (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        )}

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Salvando...' : 'Salvar nova senha'}
          <ArrowRight size={18} aria-hidden="true" />
        </button>
      </form>
    </AuthShell>
  )
}
