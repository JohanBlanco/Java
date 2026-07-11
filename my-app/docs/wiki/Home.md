# GymPlatform Wiki

Bienvenido a la documentación de **GymPlatform**, plataforma SaaS multi-tenant para administración de gimnasios.

## Índice

| Página | Descripción |
|--------|-------------|
| [Inicio rápido](Getting-Started) | Requisitos y cómo levantar el proyecto |
| [Guía de pruebas](Testing-Guide) | Checklists por rol y flujo |
| [Arquitectura](Architecture) | Capas, roles y multi-tenant |
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
| Dueño plataforma | admin@gymplatform.com | admin123 |
| Dueño gimnasio | dueno@fitlife.com | 12345678 |
| Instructor | instructor@fitlife.com | instructor123 |
| Miembro | miembro@fitlife.com | miembro123 |

## Contraseña por defecto

| Acción | Login | Contraseña inicial |
|--------|-------|-------------------|
| Crear cliente (platform owner) | Correo de contacto del gimnasio | `12345678` |
| Crear usuario staff en un gym (`POST /api/users` sin `password`) | Email indicado | `12345678` |

El dueño del gimnasio se crea automáticamente al registrar un nuevo cliente.

## Otras fuentes de documentación

- **Swagger** — documentación interactiva de la API
- **Notion** — hub de proyecto (uso, roadmap, notas)
- **Cursor Rules** — convenciones en `.cursor/rules/`
- **README** — inicio rápido en el repositorio

---

*Última actualización: 2026-07-11 (auto-sync)*
