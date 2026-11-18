import { createBrowserRouter } from 'react-router'
import Layout from './layouts/Layout'
import RutaProtegida from './layouts/RutaProtegida'
import RutaPorRol from './layouts/RutaPorRol'
import SoloAnonimo from './layouts/SoloAnonimo'
import Inicio from './pages/Inicio'
import ErrorPagina from './pages/ErrorPagina'
import { RUTAS } from './lib/rutas'

export const router = createBrowserRouter([
  {
    path: RUTAS.inicio,
    element: <Layout />,
    errorElement: <ErrorPagina />,
    children: [
      { index: true, element: <Inicio /> },
      {
        path: RUTAS.eventos,
        lazy: async () => ({
          Component: (await import('./pages/CatalogoEventos')).default,
        }),
      },
      {
        path: RUTAS.patronEvento,
        lazy: async () => ({
          Component: (await import('./pages/DetalleEvento')).default,
        }),
      },
      {
        path: RUTAS.artistas,
        lazy: async () => ({
          Component: (await import('./pages/ListaArtistas')).default,
        }),
      },
      {
        path: RUTAS.patronArtista,
        lazy: async () => ({
          Component: (await import('./pages/DetalleArtista')).default,
        }),
      },
      {
        path: RUTAS.sedes,
        lazy: async () => ({
          Component: (await import('./pages/ListaSedes')).default,
        }),
      },
      {
        path: RUTAS.patronSede,
        lazy: async () => ({
          Component: (await import('./pages/DetalleSede')).default,
        }),
      },
      {
        element: <SoloAnonimo />,
        children: [
          {
            path: RUTAS.login,
            lazy: async () => ({
              Component: (await import('./pages/Login')).default,
            }),
          },
          {
            path: RUTAS.registro,
            lazy: async () => ({
              Component: (await import('./pages/Registro')).default,
            }),
          },
        ],
      },
      {
        element: <RutaProtegida />,
        children: [
          {
            path: RUTAS.cuenta,
            lazy: async () => ({
              Component: (await import('./pages/Cuenta')).default,
            }),
          },
          {
            element: <RutaPorRol roles={['USER']} />,
            children: [
              {
                path: RUTAS.misTickets,
                lazy: async () => ({ Component: (await import('./pages/MisTickets')).default }),
              },
              {
                path: RUTAS.patronTicket,
                lazy: async () => ({ Component: (await import('./pages/DetalleTicket')).default }),
              },
              {
                path: RUTAS.patronConfirmacion,
                lazy: async () => ({ Component: (await import('./pages/ConfirmacionCompra')).default }),
              },
            ],
          },
          {
            element: <RutaPorRol roles={['STAFF', 'ADMIN']} />,
            children: [
              {
                path: RUTAS.validacion,
                lazy: async () => ({
                  Component: (await import('./pages/ValidacionIngreso')).default,
                }),
              },
              {
                path: RUTAS.staffEventos,
                lazy: async () => ({
                  Component: (await import('./pages/TicketsPagadosEvento')).default,
                }),
              },
              {
                path: RUTAS.patronTicketsPagados,
                lazy: async () => ({
                  Component: (await import('./pages/TicketsPagadosEvento')).default,
                }),
              },
            ],
          },
          {
            element: <RutaPorRol roles={['ADMIN']} />,
            children: [
              { path: RUTAS.adminEventos, lazy: async () => ({ Component: (await import('./pages/AdminEventos')).default }) },
              {
                path: RUTAS.adminRoles,
                lazy: async () => ({ Component: (await import('./pages/AdminRoles')).default }),
              },
              { path: RUTAS.adminNuevo, lazy: async () => ({ Component: (await import('./pages/AdminCrearEvento')).default }) },
              { path: RUTAS.patronAdminEvento, lazy: async () => ({ Component: (await import('./pages/AdminGestionarEvento')).default }) },
            ],
          },
        ],
      },
      {
        path: RUTAS.sinPermisos,
        lazy: async () => ({
          Component: (await import('./pages/SinPermisos')).default,
        }),
      },
      {
        path: '*',
        lazy: async () => ({
          Component: (await import('./pages/NoEncontrada')).default,
        }),
      },
    ],
  },
])
