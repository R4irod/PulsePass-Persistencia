import EnlaceBoton from '../componentes/EnlaceBoton'
import PaginaEstado from '../componentes/PaginaEstado'
import { RUTAS } from '../lib/rutas'

export default function NoEncontrada() {
  return (
    <PaginaEstado codigo="404" titulo="No encontramos esta página" texto="Revisa la dirección o vuelve al inicio.">
      <EnlaceBoton to={RUTAS.inicio}>Volver al inicio</EnlaceBoton>
    </PaginaEstado>
  )
}