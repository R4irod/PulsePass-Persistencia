import { Music } from 'lucide-react'

export default function Logo() {
  return (
    <span className="inline-flex items-center gap-2.5">
      <span className="grid size-9 place-items-center rounded-xl bg-linear-to-br from-marca-500 to-acento-500 text-white shadow-md shadow-marca-600/30">
        <Music className="size-5" aria-hidden="true" />
      </span>
      <span className="font-titulo text-lg font-bold tracking-tight">PulsePass</span>
    </span>
  )
}