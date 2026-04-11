import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useSessionStore } from '@/entities/session'
import { APP_ROUTES } from '@/shared/config/constants'
import { AuthGuardFallback } from '@/features/auth/ui/AuthGuardFallback'

export const RequireAuth = () => {
  const status = useSessionStore((state) => state.status)
  const location = useLocation()

  if (status === 'checking') {
    return <AuthGuardFallback />
  }

  if (status === 'guest') {
    return <Navigate to={APP_ROUTES.login} replace state={{ from: location }} />
  }

  return <Outlet />
}
