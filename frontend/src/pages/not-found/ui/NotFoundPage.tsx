import { Link } from 'react-router-dom'

import { Button } from '@/shared/ui/Button'
import { PageShell } from '@/shared/ui/PageShell'
import { EmptyState } from '@/shared/ui/EmptyState'

export const NotFoundPage = () => {
  return (
    <PageShell title="Page not found">
      <EmptyState
        title="404"
        description="Route is not implemented yet in the current frontend foundation."
        actions={
          <Link to="/">
            <Button>Go to home</Button>
          </Link>
        }
      />
    </PageShell>
  )
}
