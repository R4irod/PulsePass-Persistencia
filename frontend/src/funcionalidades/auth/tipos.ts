import { z } from 'zod'

export const LoginSchema = z.object({
  identifier: z
    .string()
    .trim()
    .min(1, 'Escribe tu usuario o correo.')
    .max(150, 'El usuario o correo es demasiado largo.'),

  password: z
    .string()
    .min(1, 'Escribe tu contraseña.')
    .max(128, 'La contraseña es demasiado larga.'),
})

export type DatosLogin = z.infer<typeof LoginSchema>