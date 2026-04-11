import { create } from 'zustand'

import { sessionStorage } from '@/entities/session/model/session-storage'
import type { AuthToken, CurrentUser, SessionStatus } from '@/entities/session/model/types'

type SessionStore = {
  status: SessionStatus
  token: AuthToken | null
  user: CurrentUser | null
  beginChecking: () => void
  setAuthenticated: (payload: { token: AuthToken; user: CurrentUser }) => void
  setGuest: () => void
}

const initialToken = sessionStorage.read()

const initialState: Pick<SessionStore, 'status' | 'token' | 'user'> = {
  status: initialToken ? 'checking' : 'guest',
  token: initialToken,
  user: null,
}

export const useSessionStore = create<SessionStore>()((set) => ({
  ...initialState,
  beginChecking: () => set((state) => ({ ...state, status: 'checking' })),
  setAuthenticated: ({ token, user }) => {
    sessionStorage.write(token)
    set({
      status: 'authenticated',
      token,
      user,
    })
  },
  setGuest: () => {
    sessionStorage.clear()
    set({
      status: 'guest',
      token: null,
      user: null,
    })
  },
}))
