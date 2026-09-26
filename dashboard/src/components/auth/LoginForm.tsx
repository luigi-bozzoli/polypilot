import { useState } from 'react'
import type { FormEvent } from 'react'
import { login } from '../../features/auth/authApi'
import { Button } from './Button'
import { FormError } from './FormError'
import { FormField } from './FormField'
import { PasswordField } from './PasswordField'
import { LoginResponse } from '../../features/auth/types'

type LoginFormProps = {
  onSuccess: (result: LoginResponse) => void
}

export function LoginForm({ onSuccess }: LoginFormProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const validate = () => {
    const errors: { email?: string; password?: string } = {}
    if (!/^\S+@\S+\.\S+$/.test(email)) errors.email = 'Enter a valid email address'
    if (!password) errors.password = 'Password is required'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setServerError(null)
    if (!validate()) return

    setSubmitting(true)
    try {
      const result = await login(email, password)
      onSuccess(result)
    } catch (err) {
      setServerError((err as Error).message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {serverError && <FormError message={serverError} />}

      <FormField
        id="login-email"
        label="Email"
        type="email"
        autoComplete="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        error={fieldErrors.email}
        placeholder="you@example.com"
      />

      <PasswordField
        id="login-password"
        label="Password"
        autoComplete="current-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        error={fieldErrors.password}
        placeholder="••••••••"
      />

      <Button type="submit" loading={submitting} className="mt-2">
        {submitting ? 'Logging in…' : 'Log in'}
      </Button>
    </form>
  )
}
