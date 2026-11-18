import { useState } from 'react'
import { Moon, Sun } from 'lucide-react'

export default function BotonTema() {
  const [oscuro, setOscuro] = useState(() => document.documentElement.classList.contains('dark'))

  function alternar() {
    const siguiente = !oscuro
    document.documentElement.classList.toggle('dark', siguiente)
    try {
      localStorage.setItem('tema', siguiente ? 'oscuro' : 'claro')
    } catch {
      // Si el navegador bloquea el almacenamiento, el tema solo dura esta visita.
    }
    setOscuro(siguiente)
  }

  const Icono = oscuro ? Sun : Moon

  return (
    <button
      type="button"
      onClick={alternar}
      aria-label={oscuro ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
      className="inline-flex size-11 items-center justify-center rounded-xl text-slate-600 transition hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
    >
      <Icono className="size-5" aria-hidden="true" />
    </button>
  )
}