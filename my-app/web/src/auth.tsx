import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { api } from './api'
import type { AuthResponse } from './types'
import { resolveActiveRole, type GymRole } from './roles'

interface AuthContextType {
  user: AuthResponse | null
  activeRole: string | null
  setActiveRole: (role: GymRole) => void
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  isLoading: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

function activeRoleKey(userId: number) {
  return `activeRole_${userId}`
}

function readStoredActiveRole(user: AuthResponse): string | null {
  return resolveActiveRole(user, localStorage.getItem(activeRoleKey(user.userId)))
}

function persistActiveRole(userId: number, role: string) {
  localStorage.setItem(activeRoleKey(userId), role)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(null)
  const [activeRole, setActiveRoleState] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem('user')
    if (stored) {
      const parsed = JSON.parse(stored) as AuthResponse
      setUser(parsed)
      setActiveRoleState(readStoredActiveRole(parsed))
    }
    setIsLoading(false)
  }, [])

  const login = async (email: string, password: string) => {
    const response = await api.login(email, password)
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(response))
    const role = resolveActiveRole(response, localStorage.getItem(activeRoleKey(response.userId)))
    if (role) persistActiveRole(response.userId, role)
    setUser(response)
    setActiveRoleState(role)
  }

  const setActiveRole = useCallback((role: GymRole) => {
    if (!user) return
    if (!user.roles.includes(role)) return
    persistActiveRole(user.userId, role)
    setActiveRoleState(role)
  }, [user])

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
    setActiveRoleState(null)
  }

  return (
    <AuthContext.Provider value={{ user, activeRole, setActiveRole, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
