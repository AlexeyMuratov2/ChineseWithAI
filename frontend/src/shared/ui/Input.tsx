import type { InputHTMLAttributes } from 'react'

import { cn } from '@/shared/lib/cn'

type Props = InputHTMLAttributes<HTMLInputElement>

export const Input = ({ className, ...props }: Props) => {
  return (
    <input
      className={cn(
        'h-11 w-full rounded-xl border border-[#d8dcff] bg-white px-3 text-sm text-[#1e2468] shadow-[inset_0_1px_0_rgba(255,255,255,0.8)] outline-none transition focus:border-[#6273ff] focus:ring-2 focus:ring-[#6273ff]/20',
        className,
      )}
      {...props}
    />
  )
}
