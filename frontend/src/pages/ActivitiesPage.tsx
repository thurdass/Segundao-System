import {
  CalendarClock,
  Check,
  CheckCircle2,
  CircleAlert,
  ClipboardList,
  Filter,
  Pencil,
  Plus,
  Trash2,
  X as CloseIcon,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { activitiesApi, type ActivityQuery } from '../api/activities'
import { getApiErrorMessage } from '../api/client'
import { schoolApi } from '../api/school'
import { useAuth } from '../hooks/useAuth'
import type { Activity, ActivityRequest, ActivityStatus, DeadlineMode, NextClass, Subject } from '../types/api'
import {
  formatDate,
  formatDateTime,
  formatDueDate,
  formatTime,
  getTodayDate,
  isPastDate,
} from '../utils/date'

interface ActivityFormState {
  title: string
  description: string
  subjectId: string
  deadlineMode: DeadlineMode
  dueDate: string
}

const initialForm: ActivityFormState = {
  title: '',
  description: '',
  subjectId: '',
  deadlineMode: 'CUSTOM_DATE',
  dueDate: getTodayDate(),
}

function statusFor(activity: Activity): { label: string; className: string } {
  if (activity.completed) {
    return { label: 'Concluída', className: 'status-completed' }
  }

  if (isPastDate(activity.dueDate)) {
    return { label: 'Atrasada', className: 'status-late' }
  }

  return { label: 'Pendente', className: 'status-pending' }
}

function ActivityForm({
  editingActivity,
  form,
  isSubmitting,
  nextClass,
  nextClassError,
  nextClassLoading,
  onCancel,
  onChange,
  onSubmit,
  subjects,
}: {
  editingActivity: Activity | null
  form: ActivityFormState
  isSubmitting: boolean
  nextClass: NextClass | null
  nextClassError: string
  nextClassLoading: boolean
  onCancel(): void
  onChange(field: keyof ActivityFormState, value: string): void
  onSubmit(event: FormEvent<HTMLFormElement>): void
  subjects: Subject[]
}) {
  return (
    <section className="form-card" aria-labelledby="activity-form-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">{editingActivity ? 'Atualização' : 'Nova atividade'}</p>
          <h3 id="activity-form-title">
            {editingActivity ? 'Editar atividade' : 'Compartilhe uma atividade'}
          </h3>
        </div>
        <button className="icon-button" type="button" aria-label="Fechar formulário" onClick={onCancel}>
          <CloseIcon size={18} aria-hidden="true" />
        </button>
      </div>

      <form className="data-form" onSubmit={onSubmit} noValidate>
        <div className="form-field form-field-wide">
          <label htmlFor="activity-title">Título</label>
          <input
            id="activity-title"
            type="text"
            value={form.title}
            onChange={(event) => onChange('title', event.target.value)}
            placeholder="Ex.: Lista de exercícios"
            disabled={isSubmitting}
            required
          />
        </div>

        <div className="form-field form-field-wide">
          <label htmlFor="activity-description">Descrição <span>(opcional)</span></label>
          <textarea
            id="activity-description"
            value={form.description}
            onChange={(event) => onChange('description', event.target.value)}
            placeholder="Inclua instruções ou observações para a turma"
            rows={3}
            disabled={isSubmitting}
          />
        </div>

        <div className="form-field">
          <label htmlFor="activity-subject">Disciplina</label>
          <select
            id="activity-subject"
            value={form.subjectId}
            onChange={(event) => onChange('subjectId', event.target.value)}
            disabled={isSubmitting}
            required
          >
            <option value="">Selecione uma disciplina</option>
            {subjects.map((subject) => (
              <option key={subject.id} value={subject.id}>
                {subject.name}
              </option>
            ))}
          </select>
        </div>

        <div className="form-field">
          <label htmlFor="activity-deadline-mode">Prazo</label>
          <select
            id="activity-deadline-mode"
            value={form.deadlineMode}
            onChange={(event) => onChange('deadlineMode', event.target.value)}
            disabled={isSubmitting}
          >
            <option value="CUSTOM_DATE">Escolher uma data</option>
            <option value="NEXT_CLASS">Próxima aula</option>
          </select>
        </div>

        {form.deadlineMode === 'CUSTOM_DATE' ? (
          <div className="form-field">
            <label htmlFor="activity-due-date">Data de entrega</label>
            <input
              id="activity-due-date"
              type="date"
              min={getTodayDate()}
              value={form.dueDate}
              onChange={(event) => onChange('dueDate', event.target.value)}
              disabled={isSubmitting}
              required
            />
          </div>
        ) : (
          <div className="next-class-preview" aria-live="polite">
            <CalendarClock size={18} aria-hidden="true" />
            {nextClassLoading && <span>Consultando a próxima aula...</span>}
            {!nextClassLoading && nextClass && (
              <span>
                Prazo calculado: {formatDate(nextClass.nextClassDate)} às {formatTime(nextClass.startTime)}
              </span>
            )}
            {!nextClassLoading && !nextClass && (
              <span>{nextClassError || 'Selecione uma disciplina com horário cadastrado.'}</span>
            )}
          </div>
        )}

        <div className="form-actions form-field-wide">
          <button className="secondary-button muted-button" type="button" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </button>
          <button className="primary-button page-primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : editingActivity ? 'Salvar alterações' : 'Publicar atividade'}
          </button>
        </div>
      </form>
    </section>
  )
}

export function ActivitiesPage() {
  const { user } = useAuth()
  const [activities, setActivities] = useState<Activity[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [statusFilter, setStatusFilter] = useState<'all' | ActivityStatus>('all')
  const [subjectFilter, setSubjectFilter] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [isFormOpen, setFormOpen] = useState(false)
  const [editingActivity, setEditingActivity] = useState<Activity | null>(null)
  const [form, setForm] = useState<ActivityFormState>(initialForm)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [nextClass, setNextClass] = useState<NextClass | null>(null)
  const [nextClassError, setNextClassError] = useState('')
  const [nextClassLoading, setNextClassLoading] = useState(false)

  const subjectNames = useMemo(() => {
    return new Map(subjects.map((subject) => [subject.id, subject.name]))
  }, [subjects])

  const fetchActivities = useCallback(async (): Promise<Activity[]> => {
    const query: ActivityQuery = {}

    if (statusFilter !== 'all') {
      query.status = statusFilter
    }

    if (subjectFilter) {
      query.subjectId = Number(subjectFilter)
    }

    return activitiesApi.list(query)
  }, [statusFilter, subjectFilter])

  useEffect(() => {
    let active = true

    void fetchActivities()
      .then((result) => {
        if (active) {
          setActivities(result)
          setErrorMessage('')
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar as atividades.'))
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
  }, [fetchActivities])

  const loadActivities = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      setActivities(await fetchActivities())
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar as atividades.'))
    } finally {
      setIsLoading(false)
    }
  }, [fetchActivities])

  useEffect(() => {
    let active = true

    async function loadSubjects() {
      if (!user) {
        return
      }

      try {
        const result = await schoolApi.subjects(user.classroomId)

        if (active) {
          setSubjects(result)
        }
      } catch (error: unknown) {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar as disciplinas.'))
        }
      }
    }

    void loadSubjects()

    return () => {
      active = false
    }
  }, [user])

  useEffect(() => {
    let active = true

    async function loadNextClass() {
      if (form.deadlineMode !== 'NEXT_CLASS' || !form.subjectId) {
        setNextClass(null)
        setNextClassError('')
        setNextClassLoading(false)
        return
      }

      setNextClassLoading(true)
      setNextClassError('')

      try {
        const result = await schoolApi.nextClass(Number(form.subjectId))

        if (active) {
          setNextClass(result)
        }
      } catch (error: unknown) {
        if (active) {
          setNextClass(null)
          setNextClassError(getApiErrorMessage(error, 'Não há próxima aula cadastrada para esta disciplina.'))
        }
      } finally {
        if (active) {
          setNextClassLoading(false)
        }
      }
    }

    void loadNextClass()

    return () => {
      active = false
    }
  }, [form.deadlineMode, form.subjectId])

  function updateForm(field: keyof ActivityFormState, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function openCreateForm() {
    setEditingActivity(null)
    setForm({ ...initialForm, dueDate: getTodayDate() })
    setFormOpen(true)
  }

  function openEditForm(activity: Activity) {
    setEditingActivity(activity)
    setForm({
      title: activity.title,
      description: activity.description ?? '',
      subjectId: String(activity.subjectId),
      deadlineMode: 'CUSTOM_DATE',
      dueDate: activity.dueDate,
    })
    setFormOpen(true)
  }

  function closeForm() {
    setFormOpen(false)
    setEditingActivity(null)
    setForm({ ...initialForm, dueDate: getTodayDate() })
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')

    if (!form.title.trim() || !form.subjectId) {
      setErrorMessage('Informe o título e selecione uma disciplina.')
      return
    }

    if (form.deadlineMode === 'CUSTOM_DATE' && !form.dueDate) {
      setErrorMessage('Informe a data de entrega.')
      return
    }

    const payload: ActivityRequest = {
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      subjectId: Number(form.subjectId),
      deadlineMode: form.deadlineMode,
      dueDate: form.deadlineMode === 'CUSTOM_DATE' ? form.dueDate : undefined,
    }

    setIsSubmitting(true)

    try {
      if (editingActivity) {
        await activitiesApi.update(editingActivity.id, payload)
      } else {
        await activitiesApi.create(payload)
      }

      closeForm()
      await loadActivities()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível salvar a atividade.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleCompletion(activity: Activity) {
    setErrorMessage('')

    try {
      if (activity.completed) {
        await activitiesApi.uncomplete(activity.id)
      } else {
        await activitiesApi.complete(activity.id)
      }

      await loadActivities()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível atualizar a conclusão.'))
    }
  }

  async function handleDelete(activity: Activity) {
    if (!window.confirm(`Excluir a atividade “${activity.title}”?`)) {
      return
    }

    setErrorMessage('')

    try {
      await activitiesApi.remove(activity.id)
      await loadActivities()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível excluir a atividade.'))
    }
  }

  function canManage(activity: Activity): boolean {
    return user?.role === 'ADMIN' || user?.username === activity.createdBy
  }

  return (
    <div className="data-page">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Rotina compartilhada</p>
          <h2>Atividades</h2>
          <p>Organize os prazos da turma e acompanhe suas próprias conclusões.</p>
        </div>
        <button className="primary-button page-primary-button" type="button" onClick={openCreateForm}>
          <Plus size={17} aria-hidden="true" />
          Nova atividade
        </button>
      </section>

      <section className="filters-toolbar" aria-label="Filtros de atividades">
        <div className="filter-title">
          <Filter size={17} aria-hidden="true" />
          <span>Filtrar lista</span>
        </div>
        <label>
          Status
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as 'all' | ActivityStatus)}>
            <option value="all">Todas</option>
            <option value="pending">Pendentes</option>
            <option value="completed">Concluídas</option>
          </select>
        </label>
        <label>
          Disciplina
          <select value={subjectFilter} onChange={(event) => setSubjectFilter(event.target.value)}>
            <option value="">Todas</option>
            {subjects.map((subject) => (
              <option key={subject.id} value={subject.id}>
                {subject.name}
              </option>
            ))}
          </select>
        </label>
      </section>

      {isFormOpen && (
        <ActivityForm
          editingActivity={editingActivity}
          form={form}
          isSubmitting={isSubmitting}
          nextClass={nextClass}
          nextClassError={nextClassError}
          nextClassLoading={nextClassLoading}
          onCancel={closeForm}
          onChange={updateForm}
          onSubmit={handleSubmit}
          subjects={subjects}
        />
      )}

      {errorMessage && (
        <div className="inline-error" role="alert">
          <CircleAlert size={18} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      )}

      {isLoading ? (
        <div className="data-list-skeleton" aria-label="Carregando atividades">
          <div />
          <div />
          <div />
        </div>
      ) : activities.length === 0 ? (
        <div className="content-card empty-state page-empty-state">
          <ClipboardList size={24} aria-hidden="true" />
          <div>
            <strong>Nenhuma atividade encontrada</strong>
            <p>A turma ainda não possui atividades para este filtro.</p>
          </div>
        </div>
      ) : (
        <section className="activity-list" aria-label="Lista de atividades">
          {activities.map((activity) => {
            const status = statusFor(activity)
            const managed = canManage(activity)

            return (
              <article className={`activity-card ${activity.completed ? 'is-completed' : ''}`} key={activity.id}>
                <div className="activity-card-main">
                  <div className="activity-card-topline">
                    <span className={`status-badge ${status.className}`}>{status.label}</span>
                    <span className="activity-subject">
                      {subjectNames.get(activity.subjectId) ?? `Disciplina #${activity.subjectId}`}
                    </span>
                  </div>
                  <h3>{activity.title}</h3>
                  {activity.description && <p>{activity.description}</p>}
                  <div className="activity-meta">
                    <span>Entrega: {formatDueDate(activity.dueDate)}</span>
                    <span>Por @{activity.createdBy}</span>
                    {activity.completedAt && <span>Concluída em {formatDateTime(activity.completedAt)}</span>}
                  </div>
                </div>
                <div className="activity-card-actions">
                  <button
                    className={`completion-button ${activity.completed ? 'is-completed' : ''}`}
                    type="button"
                    onClick={() => void handleCompletion(activity)}
                  >
                    {activity.completed ? <CheckCircle2 size={17} aria-hidden="true" /> : <Check size={17} aria-hidden="true" />}
                    {activity.completed ? 'Desfazer' : 'Concluir'}
                  </button>
                  {managed && (
                    <div className="row-actions">
                      <button className="icon-button small-icon-button" type="button" aria-label={`Editar ${activity.title}`} onClick={() => openEditForm(activity)}>
                        <Pencil size={16} aria-hidden="true" />
                      </button>
                      <button className="icon-button small-icon-button danger-icon" type="button" aria-label={`Excluir ${activity.title}`} onClick={() => void handleDelete(activity)}>
                        <Trash2 size={16} aria-hidden="true" />
                      </button>
                    </div>
                  )}
                </div>
              </article>
            )
          })}
        </section>
      )}
    </div>
  )
}
