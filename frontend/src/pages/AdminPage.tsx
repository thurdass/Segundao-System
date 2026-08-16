import {
  CheckCircle2,
  CircleAlert,
  Eye,
  LayoutDashboard,
  LockKeyhole,
  Plus,
  UserRound,
  UsersRound,
  X as CloseIcon,
} from 'lucide-react'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { adminApi } from '../api/admin'
import { getApiErrorMessage } from '../api/client'
import { AdminAnnouncementsSection } from '../components/AdminAnnouncementsSection'
import { AdminSchoolSection } from '../components/AdminSchoolSection'
import { useAuth } from '../hooks/useAuth'
import type { AdminDashboard, AdminUserRequest, User } from '../types/api'
import { formatDateTime } from '../utils/date'

interface UserFormState {
  username: string
  displayName: string
  password: string
  classroomId: string
}

const initialForm: UserFormState = {
  username: '',
  displayName: '',
  password: '',
  classroomId: '',
}

function StatCard({ label, value, detail, icon }: {
  label: string
  value: number
  detail: string
  icon: 'users' | 'activities' | 'announcements'
}) {
  const Icon = icon === 'users' ? UsersRound : icon === 'activities' ? LayoutDashboard : CheckCircle2

  return (
    <article className="admin-stat-card">
      <div className="admin-stat-icon">
        <Icon size={19} aria-hidden="true" />
      </div>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  )
}

function UserForm({ form, isSubmitting, onCancel, onChange, onSubmit }: {
  form: UserFormState
  isSubmitting: boolean
  onCancel(): void
  onChange(field: keyof UserFormState, value: string): void
  onSubmit(event: FormEvent<HTMLFormElement>): void
}) {
  return (
    <section className="form-card" aria-labelledby="admin-user-form-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Nova conta</p>
          <h3 id="admin-user-form-title">Criar usuário para a turma</h3>
        </div>
        <button className="icon-button" type="button" aria-label="Fechar formulário" onClick={onCancel}>
          <CloseIcon size={18} aria-hidden="true" />
        </button>
      </div>
      <div className="admin-onboarding-note">
        <LockKeyhole size={17} aria-hidden="true" />
        <span>A senha informada será inicial. O aluno precisará trocá-la no primeiro acesso.</span>
      </div>
      <form className="data-form" onSubmit={onSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="admin-username">Usuário</label>
          <input
            id="admin-username"
            type="text"
            value={form.username}
            onChange={(event) => onChange('username', event.target.value)}
            placeholder="ex.: arthur"
            autoComplete="off"
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="admin-display-name">Nome de exibição</label>
          <input
            id="admin-display-name"
            type="text"
            value={form.displayName}
            onChange={(event) => onChange('displayName', event.target.value)}
            placeholder="Nome do aluno"
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="admin-initial-password">Senha inicial</label>
          <input
            id="admin-initial-password"
            type="password"
            value={form.password}
            onChange={(event) => onChange('password', event.target.value)}
            placeholder="mínimo de 8 caracteres"
            autoComplete="new-password"
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="admin-classroom-id">Código da turma</label>
          <input
            id="admin-classroom-id"
            type="number"
            min="1"
            value={form.classroomId}
            onChange={(event) => onChange('classroomId', event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-actions form-field-wide">
          <button className="secondary-button muted-button" type="button" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </button>
          <button className="primary-button page-primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Criando...' : 'Criar aluno'}
          </button>
        </div>
      </form>
    </section>
  )
}

export function AdminPage() {
  const { user } = useAuth()
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isFormOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState<UserFormState>({
    ...initialForm,
    classroomId: user?.classroomId ? String(user.classroomId) : '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [busyUserId, setBusyUserId] = useState<number | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [noticeMessage, setNoticeMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const fetchAdminData = useCallback(async () => {
    const [dashboardResult, usersResult] = await Promise.all([
      adminApi.dashboard(),
      adminApi.users(),
    ])

    return { dashboard: dashboardResult, users: usersResult }
  }, [])

  useEffect(() => {
    let active = true

    void fetchAdminData()
      .then((result) => {
        if (active) {
          setDashboard(result.dashboard)
          setUsers(result.users)
          setErrorMessage('')
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar a área administrativa.'))
        }
      })
      .finally(() => {
        if (active) {
          setIsLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [fetchAdminData])

  async function reloadAdminData() {
    setIsLoading(true)

    try {
      const result = await fetchAdminData()
      setDashboard(result.dashboard)
      setUsers(result.users)
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível atualizar os dados administrativos.'))
    } finally {
      setIsLoading(false)
    }
  }

  function updateForm(field: keyof UserFormState, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function openCreateForm() {
    setNoticeMessage('')
    setErrorMessage('')
    setForm({
      ...initialForm,
      classroomId: user?.classroomId ? String(user.classroomId) : '',
    })
    setFormOpen(true)
  }

  function closeForm() {
    setFormOpen(false)
    setForm({
      ...initialForm,
      classroomId: user?.classroomId ? String(user.classroomId) : '',
    })
  }

  async function handleCreateUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')
    setNoticeMessage('')

    if (!form.username.trim() || !form.displayName.trim() || !form.password || !form.classroomId) {
      setErrorMessage('Preencha todos os campos para criar a conta.')
      return
    }

    if (form.password.length < 8) {
      setErrorMessage('A senha inicial deve ter pelo menos 8 caracteres.')
      return
    }

    const payload: AdminUserRequest = {
      username: form.username.trim(),
      displayName: form.displayName.trim(),
      password: form.password,
      role: 'STUDENT',
      classroomId: Number(form.classroomId),
    }

    setIsSubmitting(true)

    try {
      await adminApi.createUser(payload)
      closeForm()
      setNoticeMessage('Usuário criado. Entregue a senha inicial ao aluno com segurança; ela não será exibida novamente.')
      await reloadAdminData()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível criar o usuário.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleToggleStatus(targetUser: User) {
    setBusyUserId(targetUser.id)
    setErrorMessage('')

    try {
      await adminApi.setUserStatus(targetUser.id, !targetUser.enabled)
      if (selectedUser?.id === targetUser.id) {
        setSelectedUser((current) => current ? { ...current, enabled: !targetUser.enabled } : current)
      }
      await reloadAdminData()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível atualizar o status da conta.'))
    } finally {
      setBusyUserId(null)
    }
  }

  async function handleShowDetails(targetUser: User) {
    if (selectedUser?.id === targetUser.id) {
      setSelectedUser(null)
      return
    }

    setDetailLoading(true)
    setErrorMessage('')

    try {
      setSelectedUser(await adminApi.user(targetUser.id))
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível consultar o usuário.'))
    } finally {
      setDetailLoading(false)
    }
  }

  return (
    <div className="data-page">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Acesso restrito · Administrador</p>
          <h2>Administração</h2>
          <p>Gerencie as contas e acompanhe o resumo atual da plataforma.</p>
        </div>
        <button className="primary-button page-primary-button" type="button" onClick={openCreateForm}>
          <Plus size={17} aria-hidden="true" />
          Criar aluno
        </button>
      </section>

      {noticeMessage && (
        <div className="inline-success" role="status">
          <CheckCircle2 size={18} aria-hidden="true" />
          <span>{noticeMessage}</span>
        </div>
      )}

      {errorMessage && (
        <div className="inline-error" role="alert">
          <CircleAlert size={18} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      )}

      {isFormOpen && (
        <UserForm
          form={form}
          isSubmitting={isSubmitting}
          onCancel={closeForm}
          onChange={updateForm}
          onSubmit={handleCreateUser}
        />
      )}

      {isLoading && !dashboard ? (
        <div className="data-list-skeleton" aria-label="Carregando administração">
          <div />
          <div />
        </div>
      ) : (
        <>
          {dashboard && (
            <section className="admin-stat-grid" aria-label="Resumo administrativo">
              <StatCard label="Usuários" value={dashboard.users} detail={`${dashboard.activeUsers} contas ativas`} icon="users" />
              <StatCard label="Atividades" value={dashboard.activities} detail="atividades ativas" icon="activities" />
              <StatCard label="Avisos" value={dashboard.announcements} detail="avisos ativos" icon="announcements" />
            </section>
          )}

          <section className="admin-users-section">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Contas cadastradas</p>
                <h3>Usuários</h3>
              </div>
              <span className="admin-count-label">{users.length} registros</span>
            </div>

            {users.length === 0 ? (
              <div className="content-card empty-state page-empty-state">
                <UsersRound size={24} aria-hidden="true" />
                <div>
                  <strong>Nenhum usuário cadastrado</strong>
                  <p>Crie a primeira conta usando o botão acima.</p>
                </div>
              </div>
            ) : (
              <div className="admin-user-list">
                {users.map((targetUser) => (
                  <article className="admin-user-row" key={targetUser.id}>
                    <span className="avatar avatar-small">{targetUser.displayName.slice(0, 1).toUpperCase()}</span>
                    <div className="admin-user-main">
                      <strong>{targetUser.displayName}</strong>
                      <span>@{targetUser.username} · {targetUser.role === 'ADMIN' ? 'Administrador' : 'Aluno'}</span>
                    </div>
                    <span className={`status-badge ${targetUser.enabled ? 'status-completed' : 'status-late'}`}>
                      {targetUser.enabled ? 'Ativa' : 'Desativada'}
                    </span>
                    <div className="admin-user-actions">
                      <button className="school-card-action" type="button" onClick={() => void handleShowDetails(targetUser)} disabled={detailLoading}>
                        <Eye size={15} aria-hidden="true" />
                        Detalhes
                      </button>
                      <button className="admin-status-button" type="button" onClick={() => void handleToggleStatus(targetUser)} disabled={busyUserId === targetUser.id}>
                        {busyUserId === targetUser.id ? 'Salvando...' : targetUser.enabled ? 'Desativar' : 'Reativar'}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          {selectedUser && (
            <section className="content-card admin-user-details" aria-labelledby="admin-user-details-title">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Consulta administrativa</p>
                  <h3 id="admin-user-details-title">Detalhes de {selectedUser.displayName}</h3>
                </div>
                <button className="icon-button" type="button" aria-label="Fechar detalhes" onClick={() => setSelectedUser(null)}>
                  <CloseIcon size={18} aria-hidden="true" />
                </button>
              </div>
              <dl className="admin-detail-grid">
                <div><dt>ID</dt><dd>{selectedUser.id}</dd></div>
                <div><dt>Username</dt><dd>@{selectedUser.username}</dd></div>
                <div><dt>Perfil</dt><dd>{selectedUser.role === 'ADMIN' ? 'Administrador' : 'Aluno'}</dd></div>
                <div><dt>Turma</dt><dd>Código {selectedUser.classroomId}</dd></div>
                <div><dt>Primeiro acesso</dt><dd>{selectedUser.mustChangePassword ? 'Troca pendente' : 'Concluído'}</dd></div>
                <div><dt>Criado em</dt><dd>{formatDateTime(selectedUser.createdAt)}</dd></div>
              </dl>
              <div className="admin-detail-safe-note">
                <UserRound size={16} aria-hidden="true" />
                <span>Senhas, hashes e tokens não fazem parte desta consulta.</span>
              </div>
            </section>
          )}

          {user?.classroomId && (
            <>
              <AdminAnnouncementsSection />
              <AdminSchoolSection classroomId={user.classroomId} />
            </>
          )}
        </>
      )}
    </div>
  )
}
