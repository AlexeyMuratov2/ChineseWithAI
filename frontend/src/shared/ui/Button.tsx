import type { ButtonHTMLAttributes } from 'react'

import { cn } from '@/shared/lib/cn'

type ButtonVariant = 'primary' | 'secondary' | 'ghost'
type ButtonSize = 'sm' | 'md'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
}

const variantMap: Record<ButtonVariant, string> = {
  primary:
    'bg-gradient-to-r from-[#ff6b4a] via-[#ff9f43] to-[#19a7a0] text-white shadow-[0_10px_20px_rgba(255,107,74,0.22)] hover:brightness-105',
  secondary: 'bg-[#e7f7f4] text-[#174b57] hover:bg-[#d6f0eb]',
  ghost: 'bg-transparent text-[#39516c] hover:bg-[#edf6fb]',
}

const sizeMap: Record<ButtonSize, string> = {
  sm: 'h-9 px-3 text-sm',
  md: 'h-10 px-5 text-sm',
}

export const Button = ({
  className,
  variant = 'primary',
  size = 'md',
  type = 'button',
  ...props
}: Props) => {
  return (
    <button
      type={type}
      className={cn(
        'inline-flex items-center justify-center rounded-xl font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#19a7a0] focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
        variantMap[variant],
        sizeMap[size],
        className,
      )}
      {...props}
    />
  )
}
