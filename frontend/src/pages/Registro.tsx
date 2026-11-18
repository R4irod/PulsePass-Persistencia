import { Link, useLocation, useNavigate } from 'react-router'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'

import Contenedor from '../componentes/Contenedor'
import Tarjeta from '../componentes/Tarjeta'
import Campo from '../componentes/Campo'
import CampoContrasena from '../componentes/CampoContrasena'
import Boton from '../componentes/Boton'
import { RUTAS } from '../lib/rutas'
import {
  RegistroSchema,
  fechaHoyLocal,
  type DatosRegistro,
} from '../funcionalidades/auth/registro'
import { registrarUsuario } from '../funcionalidades/auth/api'
import { leerEstadoNavegacion } from '../funcionalidades/auth/navegacion'
import { mostrarErrorFormulario } from '../funcionalidades/auth/erroresFormulario'

const campos = [
  'username', 'email', 'firstName', 'lastName',
  'birthDate', 'password', 'phone', 'city',
] as const

export default function Registro() {
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const estado = leerEstadoNavegacion(ubicacion.state)
  const {
    register,
    control,
    handleSubmit,
    setError,
    clearErrors,
    formState: { errors, isSubmitting },
  } = useForm<DatosRegistro>({
    resolver: zodResolver(RegistroSchema),
    defaultValues: {
      username: '', email: '', firstName: '', lastName: '',
      phone: '', city: '', birthDate: '', password: '',
    },
  })
  const password = useWatch({ control, name: 'password' })
  const bytes = new TextEncoder().encode(password).length
  const orientacion = password.length < 12
    ? 'Añade más caracteres'
    : password.length < 16 ? 'Longitud aceptable' : 'Buena longitud'

  async function enviar(datos: DatosRegistro) {
    clearErrors()
    try {
      await registrarUsuario(datos)
      await navegar(RUTAS.login, {
        replace: true,
        state: { registrado: true, desde: estado.desde },
      })
    } catch (error: unknown) {
      mostrarErrorFormulario(error, setError, campos, 'registro')
    }
  }

  return (
    <Contenedor estrecho>
      <Tarjeta className="p-6 sm:p-10">
        <p className="text-sm font-semibold text-marca-600 dark:text-marca-300">
          Tu próximo evento empieza aquí
        </p>
        <h1 className="mt-2 text-3xl font-bold">Crea tu cuenta</h1>
        <p className="mb-7 mt-3 text-slate-600 dark:text-slate-300">
          Completa tus datos. Teléfono y ciudad son opcionales.
        </p>

        <form noValidate onSubmit={handleSubmit(enviar)} className="space-y-5">
          <fieldset disabled={isSubmitting} className="min-w-0 space-y-5">
            <legend className="sr-only">Datos de registro</legend>
            <div className="grid gap-5 sm:grid-cols-2">
              <Campo
                etiqueta="Nombre de usuario"
                autoComplete="username"
                maxLength={50}
                ayuda="Sin espacios ni @."
                error={errors.username?.message}
                {...register('username')}
              />
              <Campo
                etiqueta="Correo electrónico"
                type="email"
                autoComplete="email"
                maxLength={150}
                error={errors.email?.message}
                {...register('email')}
              />
              <Campo
                etiqueta="Nombres"
                autoComplete="given-name"
                maxLength={80}
                error={errors.firstName?.message}
                {...register('firstName')}
              />
              <Campo
                etiqueta="Apellidos"
                autoComplete="family-name"
                maxLength={80}
                error={errors.lastName?.message}
                {...register('lastName')}
              />
              <Campo
                etiqueta="Fecha de nacimiento"
                type="date"
                autoComplete="bday"
                max={fechaHoyLocal()}
                error={errors.birthDate?.message}
                {...register('birthDate')}
              />
              <Campo
                etiqueta="Teléfono (opcional)"
                type="tel"
                autoComplete="tel"
                maxLength={30}
                error={errors.phone?.message}
                {...register('phone')}
              />
              <Campo
                etiqueta="Ciudad (opcional)"
                autoComplete="address-level2"
                maxLength={100}
                error={errors.city?.message}
                {...register('city')}
              />
            </div>

            <CampoContrasena
              etiqueta="Contraseña"
              autoComplete="new-password"
              ayuda="Mínimo 12 caracteres y máximo 72 bytes."
              error={errors.password?.message}
              {...register('password')}
            />
            <div className="text-sm text-slate-600 dark:text-slate-300">
              <p>{orientacion} · {bytes}/72 bytes</p>
              <meter
                aria-label="Longitud orientativa de la contraseña"
                min={0}
                max={20}
                low={12}
                high={16}
                optimum={20}
                value={Math.min(password.length, 20)}
                className="mt-2 h-3 w-full"
              />
              <p className="mt-1 text-xs">
                Usa una contraseña única. La longitud no garantiza su seguridad.
              </p>
            </div>

            {errors.root && (
              <p role="alert" className="text-sm text-red-700 dark:text-red-300">
                {errors.root.message}
              </p>
            )}
            <Boton type="submit" cargando={isSubmitting} className="w-full">
              Crear cuenta
            </Boton>
          </fieldset>
        </form>

        <p className="mt-6 text-sm text-slate-600 dark:text-slate-300">
          ¿Ya tienes cuenta?{' '}
          <Link
            to={RUTAS.login}
            state={{ desde: estado.desde }}
            className="font-semibold text-marca-700 underline dark:text-marca-300"
          >
            Inicia sesión
          </Link>
        </p>
      </Tarjeta>
    </Contenedor>
  )
}
