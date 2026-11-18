import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import Contenedor from '../componentes/Contenedor'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import EnlaceBoton from '../componentes/EnlaceBoton'
import Tarjeta from '../componentes/Tarjeta'
import { RUTAS } from '../lib/rutas'
import { leerEventosRecientes } from '../funcionalidades/eventos/admin'

export default function AdminEventos() {
  const [codigo, setCodigo] = useState('')
  const [recientes] = useState(leerEventosRecientes)
  const navegar = useNavigate()

  return (
    <Contenedor estrecho>
      <h1 className="mb-3 text-3xl font-bold">Administrar eventos</h1>
      <p className="mb-6">Crea un evento o busca su código para gestionarlo.</p>
      <EnlaceBoton to={RUTAS.adminNuevo}>Crear evento</EnlaceBoton>
      <form className="my-8 flex flex-wrap items-end gap-3" onSubmit={evento => {
        evento.preventDefault()
        if (codigo.trim()) void navegar(RUTAS.adminEvento(codigo.trim()))
      }}>
        <Campo etiqueta="Código del evento" required maxLength={30}
          className="min-w-0 flex-1" value={codigo}
          onChange={evento => setCodigo(evento.target.value)} />
        <Boton type="submit">Buscar</Boton>
      </form>
      <Tarjeta className="space-y-3 p-6">
        <h2 className="text-xl font-bold">Creados en esta pestaña</h2>
        <p className="text-sm">Estos accesos se borran al cerrar la pestaña. Puedes volver a buscar los eventos por su código.</p>
        {recientes.length === 0 ? <p>Aún no has creado eventos en esta pestaña.</p> : (
          <ul className="space-y-3">
            {recientes.map(codigo => (
              <li key={codigo}><Link className="break-all underline" to={RUTAS.adminEvento(codigo)}>{codigo}</Link></li>
            ))}
          </ul>
        )}
      </Tarjeta>
    </Contenedor>
  )
}
