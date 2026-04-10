import type { PropsWithChildren } from 'react'

import { Container } from '@/shared/ui/Container'

export const MainContainer = ({ children }: PropsWithChildren) => {
  return (
    <main className="py-10">
      <Container>{children}</Container>
    </main>
  )
}
