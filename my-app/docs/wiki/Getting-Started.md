# Inicio rápido

## Requisitos

| Componente | Versión |
|------------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| Flutter | 3.2+ (opcional, para móvil) |

## 1. Backend (obligatorio)

```bash
cd backend
mvn spring-boot:run
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console (JDBC: `jdbc:h2:mem:gymdb`, user: `sa`, sin password)

Al arrancar se cargan datos demo automáticamente (ver cuentas en [Home](Home)).

## 2. Web

```bash
cd web
npm install
npm run dev
```

Abre http://localhost:5173

## 3. Móvil (Flutter)

Primera vez:

```bash
cd mobile
flutter create . --org com.gymplatform
flutter pub get
flutter run
```

| Entorno | URL de API |
|---------|------------|
| Emulador Android | `http://10.0.2.2:8080/api` (default) |
| iOS Simulator | `http://localhost:8080/api` |
| Dispositivo físico | IP de tu PC, ej. `http://192.168.1.10:8080/api` |

Editar: `mobile/lib/config/api_config.dart`

## Orden recomendado

1. Levantar **backend** primero
2. Luego **web** o **móvil**
3. Probar login con cuentas demo
4. Consultar [Guía de pruebas](Testing-Guide) para flujos por rol

## Verificar que todo funciona

```bash
# Backend compila
cd backend && mvn compile

# Web construye
cd web && npm run build
```

## Problemas comunes

| Problema | Solución |
|----------|----------|
| CORS error en web | Verificar backend corriendo y `app.cors.allowed-origins` en `application.properties` |
| 401 en API | Hacer login y usar token JWT en header `Authorization: Bearer <token>` |
| Móvil no conecta | Revisar URL en `api_config.dart` según emulador/dispositivo |
| Datos vacíos | Reiniciar backend (H2 es en memoria; datos se recrean al arrancar) |
