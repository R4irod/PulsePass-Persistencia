import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { z } from 'zod'
import { http } from '../lib/http'
import { RolSchema, type Rol } from '../lib/esquemas'
import { etiquetaRol } from '../lib/etiquetas'
import Contenedor from '../componentes/Contenedor'
import Campo from '../componentes/Campo'
import Boton from '../componentes/Boton'
import Tarjeta from '../componentes/Tarjeta'
import Modal from '../componentes/Modal'

const CuentaRolesSchema = z.object({
  id: z.number(), username: z.string(), email: z.string(), roles: z.array(RolSchema),
})

export default function AdminRoles() {
  const [identificador, setIdentificador] = useState('')
  const [rol, setRol] = useState<Rol>('STAFF')
  const [confirmar, setConfirmar] = useState(false)
  const buscar = useMutation({
    mutationFn: (identifier: string) => http('/admin/users/lookup', CuentaRolesSchema, {
      parametros: { identifier },
    }),
  })
  const asignar = useMutation({
    mutationFn: ({ id, role }: { id: number; role: Rol }) =>
      http(`/admin/users/${id}/roles`, CuentaRolesSchema, {
        metodo: 'POST', cuerpo: { role },
      }),
    onSuccess: () => setConfirmar(false),
  })
  const cuenta = asignar.data ?? buscar.data

  return (
    <Contenedor estrecho>
      <h1 className="mb-3 text-3xl font-bold">Roles de usuarios</h1>
      <p className="mb-6">Busca una cuenta por usuario o correo para añadirle un rol.</p>
      <form className="mb-6 flex flex-wrap items-end gap-3" onSubmit={evento => {
        evento.preventDefault()
        if (!identificador.trim()) return
        asignar.reset()
        buscar.mutate(identificador.trim())
      }}>
        <Campo etiqueta="Usuario o correo" className="min-w-0 flex-1"
          required value={identificador} maxLength={150}
          onChange={evento => setIdentificador(evento.target.value)} />
        <Boton type="submit" cargando={buscar.isPending}>Buscar</Boton>
      </form>
      {buscar.isError && <p role="alert">{buscar.error.message}</p>}
      {!buscar.isPending && !buscar.isError && cuenta && (
        <Tarjeta className="space-y-4 p-6">
          <h2 className="text-xl font-bold">{cuenta.username}</h2>
          <p className="break-all">{cuenta.email}</p>
          <p>Roles actuales: {cuenta.roles.map(r => etiquetaRol[r]).join(', ')}</p>
          <label className="block" htmlFor="nuevo-rol">Rol que deseas añadir</label>
          <select id="nuevo-rol" value={rol}
            onChange={evento => setRol(RolSchema.parse(evento.target.value))}
            className="min-h-11 rounded-lg border p-2 dark:bg-slate-900">
            {RolSchema.options.map(r => <option key={r} value={r}>{etiquetaRol[r]}</option>)}
          </select>
          <div>
            <Boton disabled={cuenta.roles.includes(rol)} onClick={() => setConfirmar(true)}>
              Añadir rol
            </Boton>
          </div>
          <p className="text-sm">Los roles anteriores se conservan. La persona debe recargar
            la página o volver a iniciar sesión para actualizar su menú.</p>
          {asignar.isSuccess && <p role="status">Rol añadido correctamente.</p>}
        </Tarjeta>
      )}
      <Modal abierto={confirmar} titulo="Confirmar asignación de rol"
        ocupado={asignar.isPending} onCerrar={() => setConfirmar(false)}>
        <p>Vas a añadir el rol {etiquetaRol[rol]} a {cuenta?.username} ({cuenta?.email}).</p>
        {rol === 'ADMIN' && <p className="mt-3">
          Esta cuenta podrá administrar eventos y asignar roles a otras personas.
        </p>}
        {asignar.isError && <p role="alert" className="mt-3 text-red-600">{asignar.error.message}</p>}
        <div className="mt-5 flex gap-3">
          <Boton variante="secundario" disabled={asignar.isPending}
            onClick={() => setConfirmar(false)}>Cancelar</Boton>
          <Boton cargando={asignar.isPending} onClick={() => {
            if (cuenta && !asignar.isPending) asignar.mutate({ id: cuenta.id, role: rol })
          }}>Confirmar</Boton>
        </div>
      </Modal>
    </Contenedor>
  )
}
