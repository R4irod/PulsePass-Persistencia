import type { ReactNode } from 'react'
import { AlertCircle, SearchX } from 'lucide-react'
import { ApiError } from '../lib/http'
import Boton from './Boton'
import Tarjeta from './Tarjeta'

export function Cargando() {
  return (
    <div

      role="status"

      aria-label="Cargando contenido"

      className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3"

    >
      {[1, 2, 3].map(id => (
        <Tarjeta

          key={id}

          className="overflow-hidden p-5"

          aria-hidden="true"

        >
          <div className="h-40 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
          <div className="mt-5 h-5 w-3/4 rounded bg-slate-200 dark:bg-slate-800" />
          <div className="mt-3 h-4 w-1/2 rounded bg-slate-200 dark:bg-slate-800" />
        </Tarjeta>
      ))}


    </div>
  )
}

interface EstadoErrorProps {
  error: unknown;

  reintentar: () => void;

  noDisponible?: string
}
export function EstadoError({ error, reintentar, noDisponible = 'Contenido no disponible' }: EstadoErrorProps) {
  const ausente = error instanceof ApiError && [403, 404].includes(error.status)
  return (
    <Tarjeta role="alert" className="p-6 text-center">
      <AlertCircle

        aria-hidden="true"

        className="mx-auto mb-3 size-8 text-rose-600 dark:text-rose-400"

      />
      <h2 className="text-lg font-semibold">
        {ausente ? noDisponible : 'No pudimos cargar la información'}
      </h2>
      <p className="my-3 text-sm text-slate-600 dark:text-slate-300">
        {ausente

          ? 'Vuelve al listado para explorar otras opciones.'

          : error instanceof ApiError ? error.message : 'Inténtalo de nuevo en un momento.'}

      </p>
      {!ausente && <Boton variante="secundario" onClick={reintentar}>
        Reintentar
      </Boton>}


    </Tarjeta>
  )
}

interface EstadoVacioProps {
  titulo: string;

  children?: ReactNode
}
export function EstadoVacio({ titulo, children }: EstadoVacioProps) {
  return (
    <Tarjeta className="p-10 text-center">
      <SearchX aria-hidden="true" className="mx-auto mb-4 size-9 text-marca-500" />
      <h2 className="mb-4 text-lg font-semibold">
        {titulo}
      </h2>
      {children}

    </Tarjeta>
  )
}
