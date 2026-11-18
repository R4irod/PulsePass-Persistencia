import { useLocation } from 'react-router'
import Contenedor from '../componentes/Contenedor'

export default function Pendiente() {
  const { pathname } = useLocation()
  return (
    <Contenedor>
      <h1 className="font-titulo text-3xl font-bold">En construcción</h1>
      <p className="mt-2 text-slate-600 dark:text-slate-400">
        La página <code>{pathname}</code> se construye en el siguiente paso.
      </p>
    </Contenedor>
  )
}