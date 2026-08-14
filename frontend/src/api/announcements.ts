import { api } from './client'
import type { Announcement, AnnouncementRequest } from '../types/api'

export const announcementsApi = {
  async list(): Promise<Announcement[]> {
    const response = await api.get<Announcement[]>('/api/announcements')
    return response.data
  },

  async get(id: number): Promise<Announcement> {
    const response = await api.get<Announcement>(`/api/announcements/${id}`)
    return response.data
  },

  async create(payload: AnnouncementRequest): Promise<Announcement> {
    const response = await api.post<Announcement>('/api/announcements', payload)
    return response.data
  },

  async update(id: number, payload: AnnouncementRequest): Promise<Announcement> {
    const response = await api.put<Announcement>(`/api/announcements/${id}`, payload)
    return response.data
  },

  async remove(id: number): Promise<void> {
    await api.delete(`/api/announcements/${id}`)
  },
}
