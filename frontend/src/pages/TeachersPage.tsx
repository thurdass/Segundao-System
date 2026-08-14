import { CircleAlert, GraduationCap, Mail, Plus, UsersRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../api/client'
import { schoolApi } from '../api/school'
import type { Subject, Teacher } from '../types/api'

export function TeachersPage() {
  const [teachers, setTeachers] = useState<Teacher[]>([])
  const [subjectsByTeacher, setSubjectsByTeacher] = useState<Record<number, Subject[]>>({})
  const [expandedTeacherId, setExpandedTeacherId] = useState<number | null>(null)
  const [loadingTeacherId, setLoadingTeacherId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [detailErrors, setDetailErrors] = useState<Record<number, string>>({})

  useEffect(() => {
    let active = true

    async function loadTeachers() {
      try {
        const result = await schoolApi.teachers()

        if (active) {
          setTeachers(result)
        }
      } catch (error: unknown) {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar os professores.'))
        }
      } finally {
        if (active) {
          setIsLoading(false)
        }
      }
    }

    void loadTeachers()

    return () => {
      active = false
    }
  }, [])

  async function toggleSubjects(teacherId: number) {
    if (expandedTeacherId === teacherId) {
      setExpandedTeacherId(null)
      return
    }

    setExpandedTeacherId(teacherId)

    if (subjectsByTeacher[teacherId]) {
      return
    }

    setLoadingTeacherId(teacherId)
    setDetailErrors((current) => ({ ...current, [teacherId]: '' }))

    try {
      const result = await schoolApi.teacherSubjects(teacherId)
      setSubjectsByTeacher((current) => ({ ...current, [teacherId]: result }))
    } catch (error: unknown) {
      setDetailErrors((current) => ({
        ...current,
        [teacherId]: getApiErrorMessage(error, 'Não foi possível carregar as disciplinas deste professor.'),
      }))
    } finally {
      setLoadingTeacherId(null)
    }
  }

  if (errorMessage) {
    return (
      <section className="page-error" role="alert">
        <GraduationCap size={22} aria-hidden="true" />
        <div>
          <h2>Não foi possível carregar os professores</h2>
          <p>{errorMessage}</p>
        </div>
      </section>
    )
  }

  return (
    <div className="data-page">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Equipe pedagógica</p>
          <h2>Professores</h2>
          <p>Consulte quem acompanha cada disciplina da turma.</p>
        </div>
      </section>

      {isLoading ? (
        <div className="data-list-skeleton" aria-label="Carregando professores">
          <div />
          <div />
          <div />
        </div>
      ) : teachers.length === 0 ? (
        <div className="content-card empty-state page-empty-state">
          <UsersRound size={24} aria-hidden="true" />
          <div>
            <strong>Nenhum professor cadastrado</strong>
            <p>Os professores da turma aparecerão aqui quando forem configurados.</p>
          </div>
        </div>
      ) : (
        <section className="teacher-list" aria-label="Lista de professores">
          {teachers.map((teacher) => {
            const isExpanded = expandedTeacherId === teacher.id
            const teacherSubjects = subjectsByTeacher[teacher.id]
            const detailError = detailErrors[teacher.id]

            return (
              <article className="teacher-card" key={teacher.id}>
                <div className="teacher-avatar" aria-hidden="true">
                  {teacher.name.slice(0, 1).toUpperCase()}
                </div>
                <div className="teacher-main">
                  <div className="teacher-heading-row">
                    <div>
                      <span className={`status-badge ${teacher.active ? 'status-completed' : 'status-late'}`}>
                        {teacher.active ? 'Ativo' : 'Inativo'}
                      </span>
                      <h3>{teacher.name}</h3>
                    </div>
                    <button className="school-card-action" type="button" onClick={() => void toggleSubjects(teacher.id)} disabled={loadingTeacherId === teacher.id}>
                      <Plus size={16} aria-hidden="true" />
                      {loadingTeacherId === teacher.id ? 'Carregando...' : isExpanded ? 'Ocultar' : 'Disciplinas'}
                    </button>
                  </div>
                  {teacher.email && (
                    <a className="teacher-email" href={`mailto:${teacher.email}`}>
                      <Mail size={15} aria-hidden="true" />
                      {teacher.email}
                    </a>
                  )}
                  {isExpanded && (
                    <div className="teacher-subjects">
                      {detailError ? (
                        <span className="school-card-error">
                          <CircleAlert size={15} aria-hidden="true" />
                          {detailError}
                        </span>
                      ) : teacherSubjects && teacherSubjects.length > 0 ? (
                        teacherSubjects.map((subject) => <span key={subject.id}>{subject.name}</span>)
                      ) : (
                        <span>Nenhuma disciplina relacionada.</span>
                      )}
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
