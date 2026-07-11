# Guía de pruebas

Checklists manuales por rol. Marca cada ítem al validarlo.

---

## Pre-requisitos

- [ ] Backend corriendo en `:8080`
- [ ] Web corriendo en `:5173` (o móvil con Flutter)
- [ ] Swagger accesible en `/swagger-ui.html`

---

## PLATFORM_OWNER — Dueño de plataforma

**Login:** `admin@gymplatform.com` / `admin123`

### Web

- [ ] Iniciar sesión redirige a `/platform`
- [ ] Ver lista de clientes (gimnasios) existentes
- [ ] Crear nuevo cliente con nombre, slug, email de contacto
- [ ] Verificar que el dueño puede entrar con ese email y contraseña `12345678`
- [ ] Suspender un cliente activo
- [ ] Reactivar un cliente suspendido

### API (Swagger)

- [ ] `POST /api/auth/login` → obtener token
- [ ] Click **Authorize** en Swagger, pegar `Bearer <token>`
- [ ] `GET /api/platform/organizations` → lista orgs
- [ ] `POST /api/platform/organizations` → crear org
- [ ] `PUT /api/platform/organizations/{id}` → cambiar subscriptionStatus

### Resultado esperado

El platform owner solo ve gestión de clientes, no accede a funciones de gimnasio (packages, users, etc.) porque no tiene `organizationId`.

---

## GYM_OWNER — Dueño de gimnasio

**Login:** `dueno@fitlife.com` / `12345678`

### Web — Administración

- [ ] Ver sección **Administración** en sidebar (plan de entrenamiento, usuarios)
- [ ] Listar planes de entrenamiento
- [ ] Crear plan con complemento

### API (Swagger)

- [ ] `GET /api/packages`
- [ ] `POST /api/packages` (con array `addons`)
- [ ] `POST /api/activities` — crear actividad con cupo
- [ ] `POST /api/users` — crear instructor o miembro

### Resultado esperado

Todas las operaciones quedan scoped al `organizationId` del JWT (FitLife Gym).

---

## INSTRUCTOR

**Login:** `instructor@fitlife.com` / `instructor123`

### Web

- [ ] Ver actividades del gimnasio
- [ ] Ver solicitudes de rutina pendientes
- [ ] Tomar una solicitud (cambia a IN_PROGRESS)

### API (Swagger)

- [ ] `POST /api/routine-templates` — crear plantilla con ejercicios
- [ ] `GET /api/routine-templates`
- [ ] `POST /api/routines` — rutina individual (temporal o permanente)
- [ ] `POST /api/routines/assign-template` — asignar a múltiples miembros
- [ ] `PUT /api/routine-requests/{id}/status`

### Resultado esperado

Plantillas reutilizables; rutinas copian ejercicios de la plantilla.

---

## MEMBER — Miembro

**Login:** `miembro@fitlife.com` / `miembro123`

### Web

- [ ] Ver dashboard con actividades
- [ ] Reservar una actividad con cupo
- [ ] Confirmar reservación pendiente
- [ ] Cancelar reservación
- [ ] Ver rutinas asignadas
- [ ] Solicitar nueva rutina
- [ ] Editar perfil (año nacimiento, edad, objetivos, teléfono)

### Móvil (Flutter)

- [ ] Login exitoso
- [ ] Tab **Actividades** → reservar
- [ ] Tab **Reservas** → confirmar/cancelar (solo miembro)
- [ ] Tab **Rutinas** → ver ejercicios o solicitar rutina
- [ ] Tab **Perfil** → guardar cambios

#### Platform owner (móvil)

- [ ] Login `admin@gymplatform.com` → pantalla **Clientes**
- [ ] Crear gimnasio con cuenta admin (contraseña default `12345678`)
- [ ] Editar gimnasio existente (tap en tarjeta)
- [ ] Suspender/activar desde menú ⋮

#### Gym owner (móvil)

- [ ] Login con credenciales del gimnasio creado
- [ ] Tab **Admin** → plan de entrenamiento, usuarios
- [ ] Crear usuario con rol (Instructor/Miembro) y contraseña `12345678`

### API (Swagger)

- [ ] `GET /api/users/me`
- [ ] `PUT /api/users/me/profile`
- [ ] `POST /api/activities/{id}/reservations`
- [ ] `POST /api/reservations/{id}/confirm`
- [ ] `POST /api/reservations/{id}/cancel`
- [ ] `GET /api/routines/me`
- [ ] `POST /api/routine-requests`

### Casos límite

- [ ] Reservar dos veces la misma actividad → error "Ya tienes una reservación activa"
- [ ] Reservar actividad sin cupo → error "cupo máximo"
- [ ] Confirmar reservación cancelada → error

---

## Registro público

- [ ] `GET /api/public/organizations` — sin token, lista gimnasios activos
- [ ] `POST /api/auth/register/{organizationId}` — crear miembro nuevo

---

## Plantilla para nueva feature

Al agregar funcionalidad, copia y completa:

```markdown
## [Nombre feature] — YYYY-MM-DD
**Rol:** ...
**Pasos:**
1. ...
2. ...
**Resultado esperado:** ...
**Endpoint/Pantalla:** ...
```

Agregar al final de este archivo y registrar en [Changelog](Changelog).
