import {
  ArrowUpRight,
  Bell,
  CalendarClock,
  CheckCircle2,
  ClipboardList,
  Clock3,
  Megaphone,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { activitiesApi } from '../api/activities'
import { announcementsApi } from '../api/announcements'
import { schoolApi } from '../api/school'
import { getApiErrorMessage } from '../api/client'
import { LoadingScreen } from '../components/LoadingScreen'
import { useAuth } from '../hooks/useAuth'
import type { Activity, Announcement, Schedule, Subject } from '../types/api'
import {
  formatDate,
  formatDateTime,
  formatDueDate,
  formatTime,
  getCurrentDayOfWeek,
  getDayLabel,
  getTodayDate,
  getNextSchedule,
  nextScheduleDate,
  isPastDate,
} from '../utils/date'

interface DashboardData {
  activities: Activity[]
  announcements: Announcement[]
  subjects: Subject[]
  schedules: Schedule[]
}

function DashboardSkeleton() {
  return (
    <div className="dashboard-grid" aria-label="Carregando resumo">
      <div className="skeleton-block skeleton-hero" />
      <div className="skeleton-block" />
      <div className="skeleton-block skeleton-wide" />
    </div>
  )
}

export function DashboardPage() {
  const { user } = useAuth()
  const [data, setData] = useState<DashboardData | null>(null)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let active = true

    async function loadDashboard() {
      if (!user) {
        return
      }

      try {
        const [activities, announcements, subjects, schedules] = await Promise.all([
          activitiesApi.list({ status: 'pending' }),
          announcementsApi.list(),
          schoolApi.subjects(user.classroomId),
          schoolApi.schedules(user.classroomId),
        ])

        if (active) {
          setData({ activities, announcements, subjects, schedules })
        }
      } catch (error: unknown) {
        if (active) {
          setErrorMessage(getApiErrorMessage(error, 'Não foi possível carregar o resumo da turma.'))
        }
      }
    }

    void loadDashboard()

    return () => {
      active = false
    }
  }, [user])

  const subjectNames = useMemo(() => {
    return new Map(data?.subjects.map((subject) => [subject.id, subject.name]))
  }, [data?.subjects])

  if (!data && !errorMessage) {
    return <DashboardSkeleton />
  }

  if (errorMessage) {
    return (
      <section className="page-error" role="alert">
        <Bell size={22} aria-hidden="true" />
        <div>
          <h2>Não foi possível carregar a dashboard</h2>
          <p>{errorMessage}</p>
        </div>
      </section>
    )
  }

  if (!data || !user) {
    return <LoadingScreen />
  }

  const nextSchedule = getNextSchedule(data.schedules, data.subjects)
  const dueSoon = data.activities.slice(0, 3)
  const todaySchedule = data.schedules
    .filter((schedule) => schedule.dayOfWeek === getCurrentDayOfWeek())
    .slice(0, 4)
  const pendingCount = data.activities.length

  return (
    <div className="dashboard-page">
      <section className="dashboard-intro">
        <div>
          <p className="eyebrow">Visão geral · {formatDate(getTodayDate())}</p>
          <h2>Olá, {user.displayName.split(' ')[0]}.</h2>
          <p>Acompanhe o que está acontecendo na sua turma hoje.</p>
        </div>
        <Link className="outline-button" to="/activities">
          Ver atividades
          <ArrowUpRight size={17} aria-hidden="true" />
        </Link>
      </section>

      <section className="dashboard-grid dashboard-summary-grid">
        <article className="feature-card next-class-card">
          <div className="card-heading-row">
            <div className="card-icon yellow-icon">
              <CalendarClock size={20} aria-hidden="true" />
            </div>
            <span className="card-label">Próxima aula</span>
          </div>
          {nextSchedule ? (
            <>
              <strong className="feature-card-title">
                {nextSchedule.subject?.name ?? `Disciplina #${nextSchedule.schedule.subjectId}`}
              </strong>
              <span className="feature-card-meta">
                {getDayLabel(nextSchedule.schedule.dayOfWeek)} · {formatDate(nextScheduleDate(nextSchedule.schedule))}
              </span>
              <div className="feature-time">
                <Clock3 size={19} aria-hidden="true" />
                {formatTime(nextSchedule.schedule.startTime)} — {formatTime(nextSchedule.schedule.endTime)}
              </div>
            </>
          ) : (
            <p className="empty-card-copy">Nenhum horário cadastrado para a turma.</p>
          )}
        </article>

        <article className="summary-card">
          <div className="card-heading-row">
            <div className="card-icon dark-icon">
              <ClipboardList size={20} aria-hidden="true" />
            </div>
            <span className="card-label">Atividades</span>
          </div>
          <strong className="summary-number">{pendingCount}</strong>
          <span className="summary-copy">pendentes para você</span>
          <Link className="card-link" to="/activities">
            Abrir lista <ArrowUpRight size={15} aria-hidden="true" />
          </Link>
        </article>

        <article className="summary-card">
          <div className="card-heading-row">
            <div className="card-icon soft-icon">
              <Megaphone size={20} aria-hidden="true" />
            </div>
            <span className="card-label">Avisos da turma</span>
          </div>
          <strong className="summary-number">{data.announcements.length}</strong>
          <span className="summary-copy">publicados recentemente</span>
          <Link className="card-link" to="/announcements">
            Ver avisos <ArrowUpRight size={15} aria-hidden="true" />
          </Link>
        </article>
      </section>

      <section className="dashboard-content-grid">
        <article className="content-card">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Organize seus prazos</p>
              <h3>Atividades próximas</h3>
            </div>
            <Link className="text-link" to="/activities">
              Ver todas <ArrowUpRight size={15} aria-hidden="true" />
            </Link>
          </div>

          {dueSoon.length > 0 ? (
            <div className="dashboard-list">
              {dueSoon.map((activity) => (
                <Link className="dashboard-list-row" to={`/activities?activity=${activity.id}`} key={activity.id}>
                  <span className="list-status-dot" aria-hidden="true" />
                  <span className="dashboard-list-main">
                    <strong>{activity.title}</strong>
                    <small>{subjectNames.get(activity.subjectId) ?? `Disciplina #${activity.subjectId}`}</small>
                  </span>
                  <span className={`due-label ${isPastDate(activity.dueDate) ? 'is-late' : ''}`}>
                    {formatDueDate(activity.dueDate)}
                  </span>
                </Link>
              ))}
            </div>
          ) : (
            <div className="empty-state compact-empty">
              <CheckCircle2 size={22} aria-hidden="true" />
              <p>Nenhuma atividade pendente no momento.</p>
            </div>
          )}
        </article>

        <article className="content-card">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Acompanhe o dia</p>
              <h3>Horários de hoje</h3>
            </div>
            <Link className="text-link" to="/schedule">
              Abrir horário <ArrowUpRight size={15} aria-hidden="true" />
            </Link>
          </div>

          {todaySchedule.length > 0 ? (
            <div className="schedule-list">
              {todaySchedule.map((schedule) => (
                <div className="schedule-row" key={schedule.id}>
                  <span className="schedule-time">{formatTime(schedule.startTime)}</span>
                  <span className="schedule-line" aria-hidden="true" />
                  <span className="dashboard-list-main">
                    <strong>{subjectNames.get(schedule.subjectId) ?? `Disciplina #${schedule.subjectId}`}</strong>
                    <small>{getDayLabel(schedule.dayOfWeek)}</small>
                  </span>
                  <span className="schedule-end">{formatTime(schedule.endTime)}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state compact-empty">
              <Clock3 size={22} aria-hidden="true" />
              <p>Não há aulas configuradas para este período.</p>
            </div>
          )}
        </article>
      </section>

      <section className="content-card announcements-preview">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Mural da turma</p>
            <h3>Últimos avisos</h3>
          </div>
          <Link className="text-link" to="/announcements">
            Ver mural <ArrowUpRight size={15} aria-hidden="true" />
          </Link>
        </div>

        {data.announcements.length > 0 ? (
          <div className="announcement-preview-grid">
            {data.announcements.slice(0, 3).map((announcement) => (
              <Link className="announcement-preview" to="/announcements" key={announcement.id}>
                <span className="announcement-preview-topline">
                  {announcement.pinned && <span className="pinned-mark">Fixado</span>}
                  <span>{formatDateTime(announcement.createdAt)}</span>
                </span>
                <strong>{announcement.title}</strong>
                <p>{announcement.content}</p>
              </Link>
            ))}
          </div>
        ) : (
          <div className="empty-state compact-empty">
            <Bell size={22} aria-hidden="true" />
            <p>Nenhum aviso publicado ainda.</p>
          </div>
        )}
      </section>
    </div>
  )
}
