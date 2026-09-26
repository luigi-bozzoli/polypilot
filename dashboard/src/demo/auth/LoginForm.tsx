import { useState } from 'react'
import type { FormEvent } from 'react'
import { login } from '../../features/auth/authApi'
import { Button } from '../../components/auth/Button'
import { FormField } from '../../components/auth/FormField'
import { PasswordField } from '../../components/auth/PasswordField'
import { LoginResponse } from '../../features/auth/types'

type LoginFormProps = {
  onSuccess: (result: LoginResponse) => void
}

/**
 * Demo replacement for `components/auth/LoginForm.tsx` (aliased in `vite.config.ts`, only for
 * `mode === 'demo'`). Same props signature as the real component. Skips client-side validation
 * and calls the real `authApi.login`, which the demo `POST /api/auth/login` handler accepts
 * unconditionally — so any input, including the prefilled demo values, logs in.
 */
export function LoginForm({ onSuccess }: LoginFormProps) {
  const [email, setEmail] = useState('demo@polypilot.local')
  const [password, setPassword] = useState('demo')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      const result = await login(email, password)
      onSuccess(result)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={(e) => void handleSubmit(e)} noValidate>
      <p className="mb-4 text-[11px] text-text-muted">Demo: any credentials work</p>

      <FormField
        id="login-email"
        label="Email"
        type="email"
        autoComplete="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="you@example.com"
      />

      <PasswordField
        id="login-password"
        label="Password"
        autoComplete="current-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="••••••••"
      />

      <Button type="submit" loading={submitting} className="mt-2">
        {submitting ? 'Logging in…' : 'Log in'}
      </Button>
    </form>
  )
}
