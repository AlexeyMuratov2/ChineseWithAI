export const validateUsername = (username: string) => {
  const normalized = username.trim()

  if (normalized.length < 3) {
    return 'Логин должен содержать минимум 3 символа.'
  }

  if (normalized.length > 50) {
    return 'Логин не должен превышать 50 символов.'
  }

  return null
}

export const validatePassword = (password: string) => {
  if (password.length < 8) {
    return 'Пароль должен содержать минимум 8 символов.'
  }

  if (password.length > 72) {
    return 'Пароль не должен превышать 72 символа.'
  }

  return null
}

export const validateDisplayName = (displayName: string) => {
  if (displayName.length > 100) {
    return 'Имя в профиле не должно превышать 100 символов.'
  }

  return null
}
