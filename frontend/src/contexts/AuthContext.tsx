import {
  type PropsWithChildren,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react'
import { authApi } from '../api/auth'
import { tokenStorage } from '../api/client'
import { AuthContext, type AuthContextValue } from './auth-context'
import type {
  LoginRequest,
  PasswordChangeRequest,
  User,
} from '../types/api'

export function AuthProvider({ children }: PropsWithChildren) {
  const [token, setToken] = useState<string | null>(() => tokenStorage.get())
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const clearSession = useCallback(() => {
    tokenStorage.clear()
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    const handleExpiredSession = () => {
      clearSession()
    }

    window.addEventListener('auth:expired', handleExpiredSession)
    return () => window.removeEventListener('auth:expired', handleExpiredSession)
  }, [clearSession])

  useEffect(() => {
    let active = true

    async function loadCurrentUser() {
      const storedToken = tokenStorage.get()

      if (!storedToken) {
        if (active) {
          setIsLoading(false)
        }
        return
      }

      try {
        const currentUser = await authApi.me()

        if (active) {
          setToken(storedToken)
          setUser(currentUser)
        }
      } catch {
        if (active) {
          clearSession()
        }
      } finally {
        if (active) {
          setIsLoading(false)
        }
      }
    }

    void loadCurrentUser()

    return () => {
      active = false
    }
  }, [clearSession])

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials)
    tokenStorage.set(response.token)
    setToken(response.token)
    setUser(response.user)
    return response
  }, [])

  const changePassword = useCallback(async (payload: PasswordChangeRequest) => {
    const updatedUser = await authApi.changePassword(payload)
    setUser(updatedUser)
    return updatedUser
  }, [])

  const refreshUser = useCallback(async () => {
    const currentUser = await authApi.me()
    setUser(currentUser)
    return currentUser
  }, [])

  const logout = useCallback(() => {
    clearSession()
  }, [clearSession])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token && user),
      isLoading,
      login,
      changePassword,
      refreshUser,
      logout,
    }),
    [changePassword, isLoading, login, logout, refreshUser, token, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
