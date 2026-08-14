import { api } from './client'
import type {
  Activity,
  ActivityRequest,
  ActivityStatus,
} from '../types/api'

interface ActivityQuery {
  status?: ActivityStatus
  subjectId?: number
  dueBefore?: string
}

export const activitiesApi = {
  async list(query: ActivityQuery = {}): Promise<Activity[]> {
    const response = await api.get<Activity[]>('/api/activities', { params: query })
    return response.data
  },

  async get(id: number): Promise<Activity> {
    const response = await api.get<Activity>(`/api/activities/${id}`)
    return response.data
  },

  async create(payload: ActivityRequest): Promise<Activity> {
    const response = await api.post<Activity>('/api/activities', payload)
    return response.data
  },

  async update(id: number, payload: ActivityRequest): Promise<Activity> {
    const response = await api.put<Activity>(`/api/activities/${id}`, payload)
    return response.data
  },

  async remove(id: number): Promise<void> {
    await api.delete(`/api/activities/${id}`)
  },

  async complete(id: number): Promise<Activity> {
    const response = await api.post<Activity>(`/api/activities/${id}/complete`)
    return response.data
  },

  async uncomplete(id: number): Promise<void> {
    await api.delete(`/api/activities/${id}/complete`)
  },
}
