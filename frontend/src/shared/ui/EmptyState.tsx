import type { ReactNode } from 'react'

type Props = {
  title: string
  description?: string
  actions?: ReactNode
}

export const EmptyState = ({ title, description, actions }: Props) => {
  return (
    <div className="rounded-xl border border-dashed border-app-border bg-white p-8 text-center">
      <h2 className="text-xl font-semibold text-app-fg">{title}</h2>
      {description ? <p className="mt-2 text-sm text-app-muted">{description}</p> : null}
      {actions ? <div className="mt-5 flex justify-center">{actions}</div> : null}
    </div>
  )
}
