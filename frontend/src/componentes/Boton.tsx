import type { ComponentProps } from 'react'
import { Loader2 } from 'lucide-react'
import { clasesBoton, type Variante } from './estilosBoton'

interface BotonProps extends ComponentProps<'button'> {
  variante?: Variante
  cargando?: boolean
}

export default function Boton({
  variante = 'primario',
  cargando = false,
  type = 'button',
  disabled,
  className,
  children,
  ...resto
}: BotonProps) {
  return (
    <button
      type={type}
      disabled={disabled || cargando}
      className={clasesBoton(variante, className)}
      {...resto}
    >
      {cargando && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
      {cargando ? 'Cargando…' : children}
    </button>
  )
}