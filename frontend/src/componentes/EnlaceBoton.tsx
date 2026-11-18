import { Link, type LinkProps } from 'react-router'
import { clasesBoton, type Variante } from './estilosBoton'

type EnlaceBotonProps = Omit<LinkProps, 'className'> & {
  variante?: Variante
  className?: string
}

export default function EnlaceBoton({ variante = 'primario', className, ...resto }: EnlaceBotonProps) {
  return <Link className={clasesBoton(variante, className)} {...resto} />
}