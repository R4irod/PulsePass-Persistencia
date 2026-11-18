import { useContext } from 'react'

import { AuthContext } from './AuthContext'

export function useAuth() {
  const contexto = useContext(AuthContext)

  if (contexto === undefined) {
    throw new Error(
      'useAuth debe utilizarse dentro de AuthProvider.',
    )
  }

  return contexto
}