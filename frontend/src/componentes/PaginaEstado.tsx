import type { ReactNode } from 'react'
import Contenedor from './Contenedor'
import Tarjeta from './Tarjeta'

interface PaginaEstadoProps {
  codigo?: string
  titulo: string
  texto: string
  children?: ReactNode
}

export default function PaginaEstado({ codigo, titulo, texto, children }: PaginaEstadoProps) {
  return (
    <Contenedor estrecho className="py-20">
      <Tarjeta className="p-10 text-center">
        {codigo && (
          <p className="font-titulo text-7xl font-extrabold text-marca-600 dark:text-marca-300">
            {codigo}
          </p>
        )}
        <h1 className="mt-4 font-titulo text-2xl font-bold">{titulo}</h1>
        <p className="mt-2 text-slate-600 dark:text-slate-400">{texto}</p>
        <div className="mt-6 flex flex-wrap justify-center gap-3">{children}</div>
      </Tarjeta>
    </Contenedor>
  )
}