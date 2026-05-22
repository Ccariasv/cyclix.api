# Frontend Integration: Tarifas, Suscripciones y Wallet

Este documento describe los contratos ya implementados para frontend.

## Resumen funcional

- El costo de viaje se calcula al finalizar (`PUT /api/v1/trips/{id}/finish`).
- Se consumen minutos de suscripción activos primero.
- Los minutos restantes se cobran por regla de tarifa aplicable (fecha/hora/día/festivo).
- El cobro sale del wallet.
- Si el wallet no alcanza, la finalización responde error (`402 Payment Required`).
- Todo queda auditado en backend.

## Endpoints nuevos

## 1) Tarifas y festivos (ADMIN)

Base: `/api/v1/admin/pricing`

- `GET /rules`
- `POST /rules`
- `PUT /rules/{id}`
- `GET /holidays`
- `POST /holidays`
- `PUT /holidays/{id}`

### `PricingRuleRequest`

```json
{
  "name": "Tarifa nocturna",
  "priority": 10,
  "active": true,
  "baseFare": 25.00,
  "includedMinutes": 120,
  "extraFarePerBlock": 8.00,
  "extraBlockMinutes": 30,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "startTime": "22:00:00",
  "endTime": "06:00:00",
  "daysOfWeek": "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY",
  "holidayMode": "ANY"
}
```

`holidayMode`: `ANY | HOLIDAY_ONLY | NON_HOLIDAY`

### `HolidayRequest`

```json
{
  "holidayDate": "2026-09-15",
  "name": "Independencia",
  "active": true
}
```

## 2) Suscripciones (ADMIN)

Base: `/api/v1/admin/subscriptions`

- `GET /plans`
- `POST /plans`
- `PUT /plans/{id}`
- `POST /assign`

### `SubscriptionPlanRequest`

```json
{
  "name": "Plan Plus 50h",
  "monthlyPrice": 200.00,
  "includedHours": 50,
  "active": true
}
```

### `AssignSubscriptionRequest`

```json
{
  "userId": 15,
  "planId": 2,
  "startsAt": "2026-05-01T00:00:00",
  "expiresAt": "2026-05-31T23:59:59",
  "autoRenew": false
}
```

## 3) Wallet

Base: `/api/v1/wallet`

- `GET /my` (`USER` o `ADMIN`)
- `GET /my/transactions` (`USER` o `ADMIN`)
- `POST /my/top-up` (`USER` o `ADMIN`) pago simulado para recarga propia
- `POST /top-up` (`ADMIN`)

### `WalletSelfTopUpRequest`

```json
{
  "amount": 100.00,
  "paymentMethod": "CARD"
}
```

`paymentMethod`: `CARD | TRANSFER | CASH`

### `WalletTopUpRequest`

```json
{
  "userId": 15,
  "amount": 100.00
}
```

## 4) Viajes (ya existente, con response extendido)

- `PUT /api/v1/trips/{id}/finish`

El response ahora incluye:

- `pricingRuleId`
- `pricingRuleName`
- `subscriptionApplied`
- `subscriptionMinutesCovered`
- `billableMinutes`
- `baseFareApplied`
- `includedMinutesApplied`
- `extraFarePerBlockApplied`
- `extraBlockMinutesApplied`
- `extraAmount`
- `totalAmount`
- `walletChargedAmount`

Ejemplo parcial:

```json
{
  "id": 99,
  "status": "COMPLETED",
  "durationSeconds": 9900,
  "subscriptionApplied": true,
  "subscriptionMinutesCovered": 120,
  "billableMinutes": 45,
  "pricingRuleName": "Tarifa estándar",
  "baseFareApplied": 20.00,
  "extraAmount": 10.00,
  "totalAmount": 30.00,
  "walletChargedAmount": 30.00
}
```

## Reglas de cálculo para UI

- Minutos de viaje: tiempo real.
- Si hay suscripción activa, cubre primero (`remainingMinutes`).
- Lo no cubierto pasa a cobro por tarifa.
- Si minutos facturables > incluidos por tarifa:
  - `blocks = ceil((billable - includedMinutes) / extraBlockMinutes)`
  - `extra = blocks * extraFarePerBlock`
  - `total = baseFare + extra`
- Si wallet insuficiente al finalizar: error `402`.

## Códigos de error esperados

- `400`: request inválido.
- `401`: no autenticado.
- `403`: no autorizado.
- `404`: recurso no encontrado.
- `409`: conflicto de negocio.
- `402`: saldo insuficiente en wallet para finalizar viaje.

## Notas para frontend

- Mostrar desglose en pantalla de finalización con:
  - minutos totales,
  - minutos cubiertos por plan,
  - minutos cobrados,
  - total final.
- Antes de finalizar viaje, pueden consultar wallet (`GET /wallet/my`) para anticipar insuficiencia.
- Para pruebas o entorno demo, el usuario autenticado puede recargar su propio wallet con `POST /wallet/my/top-up` sin intervención de admin.
- Los valores monetarios vienen con 2 decimales (GTQ).
