# Módulo de Mantenimiento

## Objetivo

El módulo de mantenimiento administra el proceso operativo interno para revisar, reparar y liberar bicicletas.

Este módulo es **separado** de los tickets de soporte:

- `support_tickets`: registran la incidencia reportada
- `maintenance_orders`: gestionan el trabajo operativo de mantenimiento

Un ticket de categoría `BIKE` puede iniciar una orden de mantenimiento desde el panel administrativo.

---

## Alcance funcional

El módulo permite:

- crear órdenes de mantenimiento manualmente
- crear órdenes de mantenimiento a partir de un ticket `BIKE`
- asignar una bicicleta a un técnico de mantenimiento
- cambiar automáticamente la bicicleta a estado `MANTENIMIENTO`
- mostrar en la app móvil las bicicletas asignadas a cada técnico
- registrar diagnóstico, notas, ubicación actual y tiempo estimado
- resolver la orden dejando la bicicleta:
  - disponible
  - aún en mantenimiento
  - fuera de servicio
- guardar historial por orden

---

## Roles y acceso

### `ADMIN`

Puede:

- listar todas las órdenes
- ver detalle de cualquier orden
- crear órdenes manuales
- crear órdenes desde tickets de bicicleta
- asignar técnicos
- actualizar progreso
- resolver órdenes

### `MAINTENANCE`

Puede:

- ver únicamente sus órdenes asignadas
- ver el detalle de sus órdenes
- actualizar progreso de sus órdenes
- resolver sus órdenes

La autenticación usa el JWT actual del proyecto.  
Los endpoints están protegidos con `@PreAuthorize(...)`.

---

## Modelo de datos

### Tabla `maintenance_orders`

Representa la orden principal de mantenimiento.

Campos principales:

- `id`
- `ticket_id` nullable
- `bike_id`
- `assigned_to_user_id` nullable
- `created_by_user_id`
- `priority`
- `type`
- `status`
- `result_status` nullable
- `reported_issue`
- `diagnosis` nullable
- `resolution_notes` nullable
- `current_location` nullable
- `estimated_minutes` nullable
- `out_of_service_reason` nullable
- `assigned_at` nullable
- `started_at` nullable
- `completed_at` nullable
- `created_at`
- `updated_at`

### Tabla `maintenance_order_history`

Guarda el historial funcional de la orden.

Campos principales:

- `id`
- `maintenance_order_id`
- `changed_by_user_id`
- `action`
- `previous_status`
- `new_status`
- `note`
- `created_at`

---

## Catálogos lógicos

### Prioridades

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

### Tipos de mantenimiento

- `CORRECTIVE`
- `PREVENTIVE`
- `INSPECTION`
- `BRAKES`
- `TIRES`
- `CHAIN`
- `ELECTRICAL`
- `BATTERY`
- `FRAME`
- `GENERAL`

### Estados de la orden

- `PENDING`
- `ASSIGNED`
- `IN_REVIEW`
- `IN_REPAIR`
- `WAITING_PARTS`
- `PAUSED`
- `FINALIZED`

### Resultado final de la orden

- `STAYS_IN_MAINTENANCE`
- `AVAILABLE`
- `OUT_OF_SERVICE`

---

## Reglas de negocio

### Separación con soporte

- Un ticket de soporte no reemplaza una orden de mantenimiento.
- Una orden puede nacer manualmente o desde un ticket `BIKE`.
- Un ticket solo puede tener **una** orden de mantenimiento asociada.

### Reglas sobre la bicicleta

- Al crear una orden, la bicicleta pasa automáticamente a `MANTENIMIENTO`.
- Una bicicleta `EN_USO` o `RESERVADA` no puede entrar a mantenimiento.
- Si ya existe una orden activa para una bicicleta, no se puede crear otra.
- Si la orden se resuelve con:
  - `AVAILABLE`: la bicicleta pasa a `DISPONIBLE`
  - `STAYS_IN_MAINTENANCE`: la bicicleta sigue en `MANTENIMIENTO`
  - `OUT_OF_SERVICE`: la bicicleta pasa a `FUERA_DE_SERVICIO`

### Reglas sobre técnicos

- El usuario asignado debe tener rol `MAINTENANCE`.
- El usuario asignado debe estar `ACTIVE`.
- El técnico solo puede operar sus propias órdenes.

### Reglas sobre tickets

- Solo tickets con categoría `BIKE` pueden iniciar mantenimiento.
- El ticket debe tener `bike_id`.
- Al crear una orden desde ticket, el ticket pasa a `IN_PROGRESS`.
- Al resolver la orden con `AVAILABLE` o `OUT_OF_SERVICE`, el ticket pasa a `RESOLVED`.

### Reglas sobre cierre

- No se puede usar el endpoint de progreso para marcar `FINALIZED`.
- Para cerrar una orden debe usarse el endpoint de resolución.
- Si el resultado es `OUT_OF_SERVICE`, el motivo es obligatorio.

---

## Flujo recomendado

### Flujo desde ticket

1. Usuario crea ticket de categoría `BIKE`.
2. Admin revisa el reporte.
3. Admin llama el endpoint para iniciar mantenimiento desde el ticket.
4. Se crea la orden.
5. La bicicleta pasa a `MANTENIMIENTO`.
6. El ticket pasa a `IN_PROGRESS`.
7. Se asigna un técnico.
8. El técnico actualiza diagnóstico, estado y notas.
9. El técnico o admin resuelve la orden.
10. La bicicleta queda en el estado final correspondiente.

### Flujo manual

1. Admin detecta una necesidad operativa.
2. Crea la orden manualmente.
3. La bicicleta pasa a `MANTENIMIENTO`.
4. Se asigna técnico.
5. Se trabaja la orden hasta su resolución.

---

## Endpoints

## Endpoints para `ADMIN`

### `GET /api/v1/admin/maintenance/orders`

Lista todas las órdenes.

### `GET /api/v1/admin/maintenance/orders/{id}`

Obtiene el detalle completo de una orden, incluyendo historial.

### `POST /api/v1/admin/maintenance/orders`

Crea una orden manual.

Request:

```json
{
  "bikeId": 1,
  "assignedToUserId": 7,
  "priority": "HIGH",
  "type": "CORRECTIVE",
  "reportedIssue": "La bicicleta presenta falla en el freno trasero.",
  "estimatedMinutes": 45,
  "currentLocation": "Taller central"
}
```

### `POST /api/v1/admin/maintenance/orders/from-ticket/{ticketId}`

Crea una orden desde un ticket `BIKE`.

Request:

```json
{
  "assignedToUserId": 7,
  "priority": "HIGH",
  "type": "CORRECTIVE",
  "estimatedMinutes": 60,
  "currentLocation": "Puesto Centro 01"
}
```

### `POST /api/v1/admin/support/failure-reports/{id}/maintenance`

Atajo funcional para iniciar mantenimiento directamente desde el módulo de reportes de falla.

Request:

```json
{
  "assignedToUserId": 7,
  "priority": "HIGH",
  "type": "BRAKES",
  "estimatedMinutes": 40,
  "currentLocation": "Puesto Norte 02"
}
```

### `PUT /api/v1/admin/maintenance/orders/{id}/assign`

Asigna o reasigna técnico.

Request:

```json
{
  "assignedToUserId": 7,
  "estimatedMinutes": 90
}
```

### `PATCH /api/v1/admin/maintenance/orders/{id}/progress`

Actualiza progreso de la orden.

Request:

```json
{
  "status": "IN_REPAIR",
  "diagnosis": "Pastillas de freno desgastadas y cable flojo.",
  "resolutionNotes": "Se inició cambio de piezas.",
  "currentLocation": "Taller central",
  "estimatedMinutes": 50,
  "note": "Se detectó daño adicional en la maneta."
}
```

### `PATCH /api/v1/admin/maintenance/orders/{id}/resolve`

Resuelve la orden.

Ejemplo dejando la bicicleta disponible:

```json
{
  "resultStatus": "AVAILABLE",
  "resolutionNotes": "Se reemplazaron pastillas, se ajustó cable y se probó el frenado.",
  "currentLocation": "Taller central"
}
```

Ejemplo dejando la bicicleta fuera de servicio:

```json
{
  "resultStatus": "OUT_OF_SERVICE",
  "resolutionNotes": "Se detectó daño estructural irreversible en el cuadro.",
  "outOfServiceReason": "Fisura crítica en el cuadro",
  "currentLocation": "Bodega operativa"
}
```

## Endpoints para `MAINTENANCE`

### `GET /api/v1/maintenance/orders/my`

Lista las órdenes asignadas al técnico autenticado.

### `GET /api/v1/maintenance/orders/{id}`

Obtiene el detalle de una orden asignada al técnico autenticado.

### `PATCH /api/v1/maintenance/orders/{id}/progress`

Actualiza progreso de una orden asignada.

Request:

```json
{
  "status": "IN_REVIEW",
  "diagnosis": "La llanta trasera está pinchada.",
  "currentLocation": "Puesto Sur 04",
  "note": "Se requiere desmontaje para validar rin."
}
```

### `PATCH /api/v1/maintenance/orders/{id}/resolve`

Resuelve una orden asignada.

Ejemplo dejando la bicicleta aún en mantenimiento:

```json
{
  "resultStatus": "STAYS_IN_MAINTENANCE",
  "resolutionNotes": "Se dejó pendiente por falta de repuesto.",
  "currentLocation": "Taller central"
}
```

---

## Respuesta esperada

Las respuestas del módulo incluyen información de:

- orden
- bicicleta
- técnico asignado
- usuario creador
- estado actual
- prioridad
- tipo
- ubicación
- tiempos
- historial

Ejemplo resumido:

```json
{
  "id": 12,
  "ticketId": 33,
  "bike": {
    "id": 1,
    "codigo": "BIC-001",
    "marca": "Trek",
    "modelo": "City 500",
    "tipo": "URBANA",
    "estado": "MANTENIMIENTO",
    "puesto": {
      "id": 3,
      "nombre": "Puesto Centro",
      "codigo": "PC-01",
      "direccion": "Zona 1",
      "latitud": 14.6349,
      "longitud": -90.5069
    }
  },
  "assignedTo": {
    "id": 7,
    "firstName": "Luis",
    "lastName": "Pérez",
    "email": "luis@cyclix.com"
  },
  "priority": "HIGH",
  "type": "CORRECTIVE",
  "status": "IN_REPAIR",
  "resultStatus": null,
  "reportedIssue": "La bicicleta presenta ruido y falla al frenar.",
  "diagnosis": "Desgaste en frenos traseros.",
  "resolutionNotes": "Cambio de piezas en proceso.",
  "currentLocation": "Taller central",
  "estimatedMinutes": 45,
  "outOfServiceReason": null,
  "assignedAt": "2026-05-21T10:15:00",
  "startedAt": "2026-05-21T10:40:00",
  "completedAt": null,
  "createdAt": "2026-05-21T10:10:00",
  "updatedAt": "2026-05-21T10:42:00",
  "history": [
    {
      "id": 1,
      "action": "CREATED",
      "previousStatus": null,
      "newStatus": "ASSIGNED",
      "note": "Orden creada desde ticket #33.",
      "changedBy": {
        "id": 2,
        "firstName": "Admin",
        "lastName": "Principal",
        "email": "admin@cyclix.com"
      },
      "createdAt": "2026-05-21T10:10:00"
    }
  ]
}
```

---

## Archivos del módulo

### Migración

- [V8__maintenance_module.sql](/Users/diego/IdeaProjects/cyclix-api/src/main/resources/db/migration/V8__maintenance_module.sql)

### Controladores

- [AdminMaintenanceOrderController.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/maintenance/controller/AdminMaintenanceOrderController.kt)
- [MaintenanceOrderController.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/maintenance/controller/MaintenanceOrderController.kt)
- [AdminFailureReportController.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/support/controller/AdminFailureReportController.kt)

### Servicio

- [MaintenanceOrderService.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/maintenance/service/MaintenanceOrderService.kt)

### Entidades

- [MaintenanceOrder.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/maintenance/entity/MaintenanceOrder.kt)
- [MaintenanceOrderHistory.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/maintenance/entity/MaintenanceOrderHistory.kt)

### DTOs

- [MaintenanceDtos.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/maintenance/dto/MaintenanceDtos.kt)

### Repositorios

- [MaintenanceOrderRepository.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/maintenance/repository/MaintenanceOrderRepository.kt)
- [MaintenanceOrderHistoryRepository.kt](/Users/diego/IdeaProjects/cyclix-api/src/main/kotlin/com/cyclix/cyclix_api/maintenance/repository/MaintenanceOrderHistoryRepository.kt)

---

## Notas para móvil

La app de mantenimiento debería usar principalmente:

- `GET /api/v1/maintenance/orders/my`
- `GET /api/v1/maintenance/orders/{id}`
- `PATCH /api/v1/maintenance/orders/{id}/progress`
- `PATCH /api/v1/maintenance/orders/{id}/resolve`

La pantalla de detalle debería mostrar:

- código de bicicleta
- ubicación actual
- descripción del problema
- prioridad
- tipo de mantenimiento
- estado de la orden
- diagnóstico
- notas
- tiempo estimado
- historial
