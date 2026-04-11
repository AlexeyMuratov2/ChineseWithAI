import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'

import { APP_ROUTES } from '@/shared/config/constants'
import { Button } from '@/shared/ui/Button'
import { Input } from '@/shared/ui/Input'
import { getAuthErrorMessage } from '@/features/auth/model/auth-error-message'
import { useAuthActions } from '@/features/auth/model/use-auth-actions'
import {
  validateDisplayName,
  validatePassword,
  validateUsername,
} from '@/features/auth/model/validators'
import { AuthFormShell } from '@/features/auth/ui/AuthFormShell'

export const RegisterForm = () => {
  const navigate = useNavigate()
  const { signUp } = useAuthActions()

  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [repeatPassword, setRepeatPassword] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const registerMutation = useMutation({
    mutationFn: signUp,
    onSuccess: () => {
      navigate(APP_ROUTES.root, { replace: true })
    },
    onError: (error) => {
      setFormError(getAuthErrorMessage(error, 'Не удалось создать аккаунт. Попробуйте позже.'))
    },
  })

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFormError(null)

    const normalizedUsername = username.trim()
    const normalizedDisplayName = displayName.trim()

    const usernameError = validateUsername(normalizedUsername)
    if (usernameError) {
      setFormError(usernameError)
      return
    }

    const displayNameError = validateDisplayName(normalizedDisplayName)
    if (displayNameError) {
      setFormError(displayNameError)
      return
    }

    const passwordError = validatePassword(password)
    if (passwordError) {
      setFormError(passwordError)
      return
    }

    if (repeatPassword !== password) {
      setFormError('Пароли не совпадают.')
      return
    }

    registerMutation.mutate({
      username: normalizedUsername,
      password,
      displayName: normalizedDisplayName.length > 0 ? normalizedDisplayName : null,
    })
  }

  return (
    <AuthFormShell
      badge="Регистрация"
      title="Создай новый аккаунт"
      description="Новый герой в команде! Зарегистрируйся и открой доступ к интерактивным урокам."
      error={formError}
      footer={
        <p>
          Уже есть аккаунт?{' '}
          <Link
            className="font-semibold text-[#3a4dff] transition hover:text-[#2a32cc]"
            to={APP_ROUTES.login}
          >
            Войти
          </Link>
        </p>
      }
    >
      <form className="space-y-4" onSubmit={handleSubmit}>
        <label className="block space-y-2">
          <span className="text-sm font-semibold text-[#252a67]">Логин</span>
          <Input
            autoComplete="username"
            className="game-input"
            name="username"
            onChange={(event) => setUsername(event.target.value)}
            placeholder="Например, tiger_linguist"
            value={username}
          />
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold text-[#252a67]">Имя в профиле (опционально)</span>
          <Input
            autoComplete="name"
            className="game-input"
            name="displayName"
            onChange={(event) => setDisplayName(event.target.value)}
            placeholder="Как к тебе обращаться?"
            value={displayName}
          />
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold text-[#252a67]">Пароль</span>
          <Input
            autoComplete="new-password"
            className="game-input"
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Минимум 8 символов"
            type="password"
            value={password}
          />
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold text-[#252a67]">Повтори пароль</span>
          <Input
            autoComplete="new-password"
            className="game-input"
            name="repeatPassword"
            onChange={(event) => setRepeatPassword(event.target.value)}
            placeholder="Повтори пароль"
            type="password"
            value={repeatPassword}
          />
        </label>

        <Button
          className="game-button mt-2 h-12 w-full text-base font-bold"
          disabled={registerMutation.isPending}
          type="submit"
        >
          {registerMutation.isPending ? 'Создаем аккаунт...' : 'Создать аккаунт'}
        </Button>
      </form>
    </AuthFormShell>
  )
}
