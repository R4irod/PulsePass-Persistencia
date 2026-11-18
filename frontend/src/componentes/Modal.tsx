import { useEffect, useId, useRef, type ReactNode } from 'react'

interface ModalProps {
  abierto: boolean
  titulo: string
  onCerrar: () => void
  children: ReactNode
  ocupado?: boolean
}

export default function Modal({
  abierto, titulo, onCerrar, children, ocupado = false,
}: ModalProps) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const tituloId = useId()

  useEffect(() => {
    const elemento = dialogo.current
    if (!elemento) return
    if (abierto && !elemento.open) elemento.showModal()
    if (!abierto && elemento.open) elemento.close()
  }, [abierto])

  return (
    <dialog
      ref={dialogo}
      aria-labelledby={tituloId}
      onCancel={(evento) => {
        evento.preventDefault()
        if (!ocupado) onCerrar()
      }}
      className="m-auto max-h-[90dvh] w-[calc(100%_-_2rem)] max-w-lg
        overflow-y-auto rounded-2xl bg-white p-6 text-slate-900
        shadow-xl backdrop:bg-black/50 dark:bg-slate-900 dark:text-slate-100"
    >
      <h2 id={tituloId} className="mb-5 text-xl font-bold">{titulo}</h2>
      {children}
    </dialog>
  )
}
