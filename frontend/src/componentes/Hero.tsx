import EnlaceBoton from './EnlaceBoton'
import { RUTAS } from '../lib/rutas'
import { useAuth } from '../funcionalidades/auth/useAuth'

export default function Hero() {
  const { sesion } = useAuth()
  return (
    <section className="relative overflow-hidden bg-linear-to-br from-marca-700 via-marca-600 to-acento-500 text-white">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-28 top-1/2 hidden size-136 -translate-y-1/2 md:block"
      >
        {[0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className="absolute inset-0 animate-pulso rounded-full border border-white/40"
            style={{ animationDelay: `${i * 0.9}s` }}
          />
        ))}
        <span className="absolute inset-[40%] rounded-full bg-white/25 blur-md" />
      </div>

      <div className="relative mx-auto max-w-6xl px-4 py-20 sm:py-28">
        <h1 className="max-w-xl font-titulo text-4xl leading-tight font-extrabold sm:text-6xl">
          Descubre los mejores eventos
        </h1>
        <p className="mt-4 max-w-md text-lg text-white/85">
          Música, cultura, deporte y tecnología en un solo lugar.
        </p>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <EnlaceBoton to={RUTAS.eventos} variante="claro">
            Ver cartelera
          </EnlaceBoton>
          {sesion.estado === 'anonimo' && (
            <EnlaceBoton to={RUTAS.registro} variante="contorno">
              Crear cuenta
            </EnlaceBoton>
          )}
        </div>
      </div>
    </section>
  )
}
