import { usePage, router } from '@inertiajs/react'

/**
 * Reads the authenticated user from Inertia's shared props (session/cookie
 * auth) and exposes a logout that posts to the web route. Login, register and
 * profile updates are handled with Inertia forms inside their pages.
 */
export function useAuth() {
  const { auth } = usePage().props
  const logout = () => router.post('/logout')
  return { user: auth?.user ?? null, logout }
}
