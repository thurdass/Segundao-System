import { api } from './client'
import type { AdminDashboard, AdminUserRequest, User } from '../types/api'

export const adminApi = {
  async dashboard(): Promise<AdminDashboard> {
    const response = await api.get<AdminDashboard>('/api/admin/dashboard')
    return response.data
  },

  async users(): Promise<User[]> {
    const response = await api.get<User[]>('/api/admin/users')
    return response.data
  },

  async user(id: number): Promise<User> {
    const response = await api.get<User>(`/api/admin/users/${id}`)
    return response.data
  },

  async createUser(payload: AdminUserRequest): Promise<User> {
    const response = await api.post<User>('/api/admin/users', payload)
    return response.data
  },

  async setUserStatus(id: number, enabled: boolean): Promise<User> {
    const response = await api.patch<User>(`/api/admin/users/${id}/status`, null, {
      params: { enabled },
    })
    return response.data
  },
}
