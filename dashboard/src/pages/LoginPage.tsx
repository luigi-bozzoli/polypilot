import { useState } from 'react'
import { AuthLayout } from '../components/auth/AuthLayout'
import { LoginForm } from '../components/auth/LoginForm'
import { SiweLoginPanel } from '../components/auth/SiweLoginPanel'
import { LoginResponse } from '../features/auth/types'

type LoginPageProps = {
  onSuccess: (result: LoginResponse) => void
}

type Mode = 'wallet' | 'admin'

/**
 * The single public screen. Wallet (SIWE) sign-in is primary; the email/password
 * form for admin & demo/reviewer accounts is one click away and visually
 * secondary. Both paths call {@code onSuccess} with the same {@link LoginResponse}.
 */
export function LoginPage({ onSuccess }: LoginPageProps) {
  const [mode, setMode] = useState<Mode>('wallet')

  return (
    <AuthLayout
      title={mode === 'wallet' ? 'Sign in' : 'Admin & demo sign-in'}
      subtitle={
        mode === 'wallet'
          ? 'Connect your Ethereum wallet to continue'
          : 'Email & password — for reviewers and administrators'
      }
      footer={
        mode === 'wallet' ? (
          <button
            type="button"
            onClick={() => setMode('admin')}
            className="font-medium text-accent hover:underline"
          >
            Admin &amp; demo sign-in
          </button>
        ) : (
          <button
            type="button"
            onClick={() => setMode('wallet')}
            className="font-medium text-accent hover:underline"
          >
            ← Back to wallet sign-in
          </button>
        )
      }
    >
      {mode === 'wallet' ? (
        <SiweLoginPanel onSuccess={onSuccess} />
      ) : (
        <LoginForm onSuccess={onSuccess} />
      )}
    </AuthLayout>
  )
}
