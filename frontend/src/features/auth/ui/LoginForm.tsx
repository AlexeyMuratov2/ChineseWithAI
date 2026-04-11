import { useMemo, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'

import { APP_ROUTES } from '@/shared/config/constants'
import { Button } from '@/shared/ui/Button'
import { Input } from '@/shared/ui/Input'
import { getAuthErrorMessage } from '@/features/auth/model/auth-error-message'
import { useAuthActions } from '@/features/auth/model/use-auth-actions'
import { validatePassword, validateUsername } from '@/features/auth/model/validators'
import { AuthFormShell } from '@/features/auth/ui/AuthFormShell'

type RedirectState = {
  from?: {
    pathname?: string
  }
}

export const LoginForm = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { signIn } = useAuthActions()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const redirectTo = useMemo(() => {
    const state = location.state as RedirectState | null
    return state?.from?.pathname ?? APP_ROUTES.root
  }, [location.state])

  const loginMutation = useMutation({
    mutationFn: signIn,
    onSuccess: () => {
      navigate(redirectTo, { replace: true })
    },
    onError: (error) => {
      setFormError(getAuthErrorMessage(error, 'Не удалось выполнить вход. Попробуйте еще раз.'))
    },
  })

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFormError(null)

    const normalizedUsername = username.trim()

    const usernameError = validateUsername(normalizedUsername)
    if (usernameError) {
      setFormError(usernameError)
      return
    }

    const passwordError = validatePassword(password)
    if (passwordError) {
      setFormError(passwordError)
      return
    }

    loginMutation.mutate({
      username: normalizedUsername,
      password,
    })
  }

  return (
    <AuthFormShell
      badge="Вход"
      title="Добро пожаловать обратно"
      description="Продолжай прокачивать китайский: заходи в аккаунт и начинай новую миссию."
      error={formError}
      footer={
        <p>
          Еще нет аккаунта?{' '}
          <Link
            className="font-semibold text-[#3a4dff] transition hover:text-[#2a32cc]"
            to={APP_ROUTES.register}
          >
            Создать сейчас
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
            placeholder="Например, panda_master"
            value={username}
          />
        </label>

        <label className="block space-y-2">
          <span className="text-sm font-semibold text-[#252a67]">Пароль</span>
          <Input
            autoComplete="current-password"
            className="game-input"
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Минимум 8 символов"
            type="password"
            value={password}
          />
        </label>

        <Button
          className="game-button mt-2 h-12 w-full text-base font-bold"
          disabled={loginMutation.isPending}
          type="submit"
        >
          {loginMutation.isPending ? 'Входим...' : 'Войти в портал'}
        </Button>
      </form>
    </AuthFormShell>
  )
}
