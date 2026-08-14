import { api } from './client'
import type {
  NextClass,
  Schedule,
  ScheduleRequest,
  Subject,
  SubjectRequest,
  Teacher,
  TeacherRequest,
} from '../types/api'

export const schoolApi = {
  async subjects(classroomId: number): Promise<Subject[]> {
    const response = await api.get<Subject[]>('/api/subjects', {
      params: { classroomId },
    })
    return response.data
  },

  async createSubject(payload: SubjectRequest): Promise<Subject> {
    const response = await api.post<Subject>('/api/subjects', payload)
    return response.data
  },

  async nextClass(subjectId: number): Promise<NextClass> {
    const response = await api.get<NextClass>(`/api/subjects/${subjectId}/next-class`)
    return response.data
  },

  async teachers(): Promise<Teacher[]> {
    const response = await api.get<Teacher[]>('/api/teachers')
    return response.data
  },

  async createTeacher(payload: TeacherRequest): Promise<Teacher> {
    const response = await api.post<Teacher>('/api/teachers', payload)
    return response.data
  },

  async updateTeacher(id: number, payload: TeacherRequest): Promise<Teacher> {
    const response = await api.put<Teacher>(`/api/teachers/${id}`, payload)
    return response.data
  },

  async teacherSubjects(teacherId: number): Promise<Subject[]> {
    const response = await api.get<Subject[]>(`/api/teachers/${teacherId}/subjects`)
    return response.data
  },

  async schedules(classroomId: number): Promise<Schedule[]> {
    const response = await api.get<Schedule[]>(`/api/schedules/classroom/${classroomId}`)
    return response.data
  },

  async createSchedule(payload: ScheduleRequest): Promise<Schedule> {
    const response = await api.post<Schedule>('/api/schedules', payload)
    return response.data
  },

  async removeSchedule(id: number): Promise<void> {
    await api.delete(`/api/schedules/${id}`)
  },
}
