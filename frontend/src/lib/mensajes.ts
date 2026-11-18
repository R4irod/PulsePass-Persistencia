type Regla = [RegExp, string | ((m: RegExpMatchArray) => string)]

const reglas: Regla[] = [
  // Seguridad y errores generales
  [/^Invalid credentials/i, 'Credenciales inválidas.'],
  [/^Authentication is required/i, 'Tu sesión expiró. Inicia sesión de nuevo.'],
  [/^You do not have permission/i, 'No tienes permisos para realizar esta acción.'],
  [/^Internal server error/i, 'Ocurrió un error inesperado. Inténtalo más tarde.'],
  [/^Validation failed/i, 'Revisa los campos del formulario.'],
  [/^Malformed JSON/i, 'La solicitud no tiene un formato válido.'],

  // No encontrado
  [/^Event not found/i, 'No encontramos este evento.'],
  [/^Ticket not found/i, 'No encontramos este ticket.'],
  [/^User not found/i, 'No encontramos este usuario.'],
  [/^Artist not found/i, 'No encontramos este artista.'],
  [/^Venue not found/i, 'No encontramos esta sede.'],

  // Usuarios
  [/^(Username|Email) already exists/i, 'El usuario o correo ya está registrado.'],
  [/^Birth date cannot be in the future/i, 'La fecha de nacimiento no puede ser futura.'],

  // Compra de tickets
  [/^User is not active/i, 'Tu cuenta está inactiva.'],
  [/^Cannot purchase ticket for event with status/i, 'Este evento no está disponible para comprar tickets.'],
  [/^Cannot purchase ticket for an event that already occurred/i, 'Este evento ya ocurrió.'],
  [/minimum age: birth date unknown/i, 'No podemos verificar tu edad con los datos de tu cuenta.'],
  [/minimum age of (\d+)/i, (m) => `Debes tener al menos ${m[1]} años para asistir a este evento.`],
  [/^No capacity available/i, 'Ya no hay entradas disponibles para este evento.'],

  // Cancelar y usar tickets
  [/^Cannot cancel ticket after the event/i, 'No puedes cancelar un ticket de un evento que ya ocurrió.'],
  [/^Cannot cancel ticket with status/i, 'Este ticket no se puede cancelar en su estado actual.'],
  [/^Cannot mark ticket as used with status USED/i, 'El ticket ya fue utilizado.'],
  [/^Cannot mark ticket as used/i, 'Este ticket no se puede usar en su estado actual.'],

  // Eventos
  [/^Event code already exists/i, 'Ya existe un evento con ese código.'],
  [/^Cannot create event in inactive venue/i, 'La sede seleccionada no está activa.'],
  [/^Event date must be in the future/i, 'La fecha del evento debe ser futura.'],
  [/^Minimum age cannot be negative/i, 'La edad mínima no puede ser negativa.'],
  [/^Only DRAFT events can be published/i, 'Solo los eventos en borrador se pueden publicar.'],
  [/^Cannot publish an event with a past date/i, 'No puedes publicar un evento con fecha pasada.'],
  [/^Cannot publish event because venue/i, 'No se puede publicar: la sede ya no está activa.'],
  [/^Cannot add artists to an event/i, 'No se pueden agregar artistas a un evento en este estado.'],
  [/is already associated with this event/i, 'Ese artista ya está asociado al evento.'],
]

const porEstado: Record<number, string> = {
  400: 'Revisa los datos enviados.',
  401: 'Tu sesión expiró. Inicia sesión de nuevo.',
  403: 'No tienes permisos para realizar esta acción.',
  404: 'No encontramos lo que buscas.',
  409: 'No se pudo completar la operación.',
  500: 'Ocurrió un error inesperado. Inténtalo más tarde.',
}

export function traducirMensaje(mensaje: string, status: number): string {
  for (const [patron, texto] of reglas) {
    const coincidencia = mensaje.match(patron)
    if (coincidencia) {
      return typeof texto === 'function' ? texto(coincidencia) : texto
    }
  }
  return porEstado[status] ?? 'Ocurrió un error inesperado. Inténtalo más tarde.'
}