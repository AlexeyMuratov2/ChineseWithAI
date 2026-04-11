export type AuthToken = {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  issuedAt: number
}

export type CurrentUser = {
  id: string
  username: string
  displayName: string | null
  status: string
  createdAt: string
  updatedAt: string
}

export type SessionStatus = 'checking' | 'authenticated' | 'guest'
