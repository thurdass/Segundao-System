import axios from 'axios'
import type { AxiosError } from 'axios'
import type { ApiErrorPayload } from '../types/api'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const TOKEN_KEY = 'segundao.jwt'

export const tokenStorage = {
  get(): string | null {
    return window.localStorage.getItem(TOKEN_KEY)
  },

  set(token: string): void {
    window.localStorage.setItem(TOKEN_KEY, token)
  },

  clear(): void {
    window.localStorage.removeItem(TOKEN_KEY)
  },
}

export const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = tokenStorage.get()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorPayload>) => {
    if (error.response?.status === 401) {
      tokenStorage.clear()
      window.dispatchEvent(new Event('auth:expired'))
    }

    return Promise.reject(error)
  },
)

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiErrorPayload>(error)) {
    const message = error.response?.data?.message

    if (message) {
      return message
    }

    if (!error.response) {
      return 'Não foi possível conectar ao servidor.'
    }
  }

  return fallback
}
