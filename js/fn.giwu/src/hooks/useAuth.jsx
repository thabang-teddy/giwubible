import { useState, useEffect, useCallback, createContext, useContext } from 'react'
import { me, login as apiLogin, register as apiRegister, logout as apiLogout } from '../api/auth'

const TOKEN_KEY = 'giwu_token'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) { setLoading(false); return }
    me()
      .then(setUser)
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (email, password) => {
    const { token, user: u } = await apiLogin(email, password)
    localStorage.setItem(TOKEN_KEY, token)
    setUser(u)
    return u
  }, [])

  const register = useCallback(async (name, email, password) => {
    const { token, user: u } = await apiRegister(name, email, password)
    localStorage.setItem(TOKEN_KEY, token)
    setUser(u)
    return u
  }, [])

  const logout = useCallback(async () => {
    try { await apiLogout() } catch {}
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
