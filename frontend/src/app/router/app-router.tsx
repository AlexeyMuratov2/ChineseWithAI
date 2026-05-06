import { Navigate, createBrowserRouter } from 'react-router-dom'

import { AppLayout } from '@/app/layouts/AppLayout'
import { LessonWorkspacePage } from '@/pages/lesson-workspace'
import { APP_ROUTES } from '@/shared/config/constants'

export const appRouter = createBrowserRouter([
  {
    path: APP_ROUTES.root,
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <LessonWorkspacePage />,
      },
    ],
  },
  {
    path: '*',
    element: <Navigate replace to={APP_ROUTES.root} />,
  },
])
