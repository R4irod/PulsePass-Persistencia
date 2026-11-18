import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'

import { fijarToken, registrarAlExpirar } from '../../lib/http'
import { AuthContext, type EstadoSesion } from './AuthContext'
import {
  iniciarSesion as solicitarLogin,
  obtenerUsuarioActual,
} from './api'
import {
  guardarSesion,
  leerSesionGuardada,
  eliminarSesionGuardada,
} from './sesion'
import type { DatosLogin } from './tipos'

interface AuthProviderProps {
  children: ReactNode
}

export default function AuthProvider({ children }: AuthProviderProps) {
  const queryClient = useQueryClient()
  // Cada cambio invalida las respuestas pendientes de la sesion anterior.
  const version = useRef(0)
  const [sesion, setSesion] = useState<EstadoSesion>({
    estado: 'cargando',
  })

  useEffect(() => {
    function manejarSesionExpirada() {
      version.current += 1
      fijarToken(null)
      eliminarSesionGuardada()
      queryClient.clear()
      setSesion({ estado: 'anonimo', motivo: 'sesion-expirada' })
    }

    registrarAlExpirar(manejarSesionExpirada)
    return () => registrarAlExpirar(null)
  }, [queryClient])

  useEffect(() => {
    const operacion = ++version.current

    async function restaurarSesion() {
      const guardada = leerSesionGuardada()
      if (!guardada) {
        fijarToken(null)
        setSesion({ estado: 'anonimo' })
        return
      }

      fijarToken(guardada.accessToken)
      try {
        const usuario = await obtenerUsuarioActual()
        if (operacion !== version.current) return

        if (guardada.expiraEn <= Date.now()) {
          fijarToken(null)
          eliminarSesionGuardada()
          queryClient.clear()
          setSesion({ estado: 'anonimo', motivo: 'sesion-expirada' })
          return
        }

        setSesion({
          estado: 'autenticado',
          usuario,
          expiraEn: guardada.expiraEn,
        })
      } catch {
        if (operacion !== version.current) return
        fijarToken(null)
        eliminarSesionGuardada()
        queryClient.clear()
        setSesion({ estado: 'anonimo' })
      }
    }

    void restaurarSesion()
    return () => { version.current += 1 }
  }, [queryClient])

  useEffect(() => {
    if (sesion.estado !== 'autenticado') return
    const expiraEn = sesion.expiraEn

    function comprobarVencimiento() {
      if (Date.now() < expiraEn) return
      version.current += 1
      fijarToken(null)
      eliminarSesionGuardada()
      queryClient.clear()
      setSesion({ estado: 'anonimo', motivo: 'sesion-expirada' })
    }

    const temporizador = window.setTimeout(
      comprobarVencimiento,
      Math.max(0, expiraEn - Date.now()),
    )
    window.addEventListener('focus', comprobarVencimiento)
    return () => {
      window.clearTimeout(temporizador)
      window.removeEventListener('focus', comprobarVencimiento)
    }
  }, [sesion, queryClient])

  async function iniciarSesion(datos: DatosLogin): Promise<void> {
    const operacion = ++version.current
    // El formulario permanece montado para mostrar errores del servidor.
    try {
      const respuesta = await solicitarLogin(datos)
      if (operacion !== version.current) return

      const guardada = guardarSesion(
        respuesta.accessToken,
        respuesta.expiresIn,
      )
      fijarToken(respuesta.accessToken)
      const usuario = await obtenerUsuarioActual()
      if (operacion !== version.current) return

      if (guardada.expiraEn <= Date.now()) {
        fijarToken(null)
        eliminarSesionGuardada()
        queryClient.clear()
        setSesion({ estado: 'anonimo', motivo: 'sesion-expirada' })
        return
      }

      queryClient.clear()
      setSesion({
        estado: 'autenticado',
        usuario,
        expiraEn: guardada.expiraEn,
      })
    } catch (error: unknown) {
      if (operacion === version.current) {
        fijarToken(null)
        eliminarSesionGuardada()
        queryClient.clear()
        setSesion({ estado: 'anonimo' })
      }
      throw error
    }
  }

  function cerrarSesion() {
    version.current += 1
    fijarToken(null)
    eliminarSesionGuardada()
    queryClient.clear()
    setSesion({ estado: 'anonimo' })
  }

  return (
    <AuthContext.Provider value={{ sesion, iniciarSesion, cerrarSesion }}>
      {children}
    </AuthContext.Provider>
  )
}
