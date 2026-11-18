import { useRef, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router'
import clsx from 'clsx'
import { Menu, X, ChevronDown } from 'lucide-react'
import { RUTAS } from '../lib/rutas'
import Logo from '../componentes/Logo'
import BotonTema from '../componentes/BotonTema'
import Boton from '../componentes/Boton'
import EnlaceBoton from '../componentes/EnlaceBoton'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { enlacesPorRol } from '../funcionalidades/auth/enlaces'

function claseEnlace({ isActive }: { isActive: boolean }) {
  return clsx(
    'inline-flex min-h-11 items-center rounded-lg px-3 py-2 text-sm font-medium',
    isActive
      ? 'bg-marca-50 text-marca-700 dark:bg-marca-500/15 dark:text-marca-300'
      : 'text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white',
  )
}

export default function Navbar() {
  const [abierto, setAbierto] = useState(false)
  const menuUsuario = useRef<HTMLDetailsElement>(null)
  const { sesion, cerrarSesion } = useAuth()
  const navegar = useNavigate()
  const usuario = sesion.estado === 'autenticado' ? sesion.usuario : null
  const enlaces = enlacesPorRol(usuario?.roles ?? [])

  function cerrarMenus() {
    setAbierto(false)
    if (menuUsuario.current) menuUsuario.current.open = false
  }

  function salir() {
    cerrarMenus()
    cerrarSesion()
    void navegar(RUTAS.inicio, { replace: true })
  }

  return (
    <header
      className="sticky top-0 z-40 border-b border-slate-200 bg-white/95
        backdrop-blur-lg dark:border-slate-800 dark:bg-slate-950/95"
      onKeyDown={evento => {
        if (evento.key === 'Escape') {
          cerrarMenus()
          menuUsuario.current?.querySelector('summary')?.focus()
        }
      }}
    >
      <div className="mx-auto flex min-h-16 max-w-6xl flex-wrap items-center gap-3 px-4 py-2">
        <Link
          to={RUTAS.inicio}
          aria-label="PulsePass, ir al inicio"
          onClick={cerrarMenus}
          className="mr-auto"
        >
          <Logo />
        </Link>

        <BotonTema />
        {usuario ? (
          <details ref={menuUsuario} className="relative">
            <summary
              className="flex min-h-11 max-w-52 cursor-pointer list-none
                items-center gap-2 rounded-xl px-3 text-sm font-semibold"
            >
              <span
                aria-hidden="true"
                className="flex size-8 shrink-0 items-center justify-center
                  rounded-full bg-marca-100 text-marca-700"
              >
                {usuario.username.slice(0, 2).toUpperCase()}
              </span>
              <span className="truncate">{usuario.username}</span>
              <ChevronDown aria-hidden="true" className="size-4 shrink-0" />
            </summary>
            <div
              className="absolute right-0 mt-2 grid w-52 gap-1 rounded-xl
                border border-slate-200 bg-white p-2 shadow-lg
                dark:border-slate-700 dark:bg-slate-900"
            >
              <NavLink to={RUTAS.cuenta} className={claseEnlace} onClick={cerrarMenus}>
                Mi cuenta
              </NavLink>
              {usuario.roles.includes('USER') && (
                <NavLink
                  to={RUTAS.misTickets}
                  className={claseEnlace}
                  onClick={cerrarMenus}
                >
                  Mis tickets
                </NavLink>
              )}
              <Boton variante="fantasma" onClick={salir}>Cerrar sesión</Boton>
            </div>
          </details>
        ) : (
          <div className="hidden items-center gap-2 sm:flex">
            <EnlaceBoton to={RUTAS.login} variante="fantasma">Ingresar</EnlaceBoton>
            <EnlaceBoton to={RUTAS.registro}>Registrarse</EnlaceBoton>
          </div>
        )}

        <button
          type="button"
          onClick={() => setAbierto(valor => !valor)}
          aria-expanded={abierto}
          aria-controls="menu-movil"
          aria-label={abierto ? 'Cerrar menú' : 'Abrir menú'}
          className="inline-flex size-11 items-center justify-center rounded-xl lg:hidden"
        >
          {abierto ? (
            <X className="size-6" aria-hidden="true" />
          ) : (
            <Menu className="size-6" aria-hidden="true" />
          )}
        </button>

        <nav aria-label="Principal" className="hidden w-full flex-wrap gap-1 lg:flex">
          {enlaces.map(enlace => (
            <NavLink key={enlace.a} to={enlace.a} className={claseEnlace}>
              {enlace.texto}
            </NavLink>
          ))}
        </nav>
      </div>

      {abierto && (
        <nav
          id="menu-movil"
          aria-label="Menú móvil"
          className="border-t border-slate-200 px-4 pb-4 lg:hidden dark:border-slate-800"
        >
          <div className="flex flex-col py-2">
            {enlaces.map(enlace => (
              <NavLink
                key={enlace.a}
                to={enlace.a}
                className={claseEnlace}
                onClick={cerrarMenus}
              >
                {enlace.texto}
              </NavLink>
            ))}
          </div>
          {!usuario && (
            <div className="flex gap-2">
              <EnlaceBoton to={RUTAS.login} variante="secundario" onClick={cerrarMenus}>
                Ingresar
              </EnlaceBoton>
              <EnlaceBoton to={RUTAS.registro} onClick={cerrarMenus}>
                Registrarse
              </EnlaceBoton>
            </div>
          )}
        </nav>
      )}
    </header>
  )
}
