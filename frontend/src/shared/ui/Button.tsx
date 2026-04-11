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
    'bg-gradient-to-r from-[#4f62ff] via-[#5b7bff] to-[#4d98ff] text-white shadow-[0_10px_20px_rgba(63,88,255,0.28)] hover:brightness-105',
  secondary: 'bg-[#eef1ff] text-[#1f2572] hover:bg-[#e0e7ff]',
  ghost: 'bg-transparent text-[#2f3476] hover:bg-[#edf0ff]',
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
        'inline-flex items-center justify-center rounded-xl font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#4f62ff] focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
        variantMap[variant],
        sizeMap[size],
        className,
      )}
      {...props}
    />
  )
}
