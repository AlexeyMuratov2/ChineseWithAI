import type { AuthToken } from '@/entities/session/model/types'

const SESSION_STORAGE_KEY = 'cwa-auth-session'

const isAuthToken = (value: unknown): value is AuthToken => {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const token = value as Partial<AuthToken>

  return (
    typeof token.accessToken === 'string' &&
    token.accessToken.length > 0 &&
    typeof token.tokenType === 'string' &&
    token.tokenType.length > 0 &&
    typeof token.expiresInSeconds === 'number' &&
    Number.isFinite(token.expiresInSeconds) &&
    typeof token.issuedAt === 'number' &&
    Number.isFinite(token.issuedAt)
  )
}

const isTokenExpired = (token: AuthToken) => {
  const expiresAt = token.issuedAt + token.expiresInSeconds * 1000
  return Date.now() >= expiresAt
}

const read = (): AuthToken | null => {
  const raw = window.localStorage.getItem(SESSION_STORAGE_KEY)

  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as unknown

    if (!isAuthToken(parsed) || isTokenExpired(parsed)) {
      clear()
      return null
    }

    return parsed
  } catch {
    return null
  }
}

const write = (token: AuthToken) => {
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(token))
}

const clear = () => {
  window.localStorage.removeItem(SESSION_STORAGE_KEY)
}

export const sessionStorage = Object.freeze({
  read,
  write,
  clear,
})
