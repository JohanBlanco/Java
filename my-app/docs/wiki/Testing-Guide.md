# Guía de pruebas

Checklists manuales por rol. Marca cada ítem al validarlo.

---

## Pre-requisitos

- [ ] Backend corriendo en `:8080`
- [ ] Web corriendo en `:5173` (o móvil con Flutter)
- [ ] Swagger accesible en `/swagger-ui.html`

---

## Administrador (`GYM_OWNER`)

**Login:** `admin@fitlife.com` / `12345678`

### Web — Administración

- [ ] Ver sección **Administración** en sidebar
- [ ] Listar planes / membresías
- [ ] Crear usuario staff o miembro
- [ ] Estadísticas: desbloquear con `12345678`
- [ ] Switch de perfil → Miembro: ver rutinas/citas/nutrición del admin

### API (Swagger)

- [ ] `GET /api/packages`
- [ ] `POST /api/packages` (con array `addons`)
- [ ] `POST /api/activities` — crear actividad con cupo
- [ ] `POST /api/users` — crear instructor o miembro

### Resultado esperado

Operaciones scoped al `organizationId` del JWT (FitLife / Bulls Gym).

---

## Instructor

**Login:** `instructor@fitlife.com` / `instructor123`

### Web

- [ ] Ver actividades del gimnasio
- [ ] Ver solicitudes de rutina pendientes
- [ ] Tomar una solicitud (cambia a IN_PROGRESS)

### API (Swagger)

- [ ] `POST /api/routine-templates` — crear plantilla con ejercicios
- [ ] `GET /api/routine-templates`
- [ ] `POST /api/routines` — rutina individual
- [ ] `POST /api/routines/assign-template`
- [ ] `PUT /api/routine-requests/{id}/status`

---

## Miembro

**Login:** `miembro@fitlife.com` / `miembro123`

### Web

- [ ] Inicio con carrusel solo si hay promociones
- [ ] Reservar una actividad
- [ ] Ver mis actividades / citas
- [ ] Ver rutinas, nutrición y medidas
- [ ] Solicitar nueva rutina
- [ ] Editar perfil

### Móvil (Flutter)

- [ ] Login exitoso
- [ ] Tab **Actividades** → reservar
- [ ] Tab **Reservas** → confirmar/cancelar
- [ ] Tab **Rutinas** → ver o solicitar
- [ ] Tab **Perfil** → guardar cambios

#### Administrador (móvil)

- [ ] Login `admin@fitlife.com` / `12345678`
- [ ] Acceso a administración / ventas según tabs

---

## Recepcionista

**Login:** `recepcion@fitlife.com` / `recepcion123`

- [ ] Usuarios, productos, POS
- [ ] No debe entrar a estadísticas (solo administrador)

---

## Regresión rápida

- [ ] Login administrador → home staff
- [ ] Login miembro → home + servicios
- [ ] `/platform` redirige a `/` (ya no hay clientes)
- [ ] `admin@gymplatform.com` no puede iniciar sesión (cuenta desactivada / no disponible)
