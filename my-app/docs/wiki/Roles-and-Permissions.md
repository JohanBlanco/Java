# Roles y permisos

## Matriz de capacidades

| Capacidad | PLATFORM_OWNER | GYM_OWNER | INSTRUCTOR | MEMBER |
|-----------|:--------------:|:---------:|:----------:|:------:|
| Gestionar clientes (orgs) | ✅ | ❌ | ❌ | ❌ |
| Activar/suspender suscripción | ✅ | ❌ | ❌ | ❌ |
| Crear planes de entrenamiento/complementos | ❌ | ✅ | ❌* | ❌ |
| Crear usuarios staff | ❌ | ✅ | ❌* | ❌ |
| Crear actividades | ❌ | ✅ | ✅* | ❌ |
| Reservar actividades | ❌ | ❌ | ❌ | ✅ |
| Confirmar/cancelar reserva | ❌ | ❌ | ❌ | ✅** |
| Crear plantillas rutina | ❌ | ❌ | ✅ | ❌ |
| Asignar rutinas | ❌ | ❌ | ✅ | ❌ |
| Ver mis rutinas | ❌ | ❌ | ❌ | ✅ |
| Solicitar rutina | ❌ | ❌ | ❌ | ✅ |
| Atender solicitudes | ❌ | ❌ | ✅ | ❌ |
| Editar perfil propio | ❌ | ❌ | ❌ | ✅ |

\* Actualmente la API no restringe por rol a nivel método; cualquier usuario autenticado del org puede llamar endpoints de gimnasio. Restricción fina pendiente.

\** Cualquier usuario autenticado puede confirmar/cancelar cualquier reservación (mejora pendiente: solo el dueño de la reserva).

## PLATFORM_OWNER

- No tiene `organizationId` en el JWT
- Solo accede a `/api/platform/**`
- Al crear un cliente define la **cuenta del administrador** (`ownerFirstName`, `ownerLastName`, `ownerEmail`, `ownerPassword`)
- Ese administrador inicia sesión en la app web/móvil como **GYM_OWNER** de su gimnasio

## GYM_OWNER

- Administra su organización completa (usuarios aislados por `organizationId`)
- En web: sección **Administración** (plan de entrenamiento, usuarios)
- Puede crear usuarios con rol **INSTRUCTOR** o **MEMBER** (`POST /api/users`)
- Contraseña por defecto si se omite: **`12345678`**

## INSTRUCTOR

- Crea plantillas y rutinas
- Atiende solicitudes de rutina

## MEMBER

- Auto-registro vía `POST /api/auth/register/{orgId}`
- Perfil con `MemberProfile` (birthYear, age, goals, phone)
- Flujo principal: reservar actividades, ver rutinas

## Estados de suscripción (Organization)

| Estado | Significado |
|--------|-------------|
| ACTIVE | Cliente operativo |
| TRIAL | Periodo de prueba |
| SUSPENDED | Suspendido por platform owner |
| INACTIVE | Inactivo |

Solo orgs ACTIVE o TRIAL aparecen en `/api/public/organizations`.

## Estados de reservación

| Estado | Transiciones |
|--------|-------------|
| PENDING | → CONFIRMED, CANCELLED |
| CONFIRMED | → CANCELLED |
| CANCELLED | (final) |

## Estados de solicitud de rutina

| Estado | Descripción |
|--------|-------------|
| PENDING | Recién creada por miembro |
| IN_PROGRESS | Instructor la tomó |
| COMPLETED | Rutina entregada |
| REJECTED | Rechazada |
