import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'

import { appRouter } from '@/app/router/app-router'
import { AuthBootstrap } from '@/features/auth'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 1000 * 30,
    },
  },
})

export const AppProviders = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthBootstrap>
        <RouterProvider router={appRouter} />
      </AuthBootstrap>
    </QueryClientProvider>
  )
}
