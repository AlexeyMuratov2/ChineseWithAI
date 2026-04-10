import type { PropsWithChildren } from 'react'

type Props = PropsWithChildren<{
  title: string
  description?: string
}>

export const PageShell = ({ title, description, children }: Props) => {
  return (
    <section className="space-y-8">
      <header className="space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight text-app-fg">{title}</h1>
        {description ? <p className="max-w-2xl text-sm text-app-muted">{description}</p> : null}
      </header>
      {children}
    </section>
  )
}
