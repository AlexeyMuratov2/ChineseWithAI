import { useEffect } from 'react'

import { fetchCurrentUser, useSessionStore } from '@/entities/session'

export const useAuthBootstrap = () => {
  const status = useSessionStore((state) => state.status)
  const token = useSessionStore((state) => state.token)
  const setAuthenticated = useSessionStore((state) => state.setAuthenticated)
  const setGuest = useSessionStore((state) => state.setGuest)

  useEffect(() => {
    if (status !== 'checking') {
      return
    }

    if (!token) {
      setGuest()
      return
    }

    let cancelled = false

    const bootstrap = async () => {
      try {
        const user = await fetchCurrentUser(token)

        if (!cancelled) {
          setAuthenticated({ token, user })
        }
      } catch {
        if (!cancelled) {
          setGuest()
        }
      }
    }

    void bootstrap()

    return () => {
      cancelled = true
    }
  }, [setAuthenticated, setGuest, status, token])
}
