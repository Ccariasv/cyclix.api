# Cyclix API – Documentación y Uso de Endpoints

## Introducción

Esta API gestiona autenticación, usuarios y tickets de soporte para la plataforma Cyclix. Usa Spring Boot, MariaDB y JWT para la autenticación.

- Repositorio: [Ccariasv/cyclix.api](https://github.com/Ccariasv/cyclix.api)
- OpenAPI UI: [https://api.cyclix.site/swagger-ui/index.html](https://api.cyclix.site/swagger-ui/index.html)
- Base_URL https://api.cyclix.site/
---

## Autenticación y Seguridad

- **Autenticación:** JWT Bearer en el header `Authorization: Bearer <token>`.
- **Endpoints públicos:**  
  - `/api/v1/auth/**`  
  - `/swagger-ui/**`  
  - `/v3/api-docs/**`
- **El resto de los endpoints requieren token.**
- **Roles:** `USER` y `ADMIN`.
- **Estados de usuario:** `ACTIVE`, `INACTIVE`.

**Ejemplo de login y obtención de token:**
```bash
curl -X POST http://localhost:6060/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cyclix.test","password":"Test1234*"}'
```

---

## Endpoints Principales

### 1. Autenticación

- `POST /api/v1/auth/register`  
  Registro de usuario.  
  **Body:** `{ "firstName": "...", "lastName": "...", "email": "...", "phone": "...", "password": "..." }`

- `POST /api/v1/auth/login`  
  Login y obtiene JWT.  
  **Body:** `{ "email": "...", "password": "..." }`  
  **Respuesta:** `{ "token": "...", ... }`

---

### 2. Usuarios

- `GET /api/v1/get/user`  
  Lista todos los usuarios.

- `PATCH /api/v1/get/user/{userId}/status`  
  Cambia el estado de un usuario (`ACTIVE`/`INACTIVE`).  
  **Body:** `{ "status": "ACTIVE" }`

- `PATCH /api/v1/get/user/{userId}/role`  
  Cambia el rol (`USER`/`ADMIN`).  
  **Body:** `{ "role": "ADMIN" }`

**Solo ADMIN puede cambiar rol/estado de otros usuarios.**

---

### 3. Soporte

#### Usuario autenticado (`USER` o `ADMIN`)

- `POST /api/v1/support/tickets`  
  Crea ticket.  
  **Body ejemplo:**  
  ```json
  {
    "category": "APP",
    "priority": "MEDIUM",
    "title": "Error al abrir mapa",
    "description": "La app se cierra en Android al iniciar viaje"
  }
  ```

- `GET /api/v1/support/tickets/my`  
  Lista tickets propios.

- `GET /api/v1/support/tickets/{id}`  
  Ve un ticket propio.

#### Administración (`ADMIN`)

- `GET /api/v1/admin/support/tickets`  
  Lista todos los tickets.

- `PUT /api/v1/admin/support/tickets/{id}/status`  
  Cambia el estado de un ticket.  
  **Body:** `{ "status": "IN_PROGRESS" }`

- `PUT /api/v1/admin/support/tickets/{id}/priority`  
  Cambia la prioridad de un ticket.  
  **Body:** `{ "priority": "HIGH" }`

---

## Datos de apoyo (tablas base)

Estos valores están en la base de datos como catálogo y son usados por la API.

### Categorías de Tickets (`ticket_categories`)
- `BIKE`: Incidencias relacionadas a bicicletas
- `APP`: Problemas de aplicación o plataforma
- `PAYMENT`: Incidencias de cobros o pagos
- `ACCOUNT`: Problemas de cuenta de usuario
- `TRIP`: Problemas en viajes
- `EMERGENCY`: Emergencias operativas
- `OTHER`: Otras incidencias

### Prioridades (`ticket_priorities`)
- `LOW`: Baja
- `MEDIUM`: Media
- `HIGH`: Alta
- `CRITICAL`: Crítica

### Estados de Tickets (`ticket_statuses`)
- `OPEN`: Abierto
- `IN_PROGRESS`: En progreso
- `WAITING_USER`: Esperando respuesta del usuario
- `RESOLVED`: Resuelto
- `CLOSED`: Cerrado

### Roles de Usuarios (`roles`)
- `USER`: Usuario final
- `ADMIN`: Administrador

### Estados de Usuario (`user_statuses`)
- `ACTIVE`: Usuario activo
- `INACTIVE`: Usuario inactivo

---

## Negocio y reglas destacadas

- El registro genera usuario con rol `USER` y estado `ACTIVE`.
- Un ticket de categoría `EMERGENCY` siempre tiene prioridad `CRITICAL`.
- La referencia a IDs como `bikeId`, `tripId`, `paymentId` se valida en tablas correspondientes.
- JWT es obligatorio excepto en endpoints públicos.
- Formato estándar de error en las respuestas, incluye mensaje human-readable y detalles del error.

---

## Datos Seed y ejemplos

Usuarios de ejemplo:
- `admin@cyclix.test` / `Test1234*` (`ADMIN`)
- `laura@cyclix.test` / `Test1234*` (`USER`)
- `carlos@cyclix.test` / `Test1234*` (`USER`)

Ejemplo de creación de ticket:
```bash
curl -X POST http://localhost:6060/api/v1/support/tickets \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"category":"APP","priority":"MEDIUM","title":"Error app","description":"Se cierra al abrir mapa"}'
```

---

## API interactiva

Consulta y prueba todos los endpoints desde [Swagger UI](http://localhost:6060/swagger-ui)
