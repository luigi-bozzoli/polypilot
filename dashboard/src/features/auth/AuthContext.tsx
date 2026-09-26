import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useDisconnect } from 'wagmi'
import { LoginResponse } from './types'
import { clearSession, loadSession, saveSession } from './session'

type AuthContextValue = {
  session: LoginResponse | null
  isAuthenticated: boolean
  login: (result: LoginResponse) => void
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  // Rehydrate synchronously so the first render already reflects the persisted
  // session — no loading flash, no window where a refreshed user looks logged out.
  const [session, setSession] = useState<LoginResponse | null>(() => loadSession())
  const { mutate: disconnectAsync } = useDisconnect()

  const login = useCallback((result: LoginResponse) => {
    saveSession(result)
    setSession(result)
  }, [])

  const logout = useCallback(async () => {
    // Sever the browser↔wallet link too, so it doesn't linger (or silently
    // auto-reconnect on the next load) after the account session is gone.
    try {
      await disconnectAsync()
    } catch {
      // no wallet connected / connector unavailable — nothing to do
    }
    clearSession()
    setSession(null)
  }, [disconnectAsync])

  const value = useMemo<AuthContextValue>(
    () => ({ session, isAuthenticated: session !== null, login, logout }),
    [session, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
