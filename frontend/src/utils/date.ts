import type { DayOfWeek, Schedule, Subject } from '../types/api'

export const dayOrder: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
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

interface SaoPauloNow {
  date: string
  dayOfWeek: DayOfWeek
  minutes: number
}

interface ScheduleWithSubject {
  schedule: Schedule
  subject: Subject | undefined
  daysUntil: number
}

function partValue(parts: Intl.DateTimeFormatPart[], type: Intl.DateTimeFormatPartTypes): string {
  return parts.find((part) => part.type === type)?.value ?? ''
}

function dayFromWeekday(weekday: string): DayOfWeek {
  const weekdays: Record<string, DayOfWeek> = {
    Monday: 'MONDAY',
    Tuesday: 'TUESDAY',
    Wednesday: 'WEDNESDAY',
    Thursday: 'THURSDAY',
    Friday: 'FRIDAY',
    Saturday: 'SATURDAY',
    Sunday: 'SUNDAY',
  }

  return weekdays[weekday] ?? 'MONDAY'
}

function currentInSaoPaulo(): SaoPauloNow {
  const now = new Date()
  const dateParts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now)
  const timeParts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(now)
  const weekday = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    weekday: 'long',
  }).format(now)

  const year = partValue(dateParts, 'year')
  const month = partValue(dateParts, 'month')
  const day = partValue(dateParts, 'day')
  const hour = Number(partValue(timeParts, 'hour'))
  const minute = Number(partValue(timeParts, 'minute'))

  return {
    date: `${year}-${month}-${day}`,
    dayOfWeek: dayFromWeekday(weekday),
    minutes: hour * 60 + minute,
  }
}

function timeToMinutes(time: string): number {
  const [hour, minute] = time.split(':').map(Number)
  return hour * 60 + minute
}

function addDays(dateString: string, days: number): string {
  const date = new Date(`${dateString}T12:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

export function getNextSchedule(
  schedules: Schedule[],
  subjects: Subject[],
): ScheduleWithSubject | null {
  if (schedules.length === 0) {
    return null
  }

  const now = currentInSaoPaulo()
  const currentDayIndex = dayOrder.indexOf(now.dayOfWeek)
  const candidates = schedules
    .map((schedule) => {
      const scheduleDayIndex = dayOrder.indexOf(schedule.dayOfWeek)
      let daysUntil = (scheduleDayIndex - currentDayIndex + 7) % 7

      if (daysUntil === 0 && timeToMinutes(schedule.startTime) <= now.minutes) {
        daysUntil = 7
      }

      return {
        schedule,
        subject: subjects.find((subject) => subject.id === schedule.subjectId),
        daysUntil,
      }
    })
    .sort((left, right) => {
      if (left.daysUntil !== right.daysUntil) {
        return left.daysUntil - right.daysUntil
      }

      return timeToMinutes(left.schedule.startTime) - timeToMinutes(right.schedule.startTime)
    })

  return candidates[0] ?? null
}

export function nextScheduleDate(schedule: Schedule): string {
  const now = currentInSaoPaulo()
  const currentDayIndex = dayOrder.indexOf(now.dayOfWeek)
  const scheduleDayIndex = dayOrder.indexOf(schedule.dayOfWeek)
  let daysUntil = (scheduleDayIndex - currentDayIndex + 7) % 7

  if (daysUntil === 0 && timeToMinutes(schedule.startTime) <= now.minutes) {
    daysUntil = 7
  }

  return addDays(now.date, daysUntil)
}

export function getTodayDate(): string {
  return currentInSaoPaulo().date
}

export function getCurrentDayOfWeek(): DayOfWeek {
  return currentInSaoPaulo().dayOfWeek
}

export function getDayLabel(dayOfWeek: DayOfWeek): string {
  return dayLabels[dayOfWeek]
}

export function formatTime(time: string): string {
  return time.slice(0, 5)
}

export function formatDate(dateString: string): string {
  const date = new Date(`${dateString}T12:00:00Z`)
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'short',
    timeZone: 'UTC',
  }).format(date)
}

export function formatDueDate(dateString: string): string {
  const today = currentInSaoPaulo().date
  const tomorrow = addDays(today, 1)

  if (dateString === today) {
    return 'Hoje'
  }

  if (dateString === tomorrow) {
    return 'Amanhã'
  }

  return formatDate(dateString)
}

export function isPastDate(dateString: string): boolean {
  return dateString < currentInSaoPaulo().date
}

export function formatDateTime(dateString: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(dateString))
}
