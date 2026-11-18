export function fechaEvento(valor: string) {
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return 'Fecha no disponible'
  // La API envía LocalDateTime sin zona: conservamos la hora local del evento.
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(fecha)
}

export const numero = (valor: number) => new Intl.NumberFormat('es-CO').format(valor)
