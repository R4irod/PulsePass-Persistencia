import { Music2, Trophy, Cpu, GraduationCap, Palette, Sparkles, type LucideIcon } from 'lucide-react'
import clsx from 'clsx'
import type { Categoria } from '../../lib/esquemas'
import { etiquetaCategoria } from '../../lib/etiquetas'

const estilos: Record<Categoria, { fondo: string; icono: LucideIcon }> = {
  MUSIC: {
    fondo:
      'from-indigo-100 to-violet-200 text-indigo-800 ' +
      'dark:from-indigo-950 dark:to-violet-900 dark:text-indigo-200',
    icono: Music2
  },

  SPORTS: {
    fondo:
      'from-emerald-100 to-teal-200 text-emerald-800 ' +
      'dark:from-emerald-950 dark:to-teal-900 dark:text-emerald-200',
    icono: Trophy
  },

  TECHNOLOGY: {
    fondo:
      'from-sky-100 to-blue-200 text-blue-800 ' +
      'dark:from-sky-950 dark:to-blue-900 dark:text-sky-200',
    icono: Cpu
  },

  EDUCATION: {
    fondo:
      'from-amber-100 to-orange-200 text-amber-900 ' +
      'dark:from-amber-950 dark:to-orange-900 dark:text-amber-200',
    icono: GraduationCap
  },

  CULTURE: {
    fondo:
      'from-rose-100 to-pink-200 text-rose-800 ' +
      'dark:from-rose-950 dark:to-pink-900 dark:text-rose-200',
    icono: Palette
  },

  ENTERTAINMENT: {
    fondo:
      'from-fuchsia-100 to-purple-200 text-purple-800 ' +
      'dark:from-fuchsia-950 dark:to-purple-900 dark:text-fuchsia-200',
    icono: Sparkles
  },
}

interface PanelCategoriaProps {
  categoria: Categoria;
  grande?: boolean
}
export default function PanelCategoria({ categoria, grande = false }: PanelCategoriaProps) {
  const { fondo, icono: Icono } = estilos[categoria]
  return (
    <div
      className={clsx(
        'relative flex items-center justify-center overflow-hidden bg-linear-to-br',
        fondo,
        grande ? 'h-56 sm:h-72' : 'h-44'
      )}
    >
      <div
        aria-hidden="true"
        className="absolute size-64 rounded-full border border-current opacity-10"
      />
      <div
        aria-hidden="true"
        className="absolute size-44 rounded-full border border-current opacity-10"
      />
      <Icono
        aria-hidden="true"
        className={grande ? 'size-20' : 'size-14'}
        strokeWidth={1.25}
      />
      <span className="absolute bottom-4 left-5 text-xs font-bold uppercase tracking-widest">
        {etiquetaCategoria[categoria]}
      </span>
    </div>
  )
}
