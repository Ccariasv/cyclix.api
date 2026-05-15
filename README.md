# Cyclix API

Backend REST para autenticación, usuarios, soporte y viajes de la plataforma Cyclix.

Está construido con Kotlin + Spring Boot, usa JWT para autenticación, JPA/Flyway para persistencia y MariaDB como base de datos.

## Stack

- Kotlin `2.2.21`
- Spring Boot `4.0.5`
- Spring Security (JWT stateless)
- Spring Data JPA
- Flyway
- MariaDB
- Springdoc OpenAPI (Swagger UI)
- Gradle Kotlin DSL

## Requisitos

- JDK `25` (toolchain del proyecto)
- Docker + Docker Compose (opcional, recomendado)

Notas de versión Java en este repo:
- El proyecto se ejecuta con JDK 25.
- El bytecode/target está en `24` (`jvmTarget` y `options.release`).

## Configuración principal

Archivo: `src/main/resources/application.properties`

Variables relevantes:
- `SERVER_PORT` (default `6060`)
- `SPRING_DATASOURCE_URL` (default `jdbc:mariadb://localhost:3306/DB_cyclix`)
- `SPRING_DATASOURCE_USERNAME` (default `cyclix_admin`)
- `SPRING_DATASOURCE_PASSWORD` (default `cyclix10`)
- `app.jwt.secret` (default local en repo, cambiar en ambientes reales)
- `app.jwt.expiration-seconds` (default `86400`)

## Ejecutar local (sin Docker para la API)

1. Levantar MariaDB (local o contenedor) con credenciales compatibles.
2. Ejecutar la API:

```bash
./gradlew bootRun
```

La API quedará en `http://localhost:6060`.

## Ejecutar con Docker Compose

Este proyecto tiene dos servicios en `docker-compose.yaml`:
- `mariadb`
- `api` (con profile `full`)

Comando recomendado:

```bash
docker compose --profile full up --build
```

Notas importantes:
- Si ejecutás `docker compose up` sin profile, no se crea el contenedor `api`.
- La API expone `6060:6060`.
- MariaDB expone `3306:3306`.

## Swagger / OpenAPI

- UI: `http://localhost:6060/swagger-ui/index.html`
- JSON OpenAPI: `http://localhost:6060/v3/api-docs`

La seguridad está configurada con esquema Bearer JWT.

## Autenticación y seguridad

- `/api/v1/auth/**` es público.
- Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) es público.
- El resto requiere `Authorization: Bearer <token>`.
- Seguridad stateless (sin sesión).

## Endpoints

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

### Usuarios

Controlador mapeado en:
- `/api/v1/get/user`
- `/get/user`

Endpoints:
- `GET /api/v1/get/user`
- `PATCH /api/v1/get/user/{userId}/status`
- `PATCH /api/v1/get/user/{userId}/role`

### Viajes (usuario autenticado USER/ADMIN)

- `POST /api/v1/trips`
- `GET /api/v1/trips/my`
- `GET /api/v1/trips/{id}`
- `PUT /api/v1/trips/{id}/finish`

### Viajes admin (solo ADMIN)

- `GET /api/v1/admin/trips`
- `GET /api/v1/admin/trips/{id}`
- `PUT /api/v1/admin/trips/{id}/cancel`

### Soporte (usuario autenticado USER/ADMIN)

- `POST /api/v1/support/tickets`
- `GET /api/v1/support/tickets/my`
- `GET /api/v1/support/tickets/{id}`

### Soporte admin (solo ADMIN)

- `GET /api/v1/admin/support/tickets`
- `PUT /api/v1/admin/support/tickets/{id}/status`
- `PUT /api/v1/admin/support/tickets/{id}/priority`

## Reglas de negocio destacadas

- Registro crea usuarios con rol `USER` y estado `ACTIVE`.
- Login valida credenciales con BCrypt y devuelve JWT.
- - Los viajes se crean en estado `ACTIVE`.
- Un usuario no puede tener más de un viaje `ACTIVE` al mismo tiempo.
- Un viaje solo puede finalizarse si está en estado `ACTIVE`.
- Al finalizar un viaje, pasa a estado `COMPLETED`, guarda coordenadas finales, distancia opcional y duración en segundos.
- Un ADMIN puede consultar todos los viajes.
- Un ADMIN puede cancelar viajes activos, dejándolos en estado `CANCELLED`.
- `bikeId` en viajes se guarda como referencia numérica. Actualmente no tiene foreign key porque todavía no existe un módulo formal de bicicletas.
- `EMERGENCY` en tickets fuerza prioridad `CRITICAL`.
- Un ticket `EMERGENCY` no puede quedar con prioridad distinta de `CRITICAL`.
- Validaciones de `bikeId`, `tripId`, `paymentId` se hacen contra tablas `bikes`, `trips`, `payments` usando JDBC.
- Si esas tablas no existen en la BD actual, la API responde `400` para esos campos de referencia.

## Base de datos y migraciones

Migraciones Flyway en `src/main/resources/db/migration`:
- `V1__users_roles.sql`
- `V2__support_tickets.sql`
- `V3__seed_test_data.sql`
- `V4__fix_seed_user_passwords.sql`
- `V5__trips_module.sql`

Entidades base creadas:
- `roles`
- `user_statuses`
- `user`
- `ticket_categories`
- `ticket_priorities`
- `ticket_statuses`
- `support_tickets`
- `trip_statuses`
- `trips`

## Datos semilla útiles

Usuarios seed:
- `admin@cyclix.test` / `Test1234*` (rol ADMIN)
- `laura@cyclix.test` / `Test1234*` (rol USER)
- `carlos@cyclix.test` / `Test1234*` (rol USER)

## Ejemplos rápidos

### Login

```bash
curl -X POST http://localhost:6060/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@cyclix.test",
    "password": "Test1234*"
  }'
```

### Crear ticket (con token)

```bash
curl -X POST http://localhost:6060/api/v1/support/tickets \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "APP",
    "priority": "MEDIUM",
    "title": "Error al abrir mapa",
    "description": "La app se cierra en Android al iniciar viaje"
  }'
```

### Cambiar rol de usuario

```bash
curl -X PATCH http://localhost:6060/api/v1/get/user/2/role \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{"role":"ADMIN"}'
```

### Crear viaje (con token)
```bash
curl -X POST http://localhost:6060/api/v1/trips
  -H "Authorization: Bearer " -H "Content-Type: application/json" -d '{ "bikeId": 101, "startLatitude": 9.9281, "startLongitude": -84.0907 }'
```
### Listar mis viajes
```bash
curl -X GET [http://localhost:6060/api/v1/trips/my](http://localhost:6060/api/v1/trips/my)
  -H "Authorization: Bearer "
```

### Finalizar viaje
```bash
curl -X PUT http://localhost:6060/api/v1/trips/1/finish
  -H "Authorization: Bearer " -H "Content-Type: application/json" -d '{ "endLatitude": 9.935, "endLongitude": -84.085, "distanceKm": 2.5 }'
```

### Listar todos los viajes como ADMIN
```bash
curl -X GET http://localhost:6060/api/v1/admin/trips
  -H "Authorization: Bearer <TOKEN_ADMIN>"
```

### Cancelar viaje como ADMIN
```bash
curl -X PUT http://localhost:6060/api/v1/admin/trips/1/cancel
  -H "Authorization: Bearer <TOKEN_ADMIN>"
```

## Tests

Actualmente existe un test básico de contexto (`contextLoads`):

```bash
./gradlew test
```

## Estructura rápida

- `src/main/kotlin/com/cyclix/cyclix_api/auth`: auth, JWT, security
- `src/main/kotlin/com/cyclix/cyclix_api/user`: usuarios, roles, estados
- `src/main/kotlin/com/cyclix/cyclix_api/trip`: viajes, estados de viaje y endpoints de usuario/admin
- `src/main/kotlin/com/cyclix/cyclix_api/support`: tickets de soporte
- `src/main/kotlin/com/cyclix/cyclix_api/common/error`: manejo global de errores
- `src/main/resources/db/migration`: migraciones Flyway
