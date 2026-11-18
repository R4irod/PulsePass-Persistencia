import { z } from 'zod'

const CLAVE_SESION = 'pulsepass.sesion'

const SesionGuardadaSchema = z.object({
  accessToken: z.string().min(1),
  expiraEn: z.number().int().positive(),
})

type SesionGuardada = z.infer<typeof SesionGuardadaSchema>

export function guardarSesion(
  accessToken: string,
  expiresIn: number,
): SesionGuardada {
  const sesion = SesionGuardadaSchema.parse({
    accessToken,
    expiraEn: Date.now() + expiresIn * 1000,
  })

  try {
    sessionStorage.setItem(
      CLAVE_SESION,
      JSON.stringify(sesion),
    )
  } catch {
    // Si el navegador bloquea el almacenamiento,
    // podremos mantener la sesión solo en memoria.
  }

  return sesion
}

export function eliminarSesionGuardada() {
  try {
    sessionStorage.removeItem(CLAVE_SESION)
  } catch {
    // El navegador puede bloquear el almacenamiento.
  }
}

export function leerSesionGuardada(): SesionGuardada | null {
  try {
    const contenido = sessionStorage.getItem(CLAVE_SESION)

    if (!contenido) {
      return null
    }

    const datos: unknown = JSON.parse(contenido)
    const resultado = SesionGuardadaSchema.safeParse(datos)

    if (!resultado.success) {
      eliminarSesionGuardada()
      return null
    }

    const sesion = resultado.data

    if (sesion.expiraEn <= Date.now()) {
      eliminarSesionGuardada()
      return null
    }

    return sesion
  } catch {
    eliminarSesionGuardada()
    return null
  }
}