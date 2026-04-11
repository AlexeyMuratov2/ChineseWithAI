import { HttpError } from '@/shared/api/http-client'

const statusMessages: Record<number, string> = {
  400: 'Проверьте корректность заполнения полей.',
  401: 'Неверный логин или пароль.',
  403: 'Аккаунт временно недоступен. Обратитесь к администратору.',
  409: 'Пользователь с таким логином уже существует.',
}

export const getAuthErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof HttpError) {
    return statusMessages[error.status] ?? error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return fallback
}
