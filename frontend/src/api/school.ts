import { api } from './client'
import type { NextClass, Schedule, Subject, Teacher } from '../types/api'

export const schoolApi = {
  async subjects(classroomId: number): Promise<Subject[]> {
    const response = await api.get<Subject[]>('/api/subjects', {
      params: { classroomId },
    })
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

  async teacherSubjects(teacherId: number): Promise<Subject[]> {
    const response = await api.get<Subject[]>(`/api/teachers/${teacherId}/subjects`)
    return response.data
  },

  async schedules(classroomId: number): Promise<Schedule[]> {
    const response = await api.get<Schedule[]>(`/api/schedules/classroom/${classroomId}`)
    return response.data
  },
}
