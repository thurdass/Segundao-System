import { api } from './client'
import type {
  LoginRequest,
  LoginResponse,
  PasswordChangeRequest,
  User,
} from '../types/api'

export const authApi = {
  async login(payload: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/api/auth/login', payload)
    return response.data
  },

  async me(): Promise<User> {
    const response = await api.get<User>('/api/auth/me')
    return response.data
  },

  async changePassword(payload: PasswordChangeRequest): Promise<User> {
    const response = await api.patch<User>('/api/auth/password', payload)
    return response.data
  },
}
