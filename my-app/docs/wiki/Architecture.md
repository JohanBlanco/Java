# Arquitectura

## Visión general

GymPlatform es una plataforma **SaaS multi-tenant** donde:

- **Tú (PLATFORM_OWNER)** vendes el sistema a gimnasios
- Cada **gimnasio (Organization)** es un tenant aislado con un perfil único de administración
- Usuarios pertenecen a una organización con uno o más **roles**

```
┌─────────────────────────────────────────────────┐
│              PLATFORM_OWNER (tú)                │
│         Gestiona clientes / suscripciones       │
└────────────────────┬────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
   ┌─────────┐  ┌─────────┐  ┌─────────┐
   │ FitLife │  │ PowerGym│  │  ...    │  ← Organizations
   └────┬────┘  └────┬────┘  └─────────┘
        │            │
   Usuarios, paquetes, actividades, rutinas (por org)
```

## Capas técnicas

| Capa | Tecnología | Puerto |
|------|------------|--------|
| API | Spring Boot 3 + JPA + JWT | 8080 |
| Web | React + Vite + TypeScript | 5173 |
| Móvil | Flutter + Provider | — |
| DB dev | H2 in-memory | — |
| DB prod | PostgreSQL (preparado) | — |

## Modelo de dominio (resumen)

| Entidad | Relación |
|---------|----------|
| Organization | 1→N User, Package, Activity, Routine |
| MembershipPackage | 1→N PackageAddon |
| Activity | 1→N Reservation (cupo opcional) |
| RoutineTemplate | 1→N RoutineExercise |
| Routine | asignada a Member, creada por Instructor |
| RoutineRequest | solicitada por Member, atendida por Instructor |
| MemberProfile | 1:1 con User (solo miembros) |

## Autenticación

- Login → JWT con claims: `userId`, `role`, `organizationId`
- Rutas `/api/platform/**` → solo `ROLE_PLATFORM_OWNER`
- Resto de `/api/**` → autenticado; scope por `organizationId` del token

## Clientes (frontends)

```
Browser / App
     │
     ▼
  JWT en header
     │
     ▼
 Spring Security → Controller → Service → Repository → DB
```

## Documentación relacionada

- [Roles y permisos](Roles-and-Permissions)
- [Referencia API](API-Reference) + Swagger UI
- [Guía de pruebas](Testing-Guide)
