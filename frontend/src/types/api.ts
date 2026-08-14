export type Role = 'STUDENT' | 'ADMIN'

export type ActivityStatus = 'pending' | 'completed'

export type DeadlineMode = 'CUSTOM_DATE' | 'NEXT_CLASS'

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export interface User {
  id: number
  username: string
  displayName: string
  role: Role
  enabled: boolean
  mustChangePassword: boolean
  classroomId: number
  createdAt: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
  mustChangePassword: boolean
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface Activity {
  id: number
  title: string
  description: string | null
  dueDate: string
  subjectId: number
  classroomId: number
  createdBy: string
  completed: boolean
  completedAt: string | null
}

export interface ActivityRequest {
  title: string
  description?: string
  subjectId: number
  deadlineMode: DeadlineMode
  dueDate?: string
}

export interface Announcement {
  id: number
  title: string
  content: string
  createdAt: string
  createdBy: string
  pinned: boolean
}

export interface AnnouncementRequest {
  title: string
  content: string
  pinned: boolean
}

export interface Subject {
  id: number
  name: string
  shortName: string | null
  classroomId: number
}

export interface Teacher {
  id: number
  name: string
  email: string | null
  active: boolean
}

export interface Schedule {
  id: number
  classroomId: number
  subjectId: number
  teacherId: number | null
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
}

export interface NextClass {
  subjectId: number
  subjectName: string
  nextClassDate: string
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
}

export interface AdminUserRequest {
  username: string
  password: string
  displayName: string
  role?: Role
  classroomId: number
}

export interface AdminDashboard {
  users: number
  activeUsers: number
  activities: number
  announcements: number
}

export interface ApiErrorPayload {
  message?: string
  error?: string
  status?: number
}
