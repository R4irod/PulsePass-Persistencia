import EnlaceBoton from '../componentes/EnlaceBoton'
import PaginaEstado from '../componentes/PaginaEstado'
import { RUTAS } from '../lib/rutas'

export default function SinPermisos() {
  return (
    <PaginaEstado codigo="403" titulo="Sin permisos" texto="No tienes permisos para acceder a esta sección.">
      <EnlaceBoton to={RUTAS.inicio}>Volver al inicio</EnlaceBoton>
    </PaginaEstado>
  )
}