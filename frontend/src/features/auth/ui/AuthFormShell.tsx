import type { PropsWithChildren, ReactNode } from 'react'

type AuthFormShellProps = PropsWithChildren<{
  badge: string
  title: string
  description: string
  error?: string | null
  footer: ReactNode
}>

export const AuthFormShell = ({
  badge,
  title,
  description,
  error,
  footer,
  children,
}: AuthFormShellProps) => {
  return (
    <section className="game-form-card animate-card-appear w-full max-w-xl rounded-[2rem] p-7 shadow-[0_22px_50px_rgba(17,33,97,0.28)] sm:p-8">
      <header className="space-y-3">
        <p className="inline-flex rounded-full bg-[#f6efff] px-3 py-1 text-xs font-semibold uppercase tracking-[0.12em] text-[#7d39ff]">
          {badge}
        </p>
        <h1 className="font-display text-3xl font-extrabold leading-tight text-[#1a1d5a] sm:text-4xl">
          {title}
        </h1>
        <p className="max-w-md text-sm leading-relaxed text-[#4e5290] sm:text-base">{description}</p>
      </header>

      {error ? (
        <p className="mt-5 rounded-2xl border border-[#ff7b87]/30 bg-[#fff3f5] px-4 py-3 text-sm font-medium text-[#cc3245]">
          {error}
        </p>
      ) : null}

      <div className="mt-6">{children}</div>

      <footer className="mt-6 border-t border-[#d8d9ff] pt-5 text-sm text-[#4e5290]">{footer}</footer>
    </section>
  )
}
