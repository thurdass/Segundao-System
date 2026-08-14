import { CalendarDays, Clock3, GraduationCap } from 'lucide-react'
import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../api/client'
import { schoolApi } from '../api/school'
import { useAuth } from '../hooks/useAuth'
import type { Schedule, Subject, Teacher } from '../types/api'
import {
  dayOrder,
  formatTime,
  getCurrentDayOfWeek,
  getDayLabel,
} from '../utils/date'

interface ScheduleData {
  schedules: Schedule[]
  subjects: Subject[]
  teachers: Teacher[]
}

export function SchedulePage() {
  const { user } = useAuth()
  const [data, setData] = useState<ScheduleData | null>(null)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let active = true

    async function loadSchedule() {
      if (!user) {
        return
      }

      try {
        const [schedules, subjects, teachers] = await Promise.all([
          schoolApi.schedules(user.classroomId),
          schoolApi.subjects(user.classroomId),
          schoolApi.teachers(),
        ])

        if (active) {
          setData({ schedules, subjects, teachers })
        }
      } catch (error: unknown) {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar o horário da turma.'))
        }
      }
    }

    void loadSchedule()

    return () => {
      active = false
    }
  }, [user])

  if (errorMessage) {
    return (
      <section className="page-error" role="alert">
        <CalendarDays size={22} aria-hidden="true" />
        <div>
          <h2>Não foi possível carregar o horário</h2>
          <p>{errorMessage}</p>
        </div>
      </section>
    )
  }

  if (!data) {
    return (
      <div className="data-list-skeleton" aria-label="Carregando horário">
        <div />
        <div />
      </div>
    )
  }

  const subjectNames = new Map(data.subjects.map((subject) => [subject.id, subject.name]))
  const teacherNames = new Map(data.teachers.map((teacher) => [teacher.id, teacher.name]))
  const today = getCurrentDayOfWeek()
  const groupedSchedules = dayOrder
    .map((day) => ({
      day,
      schedules: data.schedules
        .filter((schedule) => schedule.dayOfWeek === day)
        .sort((left, right) => left.startTime.localeCompare(right.startTime)),
    }))
    .filter((group) => group.schedules.length > 0)

  return (
    <div className="data-page">
      <section className="page-heading">
        <div>
          <p className="eyebrow">Organização semanal</p>
          <h2>Horários</h2>
          <p>Consulte as aulas da turma com disciplina, professor e duração.</p>
        </div>
      </section>

      <div className="schedule-note">
        <Clock3 size={18} aria-hidden="true" />
        <span>Os horários são apresentados conforme o cadastro atual da turma.</span>
      </div>

      {groupedSchedules.length === 0 ? (
        <div className="content-card empty-state page-empty-state">
          <CalendarDays size={24} aria-hidden="true" />
          <div>
            <strong>Nenhum horário cadastrado</strong>
            <p>O administrador ainda não configurou as aulas desta turma.</p>
          </div>
        </div>
      ) : (
        <section className="weekly-schedule" aria-label="Horário semanal">
          {groupedSchedules.map((group) => (
            <article className={`schedule-day ${group.day === today ? 'is-today' : ''}`} key={group.day}>
              <header className="schedule-day-heading">
                <div>
                  <p className="eyebrow">{group.day === today ? 'Hoje' : 'Dia da semana'}</p>
                  <h3>{getDayLabel(group.day)}</h3>
                </div>
                {group.day === today && <span className="today-mark">Agora</span>}
              </header>
              <div className="schedule-day-list">
                {group.schedules.map((schedule) => (
                  <div className="full-schedule-row" key={schedule.id}>
                    <div className="full-schedule-time">
                      <strong>{formatTime(schedule.startTime)}</strong>
                      <span>{formatTime(schedule.endTime)}</span>
                    </div>
                    <div className="full-schedule-marker" aria-hidden="true" />
                    <div className="full-schedule-main">
                      <strong>{subjectNames.get(schedule.subjectId) ?? `Disciplina #${schedule.subjectId}`}</strong>
                      {schedule.teacherId && (
                        <span>
                          <GraduationCap size={14} aria-hidden="true" />
                          {teacherNames.get(schedule.teacherId) ?? `Professor #${schedule.teacherId}`}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </article>
          ))}
        </section>
      )}
    </div>
  )
}
