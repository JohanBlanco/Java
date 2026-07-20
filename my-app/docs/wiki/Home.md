# GymPlatform Wiki

Bienvenido a la documentación de **GymPlatform**, panel de administración para un gimnasio.

## Índice

| Página | Descripción |
|--------|-------------|
| [Inicio rápido](Getting-Started) | Requisitos y cómo levantar el proyecto |
| [Guía de pruebas](Testing-Guide) | Checklists por rol y flujo |
| [Tech Stack](Tech-Stack) | Tecnologías API, DB, web, móvil + diagramas |
| [Arquitectura](Architecture) | Capas, roles y modelo |
| [Modelo de datos (ERD)](Database-ERD) | Diagrama entidad-relación por dominios |
| [Migrar a PostgreSQL](Migrate-H2-to-PostgreSQL) | De H2 file a Postgres |
| [Frontend](Frontend) | React, rutas, auth, patrones UI |
| [Referencia API](API-Reference) | Endpoints y Swagger |
| [Roles y permisos](Roles-and-Permissions) | Qué puede hacer cada rol |
| [Changelog](Changelog) | Bitácora de cambios por sesión |

## Enlaces rápidos

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Web**: http://localhost:5173
- **API**: http://localhost:8080/api

## Cuentas de prueba

| Rol | Email | Contraseña |
|-----|-------|------------|
| Administrador | admin@fitlife.com | 12345678 |
| Instructor | instructor@fitlife.com | instructor123 |
| Recepcionista | recepcion@fitlife.com | recepcion123 |
| Miembro | miembro@fitlife.com | miembro123 |

El administrador demo puede cambiar de perfil (Administrador / Recepcionista / Instructor / Miembro) para probar el switch.

## Contraseña por defecto

| Acción | Contraseña inicial |
|--------|-------------------|
| Crear usuario staff (`POST /api/users` sin `password`) | `12345678` |
| Áreas privadas / estadísticas | `12345678` |

## Otras fuentes de documentación

- **Swagger** — documentación interactiva de la API
- **Notion** — hub de proyecto (uso, roadmap, notas)
- **Cursor Rules** — convenciones en `.cursor/rules/`
- **README** — inicio rápido en el repositorio

---

*Última actualización: 2026-07-20*
