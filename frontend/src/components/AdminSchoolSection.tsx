import { BookOpen, CalendarPlus, CircleAlert, Clock3, GraduationCap, Pencil, Plus, Trash2, X } from 'lucide-react'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../api/client'
import { schoolApi } from '../api/school'
import type {
  DayOfWeek,
  Schedule,
  ScheduleRequest,
  Subject,
  SubjectRequest,
  Teacher,
  TeacherRequest,
} from '../types/api'

interface AdminSchoolSectionProps {
  classroomId: number
}

interface SubjectFormState {
  name: string
  shortName: string
}

interface TeacherFormState {
  name: string
  email: string
  subjectIds: number[]
}

interface ScheduleFormState {
  subjectId: string
  teacherId: string
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
}

type ActiveForm = 'subject' | 'teacher' | 'schedule' | null

const weekdays: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
]

const dayLabels: Record<DayOfWeek, string> = {
  MONDAY: 'Segunda-feira',
  TUESDAY: 'Terça-feira',
  WEDNESDAY: 'Quarta-feira',
  THURSDAY: 'Quinta-feira',
  FRIDAY: 'Sexta-feira',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
}

const gradeTimeReference = ['07:30', '08:20', '09:10', '10:20', '11:00', '13:30', '14:20', '15:30', '16:20', '17:00']

const initialSubjectForm: SubjectFormState = {
  name: '',
  shortName: '',
}

const initialTeacherForm: TeacherFormState = {
  name: '',
  email: '',
  subjectIds: [],
}

const initialScheduleForm: ScheduleFormState = {
  subjectId: '',
  teacherId: '',
  dayOfWeek: 'MONDAY',
  startTime: '07:30',
  endTime: '08:20',
}

function SubjectForm({
  classroomId,
  form,
  isSubmitting,
  onCancel,
  onChange,
  onSubmit,
}: {
  classroomId: number
  form: SubjectFormState
  isSubmitting: boolean
  onCancel(): void
  onChange(field: keyof SubjectFormState, value: string): void
  onSubmit(event: FormEvent<HTMLFormElement>): void
}) {
  return (
    <section className="form-card admin-inline-form" aria-labelledby="admin-subject-form-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Catálogo escolar</p>
          <h3 id="admin-subject-form-title">Nova disciplina</h3>
        </div>
        <button className="icon-button" type="button" aria-label="Fechar formulário" onClick={onCancel}>
          <X size={18} aria-hidden="true" />
        </button>
      </div>
      <form className="data-form" onSubmit={onSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="admin-subject-name">Nome da disciplina</label>
          <input
            id="admin-subject-name"
            type="text"
            value={form.name}
            onChange={(event) => onChange('name', event.target.value)}
            placeholder="Ex.: Banco de Dados"
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="admin-subject-short-name">Abreviação</label>
          <input
            id="admin-subject-short-name"
            type="text"
            value={form.shortName}
            onChange={(event) => onChange('shortName', event.target.value)}
            placeholder="Ex.: B DADO"
            disabled={isSubmitting}
          />
        </div>
        <p className="form-help form-field-wide">A disciplina será cadastrada na turma do administrador (código {classroomId}).</p>
        <div className="form-actions form-field-wide">
          <button className="secondary-button muted-button" type="button" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </button>
          <button className="primary-button page-primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : 'Adicionar disciplina'}
          </button>
        </div>
      </form>
    </section>
  )
}

function TeacherForm({
  editingTeacher,
  form,
  subjects,
  isSubmitting,
  onCancel,
  onChange,
  onToggleSubject,
  onSubmit,
}: {
  editingTeacher: Teacher | null
  form: TeacherFormState
  subjects: Subject[]
  isSubmitting: boolean
  onCancel(): void
  onChange(field: 'name' | 'email', value: string): void
  onToggleSubject(subjectId: number, checked: boolean): void
  onSubmit(event: FormEvent<HTMLFormElement>): void
}) {
  return (
    <section className="form-card admin-inline-form" aria-labelledby="admin-teacher-form-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Equipe pedagógica</p>
          <h3 id="admin-teacher-form-title">{editingTeacher ? 'Editar professor' : 'Novo professor'}</h3>
        </div>
        <button className="icon-button" type="button" aria-label="Fechar formulário" onClick={onCancel}>
          <X size={18} aria-hidden="true" />
        </button>
      </div>
      <form className="data-form" onSubmit={onSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="admin-teacher-name">Nome</label>
          <input
            id="admin-teacher-name"
            type="text"
            value={form.name}
            onChange={(event) => onChange('name', event.target.value)}
            placeholder="Nome do professor"
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="admin-teacher-email">E-mail</label>
          <input
            id="admin-teacher-email"
            type="email"
            value={form.email}
            onChange={(event) => onChange('email', event.target.value)}
            placeholder="professor@escola.com"
            disabled={isSubmitting}
          />
        </div>
        <fieldset className="form-field form-field-wide subject-checkbox-fieldset">
          <legend>Disciplinas relacionadas</legend>
          {subjects.length === 0 ? (
            <p className="form-help">Cadastre uma disciplina antes de relacioná-la ao professor.</p>
          ) : (
            <div className="subject-checkbox-grid">
              {subjects.map((subject) => (
                <label className="checkbox-field" key={subject.id}>
                  <input
                    type="checkbox"
                    checked={form.subjectIds.includes(subject.id)}
                    onChange={(event) => onToggleSubject(subject.id, event.target.checked)}
                    disabled={isSubmitting}
                  />
                  <span>{subject.shortName ? `${subject.shortName} · ${subject.name}` : subject.name}</span>
                </label>
              ))}
            </div>
          )}
        </fieldset>
        <div className="form-actions form-field-wide">
          <button className="secondary-button muted-button" type="button" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </button>
          <button className="primary-button page-primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : editingTeacher ? 'Salvar professor' : 'Adicionar professor'}
          </button>
        </div>
      </form>
    </section>
  )
}

function ScheduleForm({
  form,
  subjects,
  teachers,
  classroomId,
  isSubmitting,
  onCancel,
  onChange,
  onSubmit,
}: {
  form: ScheduleFormState
  subjects: Subject[]
  teachers: Teacher[]
  classroomId: number
  isSubmitting: boolean
  onCancel(): void
  onChange(field: keyof ScheduleFormState, value: string): void
  onSubmit(event: FormEvent<HTMLFormElement>): void
}) {
  return (
    <section className="form-card admin-inline-form" aria-labelledby="admin-schedule-form-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Grade semanal</p>
          <h3 id="admin-schedule-form-title">Novo horário</h3>
        </div>
        <button className="icon-button" type="button" aria-label="Fechar formulário" onClick={onCancel}>
          <X size={18} aria-hidden="true" />
        </button>
      </div>
      <form className="data-form" onSubmit={onSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="admin-schedule-subject">Disciplina</label>
          <select
            id="admin-schedule-subject"
            value={form.subjectId}
            onChange={(event) => onChange('subjectId', event.target.value)}
            disabled={isSubmitting || subjects.length === 0}
            required
          >
            <option value="">Selecione uma disciplina</option>
            {subjects.map((subject) => (
              <option key={subject.id} value={subject.id}>
                {subject.shortName ? `${subject.shortName} · ${subject.name}` : subject.name}
              </option>
            ))}
          </select>
        </div>
        <div className="form-field">
          <label htmlFor="admin-schedule-teacher">Professor</label>
          <select
            id="admin-schedule-teacher"
            value={form.teacherId}
            onChange={(event) => onChange('teacherId', event.target.value)}
            disabled={isSubmitting}
          >
            <option value="">Sem professor definido</option>
            {teachers.map((teacher) => (
              <option key={teacher.id} value={teacher.id}>{teacher.name}</option>
            ))}
          </select>
        </div>
        <div className="form-field">
          <label htmlFor="admin-schedule-day">Dia da semana</label>
          <select
            id="admin-schedule-day"
            value={form.dayOfWeek}
            onChange={(event) => onChange('dayOfWeek', event.target.value)}
            disabled={isSubmitting}
            required
          >
            {weekdays.map((day) => <option key={day} value={day}>{dayLabels[day]}</option>)}
          </select>
        </div>
        <div className="form-field">
          <label htmlFor="admin-schedule-start">Início</label>
          <input
            id="admin-schedule-start"
            type="time"
            value={form.startTime}
            onChange={(event) => onChange('startTime', event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="admin-schedule-end">Fim</label>
          <input
            id="admin-schedule-end"
            type="time"
            value={form.endTime}
            onChange={(event) => onChange('endTime', event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>
        <p className="form-help form-field-wide">
          Turma: código {classroomId}. Horários observados na grade: {gradeTimeReference.join(' · ')}.
        </p>
        <div className="form-actions form-field-wide">
          <button className="secondary-button muted-button" type="button" onClick={onCancel} disabled={isSubmitting}>
            Cancelar
          </button>
          <button className="primary-button page-primary-button" type="submit" disabled={isSubmitting || subjects.length === 0}>
            {isSubmitting ? 'Salvando...' : 'Adicionar horário'}
          </button>
        </div>
      </form>
    </section>
  )
}

export function AdminSchoolSection({ classroomId }: AdminSchoolSectionProps) {
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [teachers, setTeachers] = useState<Teacher[]>([])
  const [schedules, setSchedules] = useState<Schedule[]>([])
  const [subjectForm, setSubjectForm] = useState<SubjectFormState>(initialSubjectForm)
  const [teacherForm, setTeacherForm] = useState<TeacherFormState>(initialTeacherForm)
  const [scheduleForm, setScheduleForm] = useState<ScheduleFormState>(initialScheduleForm)
  const [editingTeacher, setEditingTeacher] = useState<Teacher | null>(null)
  const [activeForm, setActiveForm] = useState<ActiveForm>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [busyScheduleId, setBusyScheduleId] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [noticeMessage, setNoticeMessage] = useState('')

  const fetchCatalog = useCallback(async () => {
    const [subjectResult, teacherResult, scheduleResult] = await Promise.all([
      schoolApi.subjects(classroomId),
      schoolApi.teachers(),
      schoolApi.schedules(classroomId),
    ])

    return {
      subjects: subjectResult,
      teachers: teacherResult,
      schedules: scheduleResult,
    }
  }, [classroomId])

  const loadCatalog = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const result = await fetchCatalog()
      setSubjects(result.subjects)
      setTeachers(result.teachers)
      setSchedules(result.schedules)
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar o catálogo escolar.'))
    } finally {
      setIsLoading(false)
    }
  }, [fetchCatalog])

  useEffect(() => {
    let active = true

    void fetchCatalog()
      .then((result) => {
        if (active) {
          setSubjects(result.subjects)
          setTeachers(result.teachers)
          setSchedules(result.schedules)
          setErrorMessage('')
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar o catálogo escolar.'))
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
  }, [fetchCatalog])

  function closeForm() {
    setActiveForm(null)
    setEditingTeacher(null)
    setSubjectForm(initialSubjectForm)
    setTeacherForm(initialTeacherForm)
    setScheduleForm(initialScheduleForm)
  }

  function openSubjectForm() {
    setNoticeMessage('')
    setErrorMessage('')
    setActiveForm('subject')
    setSubjectForm(initialSubjectForm)
  }

  function openTeacherForm() {
    setNoticeMessage('')
    setErrorMessage('')
    setEditingTeacher(null)
    setActiveForm('teacher')
    setTeacherForm(initialTeacherForm)
  }

  function openScheduleForm() {
    setNoticeMessage('')
    setErrorMessage('')
    setActiveForm('schedule')
    setScheduleForm({
      ...initialScheduleForm,
      subjectId: subjects[0] ? String(subjects[0].id) : '',
    })
  }

  async function openEditTeacher(teacher: Teacher) {
    setNoticeMessage('')
    setErrorMessage('')
    setEditingTeacher(teacher)
    setActiveForm('teacher')
    setTeacherForm({
      name: teacher.name,
      email: teacher.email ?? '',
      subjectIds: [],
    })

    try {
      const relatedSubjects = await schoolApi.teacherSubjects(teacher.id)
      setTeacherForm((current) => ({
        ...current,
        subjectIds: relatedSubjects.map((subject) => subject.id),
      }))
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar as disciplinas do professor.'))
    }
  }

  function updateSubjectForm(field: keyof SubjectFormState, value: string) {
    setSubjectForm((current) => ({ ...current, [field]: value }))
  }

  function updateTeacherForm(field: 'name' | 'email', value: string) {
    setTeacherForm((current) => ({ ...current, [field]: value }))
  }

  function toggleTeacherSubject(subjectId: number, checked: boolean) {
    setTeacherForm((current) => ({
      ...current,
      subjectIds: checked
        ? [...current.subjectIds, subjectId]
        : current.subjectIds.filter((id) => id !== subjectId),
    }))
  }

  function updateScheduleForm(field: keyof ScheduleFormState, value: string) {
    setScheduleForm((current) => ({ ...current, [field]: value }))
  }

  async function handleSubjectSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')
    setNoticeMessage('')

    if (!subjectForm.name.trim()) {
      setErrorMessage('Informe o nome da disciplina.')
      return
    }

    const payload: SubjectRequest = {
      name: subjectForm.name.trim(),
      shortName: subjectForm.shortName.trim() || undefined,
      classroomId,
    }

    setIsSubmitting(true)

    try {
      await schoolApi.createSubject(payload)
      closeForm()
      setNoticeMessage('Disciplina adicionada.')
      await loadCatalog()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível adicionar a disciplina.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleTeacherSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')
    setNoticeMessage('')

    if (!teacherForm.name.trim()) {
      setErrorMessage('Informe o nome do professor.')
      return
    }

    const payload: TeacherRequest = {
      name: teacherForm.name.trim(),
      email: teacherForm.email.trim() || undefined,
      subjectIds: teacherForm.subjectIds,
    }
    const wasEditing = Boolean(editingTeacher)

    setIsSubmitting(true)

    try {
      if (editingTeacher) {
        await schoolApi.updateTeacher(editingTeacher.id, payload)
      } else {
        await schoolApi.createTeacher(payload)
      }

      closeForm()
      setNoticeMessage(wasEditing ? 'Professor atualizado.' : 'Professor adicionado.')
      await loadCatalog()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível salvar o professor.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleScheduleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage('')
    setNoticeMessage('')

    if (!scheduleForm.subjectId || !scheduleForm.startTime || !scheduleForm.endTime) {
      setErrorMessage('Selecione a disciplina e informe os dois horários.')
      return
    }

    if (scheduleForm.startTime >= scheduleForm.endTime) {
      setErrorMessage('O horário final deve ser posterior ao horário inicial.')
      return
    }

    const payload: ScheduleRequest = {
      classroomId,
      subjectId: Number(scheduleForm.subjectId),
      teacherId: scheduleForm.teacherId ? Number(scheduleForm.teacherId) : null,
      dayOfWeek: scheduleForm.dayOfWeek,
      startTime: scheduleForm.startTime,
      endTime: scheduleForm.endTime,
    }

    setIsSubmitting(true)

    try {
      await schoolApi.createSchedule(payload)
      closeForm()
      setNoticeMessage('Horário adicionado à grade.')
      await loadCatalog()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível adicionar o horário.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDeleteSchedule(schedule: Schedule) {
    const subject = subjects.find((item) => item.id === schedule.subjectId)
    const subjectName = subject?.name ?? 'este horário'

    if (!window.confirm(`Remover ${subjectName} de ${dayLabels[schedule.dayOfWeek]} às ${schedule.startTime}?`)) {
      return
    }

    setBusyScheduleId(schedule.id)
    setErrorMessage('')
    setNoticeMessage('')

    try {
      await schoolApi.removeSchedule(schedule.id)
      setNoticeMessage('Horário removido.')
      await loadCatalog()
    } catch (error: unknown) {
      setErrorMessage(getApiErrorMessage(error, 'Não foi possível remover o horário.'))
    } finally {
      setBusyScheduleId(null)
    }
  }

  return (
    <section className="admin-management-section" aria-labelledby="admin-school-title">
      <div className="admin-management-heading">
        <div>
          <p className="eyebrow">Configuração da turma</p>
          <h3 id="admin-school-title">Catálogo escolar e horários</h3>
          <p>Cadastre a grade real do 2º Informática A usando a imagem como referência.</p>
        </div>
        <div className="admin-management-actions">
          <button className="secondary-button" type="button" onClick={openSubjectForm}>
            <BookOpen size={16} aria-hidden="true" />
            Disciplina
          </button>
          <button className="secondary-button" type="button" onClick={openTeacherForm}>
            <GraduationCap size={16} aria-hidden="true" />
            Professor
          </button>
          <button className="secondary-button" type="button" onClick={openScheduleForm} disabled={subjects.length === 0}>
            <CalendarPlus size={16} aria-hidden="true" />
            Horário
          </button>
        </div>
      </div>

      {noticeMessage && <div className="inline-success" role="status">{noticeMessage}</div>}
      {errorMessage && (
        <div className="inline-error" role="alert">
          <CircleAlert size={17} aria-hidden="true" />
          <span>{errorMessage}</span>
        </div>
      )}

      {activeForm === 'subject' && (
        <SubjectForm
          classroomId={classroomId}
          form={subjectForm}
          isSubmitting={isSubmitting}
          onCancel={closeForm}
          onChange={updateSubjectForm}
          onSubmit={handleSubjectSubmit}
        />
      )}
      {activeForm === 'teacher' && (
        <TeacherForm
          editingTeacher={editingTeacher}
          form={teacherForm}
          subjects={subjects}
          isSubmitting={isSubmitting}
          onCancel={closeForm}
          onChange={updateTeacherForm}
          onToggleSubject={toggleTeacherSubject}
          onSubmit={handleTeacherSubmit}
        />
      )}
      {activeForm === 'schedule' && (
        <ScheduleForm
          form={scheduleForm}
          subjects={subjects}
          teachers={teachers}
          classroomId={classroomId}
          isSubmitting={isSubmitting}
          onCancel={closeForm}
          onChange={updateScheduleForm}
          onSubmit={handleScheduleSubmit}
        />
      )}

      {isLoading ? (
        <div className="data-list-skeleton" aria-label="Carregando catálogo escolar">
          <div />
          <div />
          <div />
        </div>
      ) : (
        <div className="admin-catalog-grid">
          <article className="admin-catalog-card">
            <div className="admin-catalog-card-heading">
              <div>
                <span className="admin-catalog-icon"><BookOpen size={17} aria-hidden="true" /></span>
                <h4>Disciplinas</h4>
              </div>
              <span>{subjects.length}</span>
            </div>
            {subjects.length === 0 ? (
              <p className="admin-catalog-empty">Nenhuma disciplina cadastrada.</p>
            ) : (
              <ul className="admin-catalog-list">
                {subjects.map((subject) => (
                  <li key={subject.id}>
                    <div>
                      <strong>{subject.name}</strong>
                      <span>{subject.shortName || 'Sem abreviação'}</span>
                    </div>
                    <small>#{subject.id}</small>
                  </li>
                ))}
              </ul>
            )}
            <button className="catalog-add-button" type="button" onClick={openSubjectForm}>
              <Plus size={15} aria-hidden="true" />
              Adicionar disciplina
            </button>
          </article>

          <article className="admin-catalog-card">
            <div className="admin-catalog-card-heading">
              <div>
                <span className="admin-catalog-icon"><GraduationCap size={17} aria-hidden="true" /></span>
                <h4>Professores</h4>
              </div>
              <span>{teachers.length}</span>
            </div>
            {teachers.length === 0 ? (
              <p className="admin-catalog-empty">Nenhum professor cadastrado.</p>
            ) : (
              <ul className="admin-catalog-list">
                {teachers.map((teacher) => (
                  <li key={teacher.id}>
                    <div>
                      <strong>{teacher.name}</strong>
                      <span>{teacher.email || 'Sem e-mail informado'}</span>
                    </div>
                    <button className="small-icon-button" type="button" aria-label={`Editar ${teacher.name}`} onClick={() => void openEditTeacher(teacher)}>
                      <Pencil size={15} aria-hidden="true" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <button className="catalog-add-button" type="button" onClick={openTeacherForm}>
              <Plus size={15} aria-hidden="true" />
              Adicionar professor
            </button>
          </article>

          <article className="admin-catalog-card admin-catalog-card-wide">
            <div className="admin-catalog-card-heading">
              <div>
                <span className="admin-catalog-icon"><Clock3 size={17} aria-hidden="true" /></span>
                <h4>Horários cadastrados</h4>
              </div>
              <span>{schedules.length}</span>
            </div>
            {schedules.length === 0 ? (
              <p className="admin-catalog-empty">Nenhum horário cadastrado.</p>
            ) : (
              <ul className="admin-schedule-list">
                {schedules.map((schedule) => {
                  const subject = subjects.find((item) => item.id === schedule.subjectId)
                  const teacher = teachers.find((item) => item.id === schedule.teacherId)

                  return (
                    <li key={schedule.id}>
                      <div className="admin-schedule-time">
                        <strong>{schedule.startTime}</strong>
                        <span>{schedule.endTime}</span>
                      </div>
                      <div className="admin-schedule-main">
                        <strong>{subject?.name ?? `Disciplina #${schedule.subjectId}`}</strong>
                        <span>{dayLabels[schedule.dayOfWeek]} · {teacher?.name ?? 'Professor não definido'}</span>
                      </div>
                      <button
                        className="small-icon-button danger-icon"
                        type="button"
                        aria-label={`Remover horário de ${subject?.name ?? 'disciplina'}`}
                        onClick={() => void handleDeleteSchedule(schedule)}
                        disabled={busyScheduleId === schedule.id}
                      >
                        <Trash2 size={15} aria-hidden="true" />
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}
            <button className="catalog-add-button" type="button" onClick={openScheduleForm} disabled={subjects.length === 0}>
              <Plus size={15} aria-hidden="true" />
              Adicionar horário
            </button>
          </article>
        </div>
      )}
    </section>
  )
}
