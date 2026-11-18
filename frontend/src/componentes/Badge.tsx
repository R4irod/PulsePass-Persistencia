import clsx from 'clsx'
import { etiquetaEstado, type EstadoVisual } from '../lib/etiquetas'

const tonos: Record<EstadoVisual, string> = {
  DRAFT: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200',
  PUBLISHED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300',
  SOLD_OUT: 'bg-amber-100 text-amber-800 dark:bg-amber-500/20 dark:text-amber-300',
  CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-500/20 dark:text-red-300',
  FINISHED: 'bg-slate-300 text-slate-800 dark:bg-slate-600 dark:text-slate-100',
  RESERVED: 'bg-sky-100 text-sky-800 dark:bg-sky-500/20 dark:text-sky-300',
  PAID: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300',
  USED: 'bg-slate-300 text-slate-800 dark:bg-slate-600 dark:text-slate-100',
}

interface BadgeProps {
  tono: EstadoVisual
  className?: string
}

export default function Badge({ tono, className }: BadgeProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold tracking-wide',
        tonos[tono],
        className,
      )}
    >
      {etiquetaEstado[tono]}
    </span>
  )
}