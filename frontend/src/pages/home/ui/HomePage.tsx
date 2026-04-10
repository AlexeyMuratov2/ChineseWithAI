import { PageShell } from '@/shared/ui/PageShell'
import { EmptyState } from '@/shared/ui/EmptyState'

export const HomePage = () => {
  return (
    <PageShell
      title="Frontend Foundation"
      description="Scalable feature-first architecture scaffold for future product modules."
    >
      <EmptyState
        title="Project skeleton is ready"
        description="Use features/entities/widgets layers to implement business modules step by step."
      />
    </PageShell>
  )
}
