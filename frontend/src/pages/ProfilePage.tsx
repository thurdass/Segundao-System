import { CircleAlert, KeyRound, ShieldCheck, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import type { User } from '../types/api'
import { formatDateTime } from '../utils/date'

export function ProfilePage() {
  const { refreshUser, user } = useAuth()
  const [profile, setProfile] = useState<User | null>(user)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let active = true

    async function loadProfile() {
      try {
        const result = await refreshUser()

        if (active) {
          setProfile(result)
        }
      } catch (error: unknown) {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível atualizar o perfil.'))
        }
      }
    }

    void loadProfile()

    return () => {
      active = false
    }
  }, [refreshUser])

  if (!profile) {
    return null
  }

  return (
    <div className="data-page">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Sua conta</p>
          <h2>Perfil</h2>
          <p>Confira os dados da conta que está utilizando o sistema.</p>
        </div>
      </section>

      {errorMessage && (
        <div className="inline-error" role="alert">
          <CircleAlert size={18} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      )}

      <section className="profile-grid">
        <article className="profile-hero">
          <div className="profile-avatar">
            <UserRound size={34} aria-hidden="true" />
          </div>
          <p className="eyebrow">Conta Segundão</p>
          <h3>{profile.displayName}</h3>
          <span>@{profile.username}</span>
        </article>

        <article className="content-card profile-details">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Informações</p>
              <h3>Dados da conta</h3>
            </div>
            <ShieldCheck size={20} aria-hidden="true" />
          </div>
          <dl className="profile-fields">
            <div>
              <dt>Nome de exibição</dt>
              <dd>{profile.displayName}</dd>
            </div>
            <div>
              <dt>Perfil de acesso</dt>
              <dd>{profile.role === 'ADMIN' ? 'Administrador' : 'Aluno'}</dd>
            </div>
            <div>
              <dt>Turma</dt>
              <dd>2º Informática A · código {profile.classroomId}</dd>
            </div>
            <div>
              <dt>Status da conta</dt>
              <dd>{profile.enabled ? 'Ativa' : 'Desativada'}</dd>
            </div>
            <div>
              <dt>Criada em</dt>
              <dd>{formatDateTime(profile.createdAt)}</dd>
            </div>
          </dl>
          <div className="profile-security-note">
            <KeyRound size={17} aria-hidden="true" />
            <span>A troca de senha é feita pela tela de primeiro acesso e não expõe sua senha no sistema.</span>
          </div>
        </article>
      </section>
    </div>
  )
}
