import { BookOpen, CalendarClock, CircleAlert } from 'lucide-react'
import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../api/client'
import { schoolApi } from '../api/school'
import { useAuth } from '../hooks/useAuth'
import type { NextClass, Subject } from '../types/api'
import { formatDate, formatTime, getDayLabel } from '../utils/date'

export function SubjectsPage() {
  const { user } = useAuth()
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [nextClasses, setNextClasses] = useState<Record<number, NextClass | null>>({})
  const [nextClassErrors, setNextClassErrors] = useState<Record<number, string>>({})
  const [loadingSubjectId, setLoadingSubjectId] = useState<number | null>(null)

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
      } finally {
        if (active) {
          setIsLoading(false)
        }
      }
    }

    void loadSubjects()

    return () => {
      active = false
    }
  }, [user])

  async function showNextClass(subjectId: number) {
    setLoadingSubjectId(subjectId)
    setNextClassErrors((current) => ({ ...current, [subjectId]: '' }))

    try {
      const result = await schoolApi.nextClass(subjectId)
      setNextClasses((current) => ({ ...current, [subjectId]: result }))
    } catch (error: unknown) {
      setNextClasses((current) => ({ ...current, [subjectId]: null }))
      setNextClassErrors((current) => ({
        ...current,
        [subjectId]: getApiErrorMessage(error, 'Não há próxima aula cadastrada.'),
      }))
    } finally {
      setLoadingSubjectId(null)
    }
  }

  if (errorMessage) {
    return (
      <section className="page-error" role="alert">
        <BookOpen size={22} aria-hidden="true" />
        <div>
          <h2>Não foi possível carregar as disciplinas</h2>
          <p>{errorMessage}</p>
        </div>
      </section>
    )
  }

  return (
    <div className="data-page">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Base de conhecimento</p>
          <h2>Disciplinas</h2>
          <p>Veja as matérias da turma e consulte quando será a próxima aula.</p>
        </div>
      </section>

      {isLoading ? (
        <div className="data-list-skeleton" aria-label="Carregando disciplinas">
          <div />
          <div />
          <div />
        </div>
      ) : subjects.length === 0 ? (
        <div className="content-card empty-state page-empty-state">
          <BookOpen size={24} aria-hidden="true" />
          <div>
            <strong>Nenhuma disciplina cadastrada</strong>
            <p>As matérias da turma aparecerão aqui quando forem configuradas.</p>
          </div>
        </div>
      ) : (
        <section className="school-card-grid" aria-label="Lista de disciplinas">
          {subjects.map((subject) => {
            const nextClass = nextClasses[subject.id]
            const nextClassError = nextClassErrors[subject.id]

            return (
              <article className="school-card" key={subject.id}>
                <div className="school-card-icon">
                  <BookOpen size={20} aria-hidden="true" />
                </div>
                <div className="school-card-main">
                  <span className="school-card-code">{subject.shortName || 'DISCIPLINA'}</span>
                  <h3>{subject.name}</h3>
                  <p>Matéria da turma 2º Informática A.</p>
                </div>
                <button className="school-card-action" type="button" onClick={() => void showNextClass(subject.id)} disabled={loadingSubjectId === subject.id}>
                  <CalendarClock size={16} aria-hidden="true" />
                  {loadingSubjectId === subject.id ? 'Consultando...' : 'Próxima aula'}
                </button>
                {nextClass && (
                  <div className="school-card-preview">
                    <strong>{getDayLabel(nextClass.dayOfWeek)}</strong>
                    <span>{formatDate(nextClass.nextClassDate)} · {formatTime(nextClass.startTime)} — {formatTime(nextClass.endTime)}</span>
                  </div>
                )}
                {nextClassError && (
                  <div className="school-card-error">
                    <CircleAlert size={15} aria-hidden="true" />
                    <span>{nextClassError}</span>
                  </div>
                )}
              </article>
            )
          })}
        </section>
      )}
    </div>
  )
}
