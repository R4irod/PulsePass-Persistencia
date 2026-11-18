import type { ComponentProps } from 'react'
import clsx from 'clsx'

export default function Tarjeta({ className, ...resto }: ComponentProps<'div'>) {
  return (
    <div
      className={clsx(
        'rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900',
        className,
      )}
      {...resto}
    />
  )
}