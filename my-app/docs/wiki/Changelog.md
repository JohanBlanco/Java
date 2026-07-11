
<!-- AUTO:a7c183381ba5ef66a3e3b6f3cf0d6177f767d9db -->
## 2026-06-25 — completed jdbctemplate module

_Auto-sync desde commit `a7c1833`_

### Docs actualizados automáticamente
- [x] Timestamps wiki
- [x] Changelog
- [x] API Reference (si --full)

---
# Changelog de desarrollo

Bitácora de cambios. Actualizar en cada sesión de trabajo.

---

## 2026-07-10 — Contraseña por defecto y dueño al crear gimnasio

### Qué se hizo
- Al crear un cliente se crea automáticamente un `GYM_OWNER` con el correo de contacto
- Contraseña inicial por defecto: `12345678` (gimnasios nuevos y usuarios staff sin password)
- Formulario web de usuarios en Administración del gimnasio
- Documentación actualizada (wiki, README, Notion, Swagger)

### Cómo probarlo
1. Login como `admin@gymplatform.com` / `admin123`
2. Crear cliente con correo de contacto nuevo
3. Login con ese correo y `12345678`
4. En Administración → crear instructor sin password → login con `12345678`

---

## 2026-07-10 — Setup inicial de documentación multi-canal

### Qué se hizo
- Plataforma GymPlatform creada: backend, web, móvil
- Documentación distribuida en 4 canales:
  - **Swagger** — OpenAPI en `/swagger-ui.html`
  - **GitHub Wiki** — páginas en `docs/wiki/`
  - **Notion** — plantilla en `docs/notion/GymPlatform-Hub.md`
  - **Cursor Rules** — `.cursor/rules/*.mdc`

### Cómo probar
1. `cd backend && mvn spring-boot:run`
2. Abrir http://localhost:8080/swagger-ui.html
3. `cd web && npm run dev` → http://localhost:5173
4. Seguir checklists en [Testing-Guide](Testing-Guide)

### 2026-07-10 — Automatización de documentación

### Qué se hizo
- Scripts `npm run docs:sync:fast` y `docs:sync:full`
- Git hook post-commit para sync completo
- Cursor hook `stop` para sync rápido al terminar agente
- GitHub Action `sync-documentation.yml`
- Export automático OpenAPI → `docs/openapi.json` → API Reference wiki
- Sync opcional a GitHub Wiki y Notion vía tokens

### Cómo activar
```bash
npm install && npm run docs:install-hooks
cp docs/env.example .env  # opcional
```

### Docs
- [AUTOMATION.md](../AUTOMATION.md)

---
- [ ] RBAC fino por rol en backend
- [ ] Suscripciones de miembros a paquetes
- [ ] Tests automatizados

---

## Plantilla para nuevas entradas

```markdown
## YYYY-MM-DD — Título

### Qué se hizo
- ...

### Cómo probar
1. ...

### Docs actualizados
- [ ] Swagger
- [ ] Wiki: (páginas)
- [ ] Notion
- [ ] Cursor rules

### Pendiente
- [ ] ...
```
