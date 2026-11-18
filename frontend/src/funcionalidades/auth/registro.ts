import { z } from 'zod'

export function fechaHoyLocal() {
  const hoy = new Date()
  return [
    hoy.getFullYear(),
    String(hoy.getMonth() + 1).padStart(2, '0'),
    String(hoy.getDate()).padStart(2, '0'),
  ].join('-')
}

export const RegistroSchema = z.object({
  username: z.string().trim()
    .min(1, 'Escribe un nombre de usuario.')
    .max(50, 'Usa máximo 50 caracteres.')
    .regex(/^[^@\s]+$/, 'No uses espacios ni @ en el usuario.'),
  email: z.string().trim()
    .email('Escribe un correo válido.')
    .max(150, 'Usa máximo 150 caracteres.'),
  firstName: z.string().trim()
    .min(1, 'Escribe tus nombres.')
    .max(80, 'Usa máximo 80 caracteres.'),
  lastName: z.string().trim()
    .min(1, 'Escribe tus apellidos.')
    .max(80, 'Usa máximo 80 caracteres.'),
  phone: z.string().trim().max(30, 'Usa máximo 30 caracteres.'),
  city: z.string().trim().max(100, 'Usa máximo 100 caracteres.'),
  birthDate: z.iso.date('Escribe una fecha válida.')
    .refine(fecha => fecha <= fechaHoyLocal(), {
      message: 'La fecha de nacimiento no puede ser futura.',
    }),
  password: z.string()
    .min(12, 'Usa al menos 12 caracteres.')
    .refine(valor => valor.trim().length > 0, {
      message: 'La contraseña no puede contener solo espacios.',
    })
    .refine(valor => new TextEncoder().encode(valor).length <= 72, {
      message: 'Usa máximo 72 bytes; las tildes y emojis ocupan más de uno.',
    }),
})

export type DatosRegistro = z.infer<typeof RegistroSchema>
