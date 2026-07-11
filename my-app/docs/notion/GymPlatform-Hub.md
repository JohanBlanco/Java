# GymPlatform — Hub de Proyecto

> Importar a Notion: copiar secciones como páginas hijas o usar Notion MCP cuando esté autenticado.

**Última actualización:** 2026-07-10  
**Estado:** MVP en desarrollo

---

## Resumen

Plataforma SaaS multi-tenant para gimnasios. Tres capas: API (Spring Boot), Web (React), Móvil (Flutter).

---

## Enlaces rápidos

| Recurso | URL / Ubicación |
|---------|-----------------|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Web local | http://localhost:5173 |
| GitHub Wiki | `docs/wiki/` → publicar en repo.wiki.git |
| Cursor Rules | `.cursor/rules/` |
| Repo | `my-app/` |

---

## Cómo levantar el proyecto

### Backend
```bash
cd backend && mvn spring-boot:run
```

### Web
```bash
cd web && npm install && npm run dev
```

### Móvil
```bash
cd mobile && flutter create . --org com.gymplatform && flutter pub get && flutter run
```

---

## Cuentas de prueba

| Rol | Email | Password |
|-----|-------|----------|
| Platform Owner | admin@gymplatform.com | admin123 |
| Gym Owner | dueno@fitlife.com | 12345678 |
| Instructor | instructor@fitlife.com | instructor123 |
| Member | miembro@fitlife.com | miembro123 |

### Contraseña por defecto

- **Nuevo gimnasio:** el dueño entra con el correo de contacto y `12345678`
- **Nuevo usuario staff:** si no se envía `password` en la API, se usa `12345678`

---

## Estado actual del MVP

### Implementado
- [x] Multi-tenant (organizations)
- [x] Auth JWT con 4 roles
- [x] Planes de entrenamiento con complementos
- [x] Actividades con cupo y reservaciones
- [x] Rutinas, plantillas, solicitudes
- [x] Perfil de miembro
- [x] Panel web responsive
- [x] App Flutter (código base)
- [x] Swagger/OpenAPI
- [x] Documentación Wiki + Cursor Rules

### Pendiente
- [ ] RBAC fino por rol
- [ ] Suscripción de miembros a paquetes
- [ ] Pagos
- [ ] Notificaciones push
- [ ] Tests automatizados
- [ ] CRUD completo (update/delete)

---

## Guía de pruebas por rol

### Platform Owner
1. Login → panel de clientes
2. Crear gimnasio nuevo
3. Suspender / activar suscripción

### Gym Owner
1. Login → Administración
2. Crear plan de entrenamiento con complemento
3. Crear usuario staff

### Instructor
1. Login → ver solicitudes de rutina
2. Tomar solicitud
3. (API) Crear plantilla y asignar

### Member
1. Login → reservar actividad
2. Confirmar / cancelar reserva
3. Editar perfil
4. Solicitar rutina

---

## Bitácora de sesiones

### 2026-07-10
- Proyecto inicial creado
- Documentación en Swagger, Wiki, Notion (plantilla), Cursor Rules
- Backend compila, web construye

---

## Roadmap (ideas)

- [ ] Dashboard con métricas por gimnasio
- [ ] Check-in con QR
- [ ] Horarios recurrentes de clases
- [ ] Chat instructor-miembro
- [ ] App white-label por gimnasio

---

## Notas de arquitectura

- DB dev: H2 in-memory (se reinicia al parar backend)
- JWT expira en 24h
- CORS: localhost:5173, localhost:3000
- Tenant isolation por `organizationId` en JWT

---

## Sincronización de docs

Al trabajar en el app, actualizar:

1. **Swagger** — automático si hay anotaciones en controllers
2. **Wiki** — `docs/wiki/*.md`
3. **Esta página Notion** — estado y bitácora
4. **Changelog** — `docs/wiki/Changelog.md`

Cursor tiene reglas en `.cursor/rules/documentation-sync.mdc` para recordar esto.
