import { isRouteErrorResponse, useRouteError } from 'react-router'
import Boton from '../componentes/Boton'
import EnlaceBoton from '../componentes/EnlaceBoton'
import PaginaEstado from '../componentes/PaginaEstado'
import { RUTAS } from '../lib/rutas'

export default function ErrorPagina() {
  const error = useRouteError()
  const noEncontrada = isRouteErrorResponse(error) && error.status === 404

  return (
    <PaginaEstado
      codigo={noEncontrada ? '404' : undefined}
      titulo={noEncontrada ? 'No encontramos esta página' : '¡Ups! Algo salió mal'}
      texto={noEncontrada ? 'Revisa la dirección o vuelve al inicio.' : 'Ocurrió un error inesperado. Inténtalo más tarde.'}
    >
      {!noEncontrada && <Boton onClick={() => window.location.reload()}>Reintentar</Boton>}
      <EnlaceBoton to={RUTAS.inicio} variante={noEncontrada ? 'primario' : 'secundario'}>
        Volver al inicio
      </EnlaceBoton>
    </PaginaEstado>
  )
}