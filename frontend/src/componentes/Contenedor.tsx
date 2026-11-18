import type { ComponentProps } from 'react'
import clsx from 'clsx'

interface ContenedorProps extends ComponentProps<'div'> {
  estrecho?: boolean
}

export default function Contenedor({ estrecho = false, className, ...resto }: ContenedorProps) {
  return (
    <div
      className={clsx('mx-auto w-full px-4 py-8', estrecho ? 'max-w-3xl' : 'max-w-6xl', className)}
      {...resto}
    />
  )
}