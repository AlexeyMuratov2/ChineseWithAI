import { httpClient } from '@/shared/api/http-client'

export type LoginRequest = {
  username: string
  password: string
}

export type RegisterRequest = {
  username: string
  password: string
  displayName: string | null
}

export type LoginResponse = {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export const loginRequest = async (payload: LoginRequest) => {
  return await httpClient<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: payload,
  })
}

export const registerRequest = async (payload: RegisterRequest) => {
  return await httpClient('/api/v1/auth/register', {
    method: 'POST',
    body: payload,
  })
}
