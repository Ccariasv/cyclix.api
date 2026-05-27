# Documento de Integracion del Modulo de Bicicleta y Estacion

## 1. Objetivo

Este documento define el contrato tecnico entre el backend `cyclix-api` y el equipo que armara y programara:

- el modulo de bicicleta
- el modulo de estacion

El objetivo es que ambos dispositivos se integren por WebSocket con el backend para:

- reportar ubicacion de bicicleta en tiempo real
- recibir instrucciones de desbloqueo desde la estacion
- mantener trazabilidad suficiente sin saturar el servidor con escrituras innecesarias

## 2. Resumen funcional

### Bicicleta

La bicicleta debe:

- abrir un WebSocket autenticado hacia la API
- enviar su ubicacion GPS periodicamente con mensajes `LOCATION_UPDATE`
- incluir `recordedAt` cuando sea posible
- tolerar que el backend no responda a cada mensaje

### Estacion

La estacion debe:

- abrir un WebSocket autenticado hacia la API
- mantenerse escuchando instrucciones
- esperar el mensaje `UNLOCK`
- ejecutar el mecanismo fisico de desbloqueo

## 3. Endpoint WebSocket

Endpoint unico:

```text
ws://<host>:6060/ws/device
```

La autenticacion se hace con API key:

- por header: `X-Device-Api-Key`
- o por query param: `apiKey`

## 4. Conexion de bicicleta

La bicicleta debe conectarse asi:

```text
ws://<host>:6060/ws/device?clientType=BIKE&bikeId=<bikeId>&apiKey=<apiKey>
```

Parametros obligatorios:

- `clientType=BIKE`
- `bikeId`
- `apiKey` si no se manda en header

## 5. Conexion de estacion

La estacion debe conectarse asi:

```text
ws://<host>:6060/ws/device?clientType=STATION&stationId=<stationId>&apiKey=<apiKey>
```

Parametros obligatorios:

- `clientType=STATION`
- `stationId`
- `apiKey` si no se manda en header

## 6. Mensaje inicial del backend

Al conectarse correctamente, el backend responde con un mensaje `CONNECTED`.

Ejemplo para bicicleta:

```json
{
  "type": "CONNECTED",
  "bikeId": 7,
  "message": "Bicicleta conectada",
  "sentAt": "2026-05-27T12:00:00"
}
```

Ejemplo para estacion:

```json
{
  "type": "CONNECTED",
  "stationId": 3,
  "message": "Estacion conectada",
  "sentAt": "2026-05-27T12:00:00"
}
```

## 7. Heartbeat

El backend responde:

- si recibe `PING`
- responde `PONG`

Recomendacion:

- enviar `PING` cada `20` a `30` segundos si no hubo trafico
- si no hay respuesta o el socket cae, reconectar

## 8. Mensaje de ubicacion de bicicleta

La bicicleta envia mensajes tipo `LOCATION_UPDATE`.

Formato recomendado:

```json
{
  "type": "LOCATION_UPDATE",
  "latitude": 14.9722,
  "longitude": -89.5305,
  "recordedAt": "2026-05-27T12:15:30"
}
```

Campos:

- `type`: obligatorio, valor `LOCATION_UPDATE`
- `latitude`: obligatorio
- `longitude`: obligatorio
- `recordedAt`: recomendado; fecha/hora en que el modulo obtuvo la muestra

## 9. Regla importante sobre auditoria

La bicicleta puede enviar ubicacion con frecuencia alta, pero el backend **no guarda cada mensaje en base de datos**.

Actualmente la politica del backend es:

### Viaje activo

Se persiste un punto si:

- la bici se movio al menos `15 metros`, o
- han pasado al menos `10 segundos` desde el ultimo punto auditado

### Fuera de viaje

Se persiste un punto si:

- la bici se movio al menos `30 metros`, o
- han pasado al menos `60 segundos` desde el ultimo punto auditado

Esto significa:

- el modulo puede transmitir mas seguido
- el backend solo audita una parte de esos mensajes
- no debe asumirse respuesta por cada punto enviado

## 10. Confirmacion cuando un punto fue auditado

Si el backend decide guardar un punto, responde con:

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

Interpretacion:

- `LOCATION_AUDITED` confirma que ese punto si fue persistido
- si no llega respuesta, no significa error; puede significar que el backend recibio el punto pero no lo audito porque no cumplia los umbrales

## 11. Mensaje de error

Si el payload es invalido, el backend puede responder:

```json
{
  "type": "ERROR",
  "message": "Payload websocket invalido",
  "sentAt": "2026-05-27T12:15:31"
}
```

O por ejemplo:

```json
{
  "type": "ERROR",
  "bikeId": 7,
  "message": "La ubicacion requiere latitude y longitude",
  "sentAt": "2026-05-27T12:15:31"
}
```

## 12. Mensaje de desbloqueo para estacion

Cuando un usuario inicia un viaje y el backend lo autoriza, se envía a la estacion asociada un mensaje `UNLOCK`.

Formato:

```json
{
  "type": "UNLOCK",
  "bikeId": 7,
  "stationId": 3,
  "tripId": 15,
  "userId": 21,
  "message": "Desbloqueo autorizado",
  "sentAt": "2026-05-27T12:20:00"
}
```

Comportamiento esperado de la estacion:

- validar que el mensaje sea `UNLOCK`
- ubicar el actuador correspondiente
- ejecutar el desbloqueo fisico
- registrar localmente el evento para diagnostico

## 13. Fallback de desbloqueo

El backend intenta enviar `UNLOCK` a la estacion conectada.

Si no hay estacion conectada:

- el backend hace fallback al socket de la bicicleta

Por eso:

- la bicicleta debe tolerar recibir `UNLOCK`
- la estacion sigue siendo el receptor principal

## 14. Frecuencia recomendada para GPS

Recomendacion para firmware:

- tomar muestra GPS cada `3` a `5` segundos
- transmitir por WebSocket cada muestra valida
- incluir `recordedAt`

No es necesario replicar en firmware la misma logica de auditoria del backend. El filtrado principal ya lo hace la API.

## 15. Reconexion

Recomendaciones:

1. Si el socket se cierra, reconectar automaticamente.
2. Usar backoff progresivo:
   - intento 1: `1s`
   - intento 2: `2s`
   - intento 3: `5s`
   - luego mantener entre `10s` y `30s`
3. Reenviar la identificacion completa en cada nueva conexion.
4. Si el GPS sigue disponible durante desconexion, el modulo puede seguir acumulando el ultimo punto localmente, pero no debe inundar la red reenviando backlog completo sin estrategia.

## 16. Seguridad

- La `apiKey` de dispositivos debe manejarse como secreto.
- No debe quedar hardcodeada en repositorios publicos.
- Si se usa firmware con consola serie, evitar imprimir la llave completa en logs.

## 17. Recomendaciones de firmware

### Bicicleta

- mantener reloj relativamente sincronizado si se usa `recordedAt`
- validar que GPS tenga fix antes de mandar
- no bloquear el loop principal esperando respuesta del backend
- separar:
  - lectura de GPS
  - envio WebSocket
  - control de actuadores o sensores

### Estacion

- mantener el WebSocket abierto de forma persistente
- no reiniciar el socket por cada operacion
- aislar la logica de actuador del hilo o loop de comunicaciones
- registrar localmente resultado de desbloqueo y fallas electricas/mecanicas

## 18. Contrato actual del backend

El backend hoy soporta:

- `CONNECTED`
- `PING` / `PONG`
- `LOCATION_UPDATE`
- `LOCATION_AUDITED`
- `UNLOCK`
- `ERROR`

No existe todavia un mensaje formal de respuesta desde estacion hacia backend tipo:

- `UNLOCK_ACK`
- `UNLOCK_FAILED`

Eso queda como siguiente etapa recomendada.

## 19. Pruebas minimas para el equipo de modulo

### Bicicleta

1. Conectar con `clientType=BIKE`.
2. Verificar recepcion de `CONNECTED`.
3. Enviar `PING` y verificar `PONG`.
4. Enviar `LOCATION_UPDATE` valido.
5. Verificar que algunos puntos reciban `LOCATION_AUDITED`.
6. Verificar reconexion automatica si el servidor se cae.

### Estacion

1. Conectar con `clientType=STATION`.
2. Verificar recepcion de `CONNECTED`.
3. Mantener el socket estable por tiempo prolongado.
4. Simular inicio de viaje desde backend y verificar recepcion de `UNLOCK`.
5. Verificar que el actuador responda sin colgar la comunicacion.

## 20. Resumen operativo

- La bicicleta transmite ubicacion constantemente por WebSocket.
- El backend recibe todos los mensajes.
- Solo algunos puntos se auditan y guardan en base de datos.
- La estacion queda conectada esperando `UNLOCK`.
- Si la estacion no esta conectada, el backend puede mandar `UNLOCK` a la bicicleta como fallback.

