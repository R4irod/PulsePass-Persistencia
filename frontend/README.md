# PulsePass — Frontend

Aplicación académica de eventos con React, TypeScript, Vite y Tailwind CSS.
Consume la API Spring Boot ubicada en la raíz de este repositorio.

## Ejecutar

Con Node.js 24 y el backend disponible en el puerto 8080:

```powershell
cd frontend
npm ci
npm run dev
```

Abre la dirección que muestra Vite, normalmente http://localhost:5173.
Las peticiones `/api` se redirigen a `http://localhost:8080` mediante el proxy
de desarrollo. `.env.example` contiene únicamente la dirección pública de la API;
no se deben colocar contraseñas de PostgreSQL ni secretos JWT en el frontend.

## Funciones implementadas

- Catálogo de eventos publicados con búsqueda y filtros por categoría y artista.
- Listas y detalles de artistas, sedes y eventos.
- Registro, login por usuario o correo, perfil y cierre de sesión.
- Sesión con token Bearer, recuperación al recargar y manejo de expiración.
- USER: compra simulada, confirmación, tickets propios y cancelación.
- STAFF y ADMIN: búsqueda y validación de entradas; tickets pagados por evento.
- ADMIN: crear borradores, asociar artistas existentes y publicar eventos.
- ADMIN: buscar cuentas y añadir roles conservando los que ya tienen.
- Tema claro/oscuro, menú móvil y mensajes de carga, error y resultados vacíos.

Los roles son acumulativos: un administrador necesita también USER para comprar.
Los permisos y la propiedad de los tickets se comprueban en el backend.
Las compras no realizan pagos reales. Los precios son valores académicos del backend.

## Organización

| Carpeta | Contenido |
| --- | --- |
| `src/pages` | Pantallas de la aplicación. |
| `src/componentes` | Botones, campos, tarjetas, modales y otros elementos compartidos. |
| `src/funcionalidades` | API, hooks y lógica de autenticación, eventos y tickets. |
| `src/layouts` | Diseño general y protección de rutas. |
| `src/lib` | Cliente HTTP, esquemas, rutas, formato y traducción de mensajes. |
| `tests` | Pruebas automatizadas con el ejecutor de Node. |

## Validación

```powershell
npm test
npm run lint
npm run build
```

Última verificación: 38 pruebas del frontend aprobadas, compilación y lint correctos.
Las pruebas HTTP usan respuestas simuladas y no modifican la base de datos.
También se comprobó en navegador contra el backend real el flujo de login,
creación de evento, asociación de artista, publicación, compra y uso de ticket,
además del menú y filtro de tickets en una pantalla de 390 × 844.
Ese recorrido se realizó con una cuenta USER + ADMIN; no sustituye todas las
pruebas de permisos con cuentas separadas.

## Límites actuales

- No hay formularios para crear artistas o sedes; se seleccionan los existentes.
- RESERVED es un estado del modelo, pero no existe un flujo de reserva en la interfaz.
- La administración busca eventos por código y conserva accesos recientes por pestaña;
  no existe un endpoint para listar todos los eventos.
- El despliegue permanente y la recuperación de contraseña desde la web no están implementados.
- La última ejecución ampliada de pruebas Java quedó sin verificar por un bloqueo
  del entorno de ejecución. No se presenta como aprobada.

## Demostración por internet

Se verificó el uso de un túnel temporal de Cloudflare hacia el frontend.
El PC, Vite, el backend y el túnel deben permanecer encendidos.
El dominio temporal debe autorizarse explícitamente en Vite mediante
`__VITE_ADDITIONAL_SERVER_ALLOWED_HOSTS`. No se guarda un dominio temporal en el código.
GitHub almacena el código; subir este proyecto no publica automáticamente la aplicación.
