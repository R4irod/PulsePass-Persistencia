import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './http'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (intentos, error) => {
        const esError4xx = error instanceof ApiError && error.status >= 400 && error.status < 500
        return !esError4xx && intentos < 1
      },
      refetchOnWindowFocus: false,
    },
    mutations: { retry: false },
  },
})