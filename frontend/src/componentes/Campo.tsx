import { useId } from 'react'
import type { ComponentProps } from 'react'
import clsx from 'clsx'
import { AlertTriangle } from 'lucide-react'

interface CampoProps extends ComponentProps<'input'> {
  etiqueta: string
  error?: string
  ayuda?: string
}

export default function Campo({
  etiqueta,
  error,
  ayuda,
  className,
  id,
  ...resto
}: CampoProps) {
  const idGenerado = useId()
  const idCampo = id ?? idGenerado
  const idMensaje = `${idCampo}-mensaje`

  return (
    <div className={className}>
      <label
        htmlFor={idCampo}
        className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300"
      >
        {etiqueta}
      </label>
      <input
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || ayuda ? idMensaje : undefined}
        className={clsx(
          'block min-h-11 w-full rounded-xl border bg-white px-4 text-sm text-slate-900 placeholder:text-slate-400 dark:bg-slate-900 dark:text-slate-100',
          error
            ? 'border-red-500'
            : 'border-slate-300 dark:border-slate-700',
        )}
        {...resto}
      />
      {error ? (
        <p
          id={idMensaje}
          className="mt-1.5 flex items-center gap-1.5 text-sm text-red-600 dark:text-red-400"
        >
          <AlertTriangle className="size-4 shrink-0" aria-hidden="true" />
          {error}
        </p>
      ) : ayuda ? (
        <p id={idMensaje} className="mt-1.5 text-xs text-slate-500 dark:text-slate-400">
          {ayuda}
        </p>
      ) : null}
    </div>
  )
}