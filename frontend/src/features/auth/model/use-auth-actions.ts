import { fetchCurrentUser, useSessionStore, type AuthToken } from '@/entities/session'
import {
  loginRequest,
  registerRequest,
  type LoginRequest,
  type RegisterRequest,
} from '@/features/auth/api/auth-api'

const toSessionToken = (token: {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}): AuthToken => {
  return {
    ...token,
    issuedAt: Date.now(),
  }
}

export const useAuthActions = () => {
  const setAuthenticated = useSessionStore((state) => state.setAuthenticated)
  const setGuest = useSessionStore((state) => state.setGuest)

  const signIn = async (payload: LoginRequest) => {
    const tokenResponse = await loginRequest(payload)
    const sessionToken = toSessionToken(tokenResponse)
    const user = await fetchCurrentUser(sessionToken)

    setAuthenticated({
      token: sessionToken,
      user,
    })
  }

  const signUp = async (payload: RegisterRequest) => {
    await registerRequest(payload)
    await signIn({
      username: payload.username,
      password: payload.password,
    })
  }

  const signOut = () => {
    setGuest()
  }

  return {
    signIn,
    signUp,
    signOut,
  }
}
