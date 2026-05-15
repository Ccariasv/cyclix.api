# Cyclix API

Backend REST de Cyclix para autenticación, usuarios, soporte, viajes, puestos y bicicletas.

## Stack

- Kotlin `2.1.20`
- Spring Boot `3.4.5`
- Spring Security + JWT (stateless)
- Spring Data JPA
- Flyway
- MariaDB
- Springdoc OpenAPI (Swagger UI)
- Gradle Kotlin DSL

## Requisitos

- JDK `21`
- Docker + Docker Compose (opcional, recomendado)

## Configuración

Archivo: `src/main/resources/application.properties`

Variables principales:

- `server.port` (default `6060`)
- `SPRING_DATASOURCE_URL` (ejemplo compose: `jdbc:mariadb://mariadb:3306/DB_cyclix`)
- `SPRING_DATASOURCE_USERNAME` (default compose: `cyclix_admin`)
- `SPRING_DATASOURCE_PASSWORD` (default compose: `cyclix10`)
- `app.jwt.secret`
- `app.jwt.expiration-seconds` (default `86400`)

## Ejecutar proyecto

### Local

1. Levantar MariaDB local.
2. Ejecutar:

```bash
./gradlew bootRun
```

API disponible en `http://localhost:6060`.

### Docker Compose

```bash
docker compose --profile full up --build
```

Servicios:
- `mariadb` (puerto `3306`)
- `api` (puerto `6060`, profile `full`)

## OpenAPI / Swagger

- UI: `http://localhost:6060/swagger-ui/index.html`
- JSON: `http://localhost:6060/v3/api-docs`

## Seguridad

- Público:
  - `/api/v1/auth/**`
  - `/swagger-ui/**`
  - `/v3/api-docs/**`
- Resto de endpoints: requiere `Authorization: Bearer <token>`.

Roles soportados: `USER`, `ADMIN`.

## Migraciones Flyway

Directorio: `src/main/resources/db/migration`

- `V1__users_roles.sql`
- `V2__support_tickets.sql`
- `V3__seed_test_data.sql`
- `V4__fix_seed_user_passwords.sql`
- `V5__trips_module.sql`
- `V6__puesto_bicicleta_module.sql`

## Módulos y endpoints

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

`login` responde:

```json
{
  "token": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "email": "admin@cyclix.test"
}
```

### Usuarios

Base mapping:
- `/api/v1/get/user`
- `/get/user`

Endpoints:
- `GET /api/v1/get/user`
- `PATCH /api/v1/get/user/{userId}/status`
- `PATCH /api/v1/get/user/{userId}/role`

### Soporte

Usuario autenticado (`USER` o `ADMIN`):
- `POST /api/v1/support/tickets`
- `GET /api/v1/support/tickets/my`
- `GET /api/v1/support/tickets/{id}`

Admin (`ADMIN`):
- `GET /api/v1/admin/support/tickets`
- `PUT /api/v1/admin/support/tickets/{id}/status`
- `PUT /api/v1/admin/support/tickets/{id}/priority`

### Viajes

Usuario autenticado (`USER` o `ADMIN`):
- `POST /api/v1/trips`
- `GET /api/v1/trips/my`
- `GET /api/v1/trips/{id}`
- `PUT /api/v1/trips/{id}/finish`

Admin (`ADMIN`):
- `GET /api/v1/admin/trips`
- `GET /api/v1/admin/trips/{id}`
- `PUT /api/v1/admin/trips/{id}/cancel`

### Puestos

- `GET /api/v1/puestos`
- `GET /api/v1/puestos/activos`
- `GET /api/v1/puestos/disponibles`
- `GET /api/v1/puestos/{id}`
- `POST /api/v1/puestos` (`ADMIN`)
- `PUT /api/v1/puestos/{id}` (`ADMIN`)
- `PATCH /api/v1/puestos/{id}/estado?nuevoEstado=ACTIVO` (`ADMIN`)

Estados de puesto:
- `ACTIVO`
- `INACTIVO`
- `MANTENIMIENTO`

### Bicicletas

- `GET /api/v1/bicicletas`
- `GET /api/v1/bicicletas/filtrar?estado=DISPONIBLE`
- `GET /api/v1/bicicletas/filtrar?tipo=ELECTRICA`
- `GET /api/v1/bicicletas/sin-puesto` (`ADMIN`)
- `GET /api/v1/bicicletas/puesto/{puestoId}`
- `GET /api/v1/bicicletas/puesto/{puestoId}/disponibles`
- `GET /api/v1/bicicletas/{id}`
- `GET /api/v1/bicicletas/qr/{codigoQr}`
- `POST /api/v1/bicicletas` (`ADMIN`)
- `PUT /api/v1/bicicletas/{id}` (`ADMIN`)
- `PATCH /api/v1/bicicletas/{id}/estado`

Tipos de bicicleta:
- `URBANA`
- `MONTAÑA`
- `ELECTRICA`

Estados de bicicleta:
- `DISPONIBLE`
- `EN_USO`
- `MANTENIMIENTO`
- `FUERA_DE_SERVICIO`
- `RESERVADA`

## Reglas de negocio clave

- Registro crea usuario con rol `USER` y estado `ACTIVE`.
- Un usuario no puede tener más de un viaje `ACTIVE` al mismo tiempo.
- Un viaje solo se puede finalizar si está `ACTIVE`.
- Categoría `EMERGENCY` fuerza prioridad `CRITICAL`.
- En bicicletas:
  - `codigo` es único.
  - `puesto` es opcional.
  - Al mover bicicletas entre puestos se ajusta capacidad disponible.
  - No se puede cambiar estado de una bicicleta `FUERA_DE_SERVICIO`.

## Datos semilla

Usuarios:
- `admin@cyclix.test` / `Test1234*` (`ADMIN`)
- `laura@cyclix.test` / `Test1234*` (`USER`)
- `carlos@cyclix.test` / `Test1234*` (`USER`)

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

### Crear puesto (ADMIN)

```bash
curl -X POST http://localhost:6060/api/v1/puestos \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Puesto Centro",
    "codigo": "PST-001",
    "direccion": "Centro de San José",
    "latitud": 9.9281,
    "longitud": -84.0907,
    "capacidadTotal": 20
  }'
```

### Crear bicicleta (ADMIN)

```bash
curl -X POST http://localhost:6060/api/v1/bicicletas \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{
    "codigo": "BIC-001",
    "marca": "Trek",
    "modelo": "FX 2",
    "color": "Negro",
    "tipo": "URBANA",
    "tamanoLlanta": 29.0,
    "precioPorHora": 1200.00,
    "puestoId": 1
  }'
```

### Crear viaje

```bash
curl -X POST http://localhost:6060/api/v1/trips \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "bikeId": 1,
    "startLatitude": 9.9281,
    "startLongitude": -84.0907
  }'
```

### Crear ticket

```bash
curl -X POST http://localhost:6060/api/v1/support/tickets \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "APP",
    "priority": "MEDIUM",
    "title": "Error al abrir mapa",
    "description": "La app se cierra al iniciar viaje"
  }'
```

## Tests

```bash
./gradlew test
```

## Estructura del código

- `src/main/kotlin/com/cyclix/cyclix_api/auth`
- `src/main/kotlin/com/cyclix/cyclix_api/user`
- `src/main/kotlin/com/cyclix/cyclix_api/support`
- `src/main/kotlin/com/cyclix/cyclix_api/trip`
- `src/main/kotlin/com/cyclix/cyclix_api/puesto`
- `src/main/kotlin/com/cyclix/cyclix_api/bicycle`
- `src/main/resources/db/migration`
