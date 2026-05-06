import type { TextareaHTMLAttributes } from 'react'

import { cn } from '@/shared/lib/cn'

type Props = TextareaHTMLAttributes<HTMLTextAreaElement>

export const Textarea = ({ className, ...props }: Props) => {
  return (
    <textarea
      className={cn(
        'min-h-28 w-full resize-y rounded-xl border border-[#dbe7f3] bg-white px-3 py-3 text-sm text-[#24324b] shadow-[inset_0_1px_0_rgba(255,255,255,0.8)] outline-none transition placeholder:text-[#8a94a8] focus:border-[#19a7a0] focus:ring-2 focus:ring-[#19a7a0]/20',
        className,
      )}
      {...props}
    />
  )
}
