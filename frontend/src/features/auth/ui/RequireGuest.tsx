import { Navigate, Outlet } from 'react-router-dom'

import { useSessionStore } from '@/entities/session'
import { APP_ROUTES } from '@/shared/config/constants'
import { AuthGuardFallback } from '@/features/auth/ui/AuthGuardFallback'

export const RequireGuest = () => {
  const status = useSessionStore((state) => state.status)

  if (status === 'checking') {
    return <AuthGuardFallback />
  }

  if (status === 'authenticated') {
    return <Navigate to={APP_ROUTES.root} replace />
  }

  return <Outlet />
}
