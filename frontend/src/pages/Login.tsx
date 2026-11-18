import { Link, useLocation } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'

import Contenedor from '../componentes/Contenedor'
import Tarjeta from '../componentes/Tarjeta'
import Campo from '../componentes/Campo'
import CampoContrasena from '../componentes/CampoContrasena'
import Boton from '../componentes/Boton'
import { RUTAS } from '../lib/rutas'
import { useAuth } from '../funcionalidades/auth/useAuth'
import { LoginSchema, type DatosLogin } from '../funcionalidades/auth/tipos'
import { leerEstadoNavegacion } from '../funcionalidades/auth/navegacion'
import { mostrarErrorFormulario } from '../funcionalidades/auth/erroresFormulario'

export default function Login() {
  const { iniciarSesion, sesion } = useAuth()
  const ubicacion = useLocation()
  const estado = leerEstadoNavegacion(ubicacion.state)
  const {
    register,
    handleSubmit,
    setError,
    clearErrors,
    formState: { errors, isSubmitting },
  } = useForm<DatosLogin>({
    resolver: zodResolver(LoginSchema),
    defaultValues: { identifier: '', password: '' },
  })

  async function enviar(datos: DatosLogin) {
    clearErrors()
    try {
      await iniciarSesion(datos)
      // SoloAnonimo devuelve a la ruta de origen cuando cambia la sesión.
    } catch (error: unknown) {
      mostrarErrorFormulario(
        error,
        setError,
        ['identifier', 'password'],
        'login',
      )
    }
  }

  const expirada = estado.motivo === 'sesion-expirada' ||
    (sesion.estado === 'anonimo' && sesion.motivo === 'sesion-expirada')

  return (
    <Contenedor estrecho>
      <Tarjeta className="mx-auto max-w-lg p-6 sm:p-10">
        <p className="text-sm font-semibold text-marca-600 dark:text-marca-300">
          Bienvenido a PulsePass
        </p>
        <h1 className="mt-2 text-3xl font-bold">Inicia sesión</h1>
        <p className="mb-7 mt-3 text-slate-600 dark:text-slate-300">
          Entra con tu usuario o correo electrónico.
        </p>

        {estado.registrado && (
          <p role="status" className="mb-5 text-emerald-700 dark:text-emerald-300">
            Cuenta creada. Inicia sesión para continuar.
          </p>
        )}
        {expirada && (
          <p role="status" className="mb-5 text-amber-800 dark:text-amber-300">
            Tu sesión expiró. Inicia sesión de nuevo.
          </p>
        )}

        <form noValidate onSubmit={handleSubmit(enviar)} className="space-y-5">
          <fieldset disabled={isSubmitting} className="min-w-0 space-y-5">
            <legend className="sr-only">Datos de acceso</legend>
            <Campo
              etiqueta="Usuario o correo"
              autoComplete="username"
              maxLength={150}
              error={errors.identifier?.message}
              {...register('identifier')}
            />
            <CampoContrasena
              etiqueta="Contraseña"
              autoComplete="current-password"
              maxLength={128}
              error={errors.password?.message}
              {...register('password')}
            />
            {errors.root && (
              <p role="alert" className="text-sm text-red-700 dark:text-red-300">
                {errors.root.message}
              </p>
            )}
            <Boton type="submit" cargando={isSubmitting} className="w-full">
              Ingresar
            </Boton>
          </fieldset>
        </form>

        <p className="mt-6 text-sm text-slate-600 dark:text-slate-300">
          ¿No tienes cuenta?{' '}
          <Link
            to={RUTAS.registro}
            state={{ desde: estado.desde }}
            className="font-semibold text-marca-700 underline dark:text-marca-300"
          >
            Regístrate
          </Link>
        </p>
      </Tarjeta>
    </Contenedor>
  )
}
