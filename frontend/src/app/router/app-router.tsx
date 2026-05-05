import { Navigate, createBrowserRouter } from 'react-router-dom'

import { AppLayout } from '@/app/layouts/AppLayout'
import { EmptyDashboardPage } from '@/pages/empty-dashboard'
import { APP_ROUTES } from '@/shared/config/constants'

export const appRouter = createBrowserRouter([
  {
    path: APP_ROUTES.root,
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <EmptyDashboardPage />,
      },
    ],
  },
  {
    path: '*',
    element: <Navigate replace to={APP_ROUTES.root} />,
  },
])
