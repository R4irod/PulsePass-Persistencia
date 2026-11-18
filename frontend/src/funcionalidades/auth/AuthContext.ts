import { createContext } from 'react'

import type { UsuarioAutenticado } from '../../lib/esquemas'
import type { DatosLogin } from './tipos'

export type EstadoSesion =
  | {
      estado: 'cargando'
    }
  | {
      estado: 'anonimo'
      motivo?: 'sesion-expirada'
    }
  | {
      estado: 'autenticado'
      usuario: UsuarioAutenticado
      expiraEn: number
    }

export interface AuthContextValor {
  sesion: EstadoSesion
  iniciarSesion: (datos: DatosLogin) => Promise<void>
  cerrarSesion: () => void
}

export const AuthContext = createContext<
  AuthContextValor | undefined
>(undefined)