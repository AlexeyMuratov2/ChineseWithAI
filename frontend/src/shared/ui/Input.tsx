import type { InputHTMLAttributes } from 'react'

import { cn } from '@/shared/lib/cn'

type Props = InputHTMLAttributes<HTMLInputElement>

export const Input = ({ className, ...props }: Props) => {
  return (
    <input
      className={cn(
        'h-10 w-full rounded-md border border-app-border bg-white px-3 text-sm text-app-fg outline-none transition focus:border-app-accent',
        className,
      )}
      {...props}
    />
  )
}
