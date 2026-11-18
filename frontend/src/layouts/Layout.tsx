import { Outlet } from 'react-router'
import Navbar from './Navbar'
import Footer from './Footer'
import RedireccionSesion from '../funcionalidades/auth/RedireccionSesion'

export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a
        href="#contenido"
        className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:font-semibold focus:text-marca-700"
      >
        Saltar al contenido
      </a>
      <Navbar />
      <RedireccionSesion />
      <main id="contenido" className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
