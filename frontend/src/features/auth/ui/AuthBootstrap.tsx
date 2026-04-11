import type { PropsWithChildren } from 'react'

import { useAuthBootstrap } from '@/features/auth/model/use-auth-bootstrap'

export const AuthBootstrap = ({ children }: PropsWithChildren) => {
  useAuthBootstrap()

  return children
}
