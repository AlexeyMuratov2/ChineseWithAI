import { httpClient } from '@/shared/api/http-client'
import type { AuthToken } from '@/entities/session/model/types'
import type { CurrentUser } from '@/entities/session/model/types'

type AuthHeaderToken = Pick<AuthToken, 'accessToken' | 'tokenType'>

export const fetchCurrentUser = async (token: AuthHeaderToken) => {
  return await httpClient<CurrentUser>('/api/v1/users/me', {
    headers: {
      Authorization: `${token.tokenType} ${token.accessToken}`,
    },
  })
}
