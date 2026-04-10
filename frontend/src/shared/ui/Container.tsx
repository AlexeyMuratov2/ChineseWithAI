import type { HTMLAttributes } from 'react'

import { cn } from '@/shared/lib/cn'

type Props = HTMLAttributes<HTMLDivElement>

export const Container = ({ className, ...props }: Props) => {
  return (
    <div className={cn('mx-auto w-full max-w-6xl px-4 sm:px-6 lg:px-8', className)} {...props} />
  )
}
