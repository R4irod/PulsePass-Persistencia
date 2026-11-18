# PulsePass

Proyecto académico de gestión de eventos y tickets con backend Spring Boot,
PostgreSQL y frontend React + TypeScript.

- Backend: `src/`, `pom.xml` y Maven Wrapper en la raíz.
- Frontend: [instrucciones y funciones](frontend/README.md).
- Documentación de seguridad y Postman: [README_postman.md](README_postman.md).
- Colección exportada: `Docs/PulsePass-Fase4.postman_collection.json`.

La nueva interfaz permite consultar eventos, registrarse, iniciar sesión, comprar
tickets simulados y validarlos. Los administradores pueden crear y publicar eventos,
asociar artistas existentes y añadir roles a las cuentas.

El backend requiere su configuración de PostgreSQL y el secreto JWT. Usa el archivo
`.env.example` como referencia; no subas credenciales reales. El frontend se ejecuta
por separado y redirige `/api` al backend durante el desarrollo.

No se incluyen dependencias instaladas, compilados ni datos de la base de datos.
