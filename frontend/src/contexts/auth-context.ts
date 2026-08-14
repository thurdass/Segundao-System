import { createContext } from 'react'
import type {
  LoginRequest,
  LoginResponse,
  PasswordChangeRequest,
  User,
} from '../types/api'

export interface AuthContextValue {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login(credentials: LoginRequest): Promise<LoginResponse>
  changePassword(payload: PasswordChangeRequest): Promise<User>
  refreshUser(): Promise<User>
  logout(): void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
