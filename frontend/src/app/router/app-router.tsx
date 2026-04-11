import { Navigate, createBrowserRouter } from 'react-router-dom'

import { AuthLayout } from '@/app/layouts/AuthLayout'
import { ProtectedLayout } from '@/app/layouts/ProtectedLayout'
import { RequireAuth, RequireGuest } from '@/features/auth'
import { AuthLoginPage } from '@/pages/auth-login'
import { AuthRegisterPage } from '@/pages/auth-register'
import { EmptyDashboardPage } from '@/pages/empty-dashboard'
import { APP_ROUTES } from '@/shared/config/constants'

export const appRouter = createBrowserRouter([
  {
    element: <RequireAuth />,
    children: [
      {
        path: APP_ROUTES.root,
        element: <ProtectedLayout />,
        children: [
          {
            index: true,
            element: <EmptyDashboardPage />,
          },
        ],
      },
    ],
  },
  {
    path: APP_ROUTES.auth,
    element: <RequireGuest />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          {
            index: true,
            element: <Navigate replace to={APP_ROUTES.login} />,
          },
          {
            path: 'login',
            element: <AuthLoginPage />,
          },
          {
            path: 'register',
            element: <AuthRegisterPage />,
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate replace to={APP_ROUTES.root} />,
  },
])
