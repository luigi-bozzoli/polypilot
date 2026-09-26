import { Navigate, useNavigate } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { useAuth } from '../features/auth/AuthContext'

/**
 * The single public route. Self-service signup has been removed — the only ways
 * in are wallet (SIWE) sign-in and the admin/demo email+password form, both
 * rendered by {@link LoginPage}.
 */
export function AuthPage() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()

  if (isAuthenticated) {
    return <Navigate to="/overview" replace />
  }

  return (
    <LoginPage
      onSuccess={(result) => {
        login(result)
        navigate('/overview', { replace: true })
      }}
    />
  )
}
