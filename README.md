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
- `app.device.api-key` (llave compartida para dispositivos ESP32)

## Ejecutar proyecto

### Local

1. Tener Docker y Docker Compose instalados.
2. Ejecutar:

```bash
./gradlew bootRun
```

Spring Boot levantara `docker-compose.yaml`, iniciara `mariadb` y tomara la conexion del servicio automaticamente durante desarrollo.

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
  - `/api/v1/device/**`
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

## Modelo de datos (tablas)

### Tabla `roles`
- `id` (PK, bigint)
- `name` (varchar 50, unique) -> `USER` | `ADMIN`
- `description` (varchar 255, nullable)
- `created_at`, `updated_at` (timestamp)

### Tabla `user_statuses`
- `id` (PK, bigint)
- `name` (varchar 50, unique) -> `ACTIVE` | `INACTIVE`
- `description` (varchar 255, nullable)
- `created_at`, `updated_at`

### Tabla `user`
- `id` (PK)
- `first_name` (varchar 100)
- `last_name` (varchar 100, nullable)
- `email` (varchar 150, unique)
- `phone` (varchar 30, nullable)
- `password_hash` (varchar 255)
- `role_id` (FK -> `roles.id`)
- `status_id` (FK -> `user_statuses.id`)
- `email_verified` (boolean)
- `last_login_at` (timestamp, nullable)
- `created_at`, `updated_at`

### Tabla `ticket_categories` (apoyo)
- `name` (PK, varchar 50): `BIKE`, `APP`, `PAYMENT`, `ACCOUNT`, `TRIP`, `EMERGENCY`, `OTHER`
- `description` (varchar 255, nullable)

### Tabla `ticket_priorities` (apoyo)
- `name` (PK, varchar 50): `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `description` (varchar 255, nullable)

### Tabla `ticket_statuses` (apoyo)
- `name` (PK, varchar 50): `OPEN`, `IN_PROGRESS`, `WAITING_USER`, `RESOLVED`, `CLOSED`
- `description` (varchar 255, nullable)

### Tabla `support_tickets`
- `id` (PK)
- `user_id` (FK -> `user.id`)
- `bike_id`, `trip_id`, `payment_id` (bigint, nullable, referencias lógicas)
- `category` (FK -> `ticket_categories.name`)
- `priority` (FK -> `ticket_priorities.name`)
- `status` (FK -> `ticket_statuses.name`)
- `title` (varchar 180)
- `description` (text)
- `created_at`, `updated_at`

### Tabla `trip_statuses` (apoyo)
- `name` (PK, varchar 50): `ACTIVE`, `COMPLETED`, `CANCELLED`
- `description` (varchar 255, nullable)

### Tabla `trips`
- `id` (PK)
- `user_id` (FK -> `user.id`)
- `bike_id` (bigint)
- `status` (FK -> `trip_statuses.name`)
- `start_latitude`, `start_longitude` (decimal 10,7)
- `end_latitude`, `end_longitude` (decimal 10,7, nullable)
- `started_at`, `ended_at` (timestamp, `ended_at` nullable)
- `distance_km` (decimal 10,2, nullable)
- `duration_seconds` (bigint, nullable)
- `created_at`, `updated_at`

### Tabla `puesto`
- `id` (PK)
- `nombre` (varchar 100)
- `codigo` (varchar 50, unique)
- `direccion` (varchar 255)
- `latitud`, `longitud` (decimal 10,7)
- `capacidad_total` (int, 1..100)
- `capacidad_disponible` (int, 0..capacidad_total)
- `estado` (varchar 30): `ACTIVO`, `INACTIVO`, `MANTENIMIENTO`
- `created_at`, `updated_at`

### Tabla `bicicleta`
- `id` (PK)
- `codigo` (varchar 50, unique)
- `marca`, `modelo` (varchar 100)
- `color` (varchar 50)
- `tipo` (varchar 30): `URBANA`, `MONTAÑA`, `ELECTRICA`
- `tamano_llanta` (double)
- `precio_por_hora` (decimal 10,2)
- `estado` (varchar 30): `DISPONIBLE`, `EN_USO`, `MANTENIMIENTO`, `FUERA_DE_SERVICIO`, `RESERVADA`
- `codigo_qr` (varchar 255, nullable, unique)
- `puesto_id` (FK nullable -> `puesto.id`)
- `created_at`, `updated_at`

## Módulos y endpoints

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

Notas:
- `register` crea usuarios con rol `USER` y estado `ACTIVE`.
- `login` solo permite acceso a usuarios con estado `ACTIVE`.

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

Payloads:

- `POST /api/v1/auth/register` request
```json
{
  "firstName": "Diego",
  "lastName": "Carias",
  "email": "diego@correo.com",
  "phone": "88880000",
  "password": "Test1234*"
}
```
- `POST /api/v1/auth/register` response
```json
{ "message": "Usuario registrado correctamente", "userId": 10 }
```
- `POST /api/v1/auth/login` request
```json
{ "email": "admin@cyclix.test", "password": "Test1234*" }
```

### Flujo de desbloqueo con ESP32

Flujo recomendado:

1. La app escanea el `QR` o `NFC` de la bicicleta.
2. La app identifica la bicicleta.
   - Para QR ya existe `GET /api/v1/bicicletas/qr/{codigoQr}`.
3. La bicicleta y la estación abren sus WebSocket hacia la API:
   - bicicleta GPS:
     `ws://<host>:6060/ws/device?clientType=BIKE&bikeId=<bikeId>&apiKey=<app.device.api-key>`
   - estación de desbloqueo:
     `ws://<host>:6060/ws/device?clientType=STATION&stationId=<stationId>&apiKey=<app.device.api-key>`
   - también puede enviar la llave en el header `X-Device-Api-Key`
4. La app autenticada inicia el viaje con `POST /api/v1/trips`.
5. Si el viaje se crea, el backend deja:
   - viaje en estado `ACTIVE`
   - bicicleta en estado `EN_USO`
6. Después del commit de la transacción, la API empuja por WebSocket un mensaje `UNLOCK` a la estación asociada a la bicicleta.
7. Si no hay estación conectada, la API usa fallback al socket de la bicicleta.
8. La bicicleta puede enviar su ubicación en tiempo real por el mismo socket con mensajes `LOCATION_UPDATE`.
9. La auditoría de ruta no persiste cada mensaje: por defecto guarda puntos activos cada `15m` o `10s`, y fuera de viaje cada `30m` o `60s`.

Mensaje inicial al conectar:

```json
{
  "type": "CONNECTED",
  "bikeId": 7,
  "stationId": null,
  "tripId": null,
  "userId": null,
  "latitude": null,
  "longitude": null,
  "message": "Bicicleta conectada",
  "sentAt": "2026-05-22T10:30:45"
}
```

Mensaje de desbloqueo:

```json
{
  "type": "UNLOCK",
  "bikeId": 7,
  "stationId": 3,
  "tripId": 15,
  "userId": 21,
  "latitude": null,
  "longitude": null,
  "message": "Desbloqueo autorizado",
  "sentAt": "2026-05-22T10:30:45"
}
```

Mensaje de ubicación desde bicicleta:

```json
{
  "type": "LOCATION_UPDATE",
  "latitude": 14.9722,
  "longitude": -89.5305,
  "recordedAt": "2026-05-27T12:15:30"
}
```

Mensaje de confirmación cuando un punto fue auditado:

```json
{
  "type": "LOCATION_AUDITED",
  "bikeId": 7,
  "stationId": 3,
  "tripId": 15,
  "latitude": 14.9722,
  "longitude": -89.5305,
  "recordedAt": "2026-05-27T12:15:30",
  "message": "Ubicacion auditada",
  "sentAt": "2026-05-27T12:15:31"
}
```

### Fallback HTTP para dispositivos

También queda disponible `POST /api/v1/device/bikes/{bikeId}/unlock` como fallback por HTTP si un dispositivo no usa WebSocket.

### Usuarios

Base mapping:
- `/api/v1/get/user`
- `/get/user`

Acceso:
- Solo `ADMIN`

Endpoints:
- `GET /api/v1/get/user`
- `PATCH /api/v1/get/user/{userId}/status`
- `PATCH /api/v1/get/user/{userId}/role`

Payloads:
- `PATCH .../status`
```json
{ "status": "ACTIVE" }
```
- `PATCH .../role`
```json
{ "role": "ADMIN" }
```

### Soporte

Usuario autenticado (`USER` o `ADMIN`):
- `POST /api/v1/support/tickets`
- `GET /api/v1/support/tickets/my`
- `GET /api/v1/support/tickets/{id}`

Admin (`ADMIN`):
- `GET /api/v1/admin/support/tickets`
- `PUT /api/v1/admin/support/tickets/{id}/status`
- `PUT /api/v1/admin/support/tickets/{id}/priority`

Payloads:
- `POST /api/v1/support/tickets` request
```json
{
  "bikeId": 1,
  "tripId": 20,
  "paymentId": null,
  "category": "APP",
  "priority": "MEDIUM",
  "title": "Error al abrir mapa",
  "description": "La app se cierra al iniciar viaje"
}
```
- `PUT /api/v1/admin/support/tickets/{id}/status`
```json
{ "status": "IN_PROGRESS" }
```
- `PUT /api/v1/admin/support/tickets/{id}/priority`
```json
{ "priority": "HIGH" }
```

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

Payloads:
- `POST /api/v1/trips`
```json
{
  "bikeId": 1,
  "startLatitude": 9.9281,
  "startLongitude": -84.0907
}
```
- `PUT /api/v1/trips/{id}/finish`
```json
{
  "endLatitude": 9.9350,
  "endLongitude": -84.0850,
  "distanceKm": 2.50
}
```

### Wallet

Usuario autenticado (`USER` o `ADMIN`):
- `GET /api/v1/wallet/my`
- `GET /api/v1/wallet/my/transactions`
- `POST /api/v1/wallet/my/top-up`

Admin (`ADMIN`):
- `POST /api/v1/wallet/top-up`

Payloads:
- `POST /api/v1/wallet/my/top-up`
```json
{
  "amount": 100.00,
  "paymentMethod": "CARD"
}
```
- `POST /api/v1/wallet/top-up`
```json
{
  "userId": 15,
  "amount": 100.00
}
```

### Puestos

- `GET /api/v1/puestos`
- `GET /api/v1/puestos/activos`
- `GET /api/v1/puestos/disponibles`
- `GET /api/v1/puestos/{id}`
- `POST /api/v1/puestos` (`ADMIN`)
- `PUT /api/v1/puestos/{id}` (`ADMIN`)
- `PATCH /api/v1/puestos/{id}/estado?nuevoEstado=ACTIVO` (`ADMIN`)

Payloads:
- `POST/PUT /api/v1/puestos`
```json
{
  "nombre": "Puesto Centro",
  "codigo": "PST-001",
  "direccion": "Centro de San José",
  "latitud": 9.9281,
  "longitud": -84.0907,
  "capacidadTotal": 20
}
```

Estados de puesto:
- `ACTIVO`
- `INACTIVO`
- `MANTENIMIENTO`

### Bicicletas

- `GET /api/v1/bicicletas` (publico)
- `GET /api/v1/bicicletas?estado=DISPONIBLE` (publico)
- `GET /api/v1/bicicletas?tipo=ELECTRICA` (publico)
- `GET /api/v1/bicicletas?puestoId=1` (publico)
- `GET /api/v1/bicicletas?puestoId=1&soloDisponibles=true` (publico)
- `GET /api/v1/bicicletas/filtrar?estado=DISPONIBLE` (publico, compatibilidad)
- `GET /api/v1/bicicletas/filtrar?tipo=ELECTRICA` (publico, compatibilidad)
- `GET /api/v1/bicicletas/sin-puesto` (`ADMIN`)
- `GET /api/v1/bicicletas/puesto/{puestoId}`
- `GET /api/v1/bicicletas/puesto/{puestoId}/disponibles`
- `GET /api/v1/bicicletas/{id}`
- `GET /api/v1/bicicletas/qr/{codigoQr}`
- `POST /api/v1/bicicletas` (`ADMIN`)
- `PUT /api/v1/bicicletas/{id}` (`ADMIN`)
- `PATCH /api/v1/bicicletas/{id}/estado`
- `DELETE /api/v1/bicicletas/{id}` (`ADMIN`, baja logica)

Payloads:
- `POST/PUT /api/v1/bicicletas`
```json
{
  "codigo": "BIC-001",
  "marca": "Trek",
  "modelo": "FX 2",
  "color": "Negro",
  "tipo": "URBANA",
  "tamanoLlanta": 29.0,
  "precioPorHora": 1200.00,
  "puestoId": 1
}
```
- `PATCH /api/v1/bicicletas/{id}/estado`
```json
{
  "nuevoEstado": "MANTENIMIENTO",
  "puestoId": null
}
```

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
- Login rechaza usuarios con estado distinto de `ACTIVE`.
- Los endpoints de gestión de usuarios solo pueden ser usados por `ADMIN`.
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
