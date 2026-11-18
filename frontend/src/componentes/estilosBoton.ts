import clsx from 'clsx'

export type Variante = 'primario' | 'secundario' | 'peligro' | 'fantasma' | 'claro' | 'contorno'

const variantes: Record<Variante, string> = {
  primario: 'bg-marca-600 text-white shadow-md shadow-marca-600/30 hover:bg-marca-700',
  secundario:
    'border border-slate-300 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800',
  peligro: 'bg-red-600 text-white hover:bg-red-700',
  fantasma: 'text-marca-600 hover:bg-marca-50 dark:text-marca-300 dark:hover:bg-slate-800',
  claro: 'bg-white text-marca-700 shadow-lg shadow-black/10 hover:bg-marca-50',
  contorno: 'border border-white/60 text-white hover:bg-white/10',
}

export function clasesBoton(variante: Variante, className?: string) {
  return clsx(
    'inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-5 text-sm font-semibold transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60',
    variantes[variante],
    className,
  )
}