import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'

/**
 * Guard for every non-public route. Unauthenticated visitors are sent to the
 * Auth page; authenticated ones get the nested route via <Outlet />.
 *
 * Auth state is rehydrated synchronously in AuthProvider, so there is no
 * initialization gap to gate on here.
 */
export function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <Outlet /> : <Navigate to="/" replace />
}
