# GymPlatform

Plataforma SaaS multi-tenant para administración de gimnasios.

## Inicio rápido

```bash
# 1. API
cd backend && mvn spring-boot:run

# 2. Web
cd web && npm install && npm run dev
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Web: http://localhost:5173

## Documentación

| Canal | Link |
|-------|------|
| **Automatización** | [docs/AUTOMATION.md](docs/AUTOMATION.md) |
| **Índice completo** | [docs/README.md](docs/README.md) |
| **Guía de pruebas** | [docs/wiki/Testing-Guide.md](docs/wiki/Testing-Guide.md) |
| **Swagger (API)** | http://localhost:8080/swagger-ui.html |
| **GitHub Wiki** | Auto-sync → [SETUP-WIKI.md](docs/wiki/SETUP-WIKI.md) |
| **Notion** | Auto-sync con `NOTION_TOKEN` |
| **Cursor Rules** | [.cursor/rules/](.cursor/rules/) |

### Activar auto-sync (una vez)

```bash
npm install && npm run docs:install-hooks
```

## Estructura

```
my-app/
├── backend/        Spring Boot 3 + JWT
├── web/            React + Vite
├── mobile/         Flutter
├── docs/           Wiki, Notion, índice
└── .cursor/rules/  Convenciones para IA
```

## Cuentas demo

| Rol | Email | Contraseña |
|-----|-------|------------|
| Platform Owner | admin@gymplatform.com | admin123 |
| Gym Owner | dueno@fitlife.com | 12345678 |
| Instructor | instructor@fitlife.com | instructor123 |
| Member | miembro@fitlife.com | miembro123 |

**Contraseña por defecto** al crear un gimnasio o un usuario staff (sin indicar password): `12345678`

## Móvil

```bash
cd mobile && flutter create . --org com.gymplatform && flutter pub get && flutter run
```
