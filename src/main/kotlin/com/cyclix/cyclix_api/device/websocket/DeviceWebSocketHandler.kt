package com.cyclix.cyclix_api.device.websocket

import com.cyclix.cyclix_api.device.dto.DeviceSocketMessage
import com.cyclix.cyclix_api.device.service.DeviceLocationAuditService
import com.cyclix.cyclix_api.device.service.DeviceWebSocketSessionRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class DeviceWebSocketHandler(
    private val deviceWebSocketSessionRegistry: DeviceWebSocketSessionRegistry,
    private val deviceLocationAuditService: DeviceLocationAuditService,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(DeviceWebSocketHandler::class.java)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val clientType = session.attributes[DeviceWebSocketSessionRegistry.CLIENT_TYPE_ATTRIBUTE] as? String ?: return
        val payload = when (clientType) {
            DeviceWebSocketSessionRegistry.CLIENT_TYPE_BIKE -> {
                val bikeId = session.attributes[DeviceWebSocketSessionRegistry.BIKE_ID_ATTRIBUTE] as? Long ?: return
                deviceWebSocketSessionRegistry.registerBike(bikeId, session)
                DeviceSocketMessage(
                    type = "CONNECTED",
                    bikeId = bikeId,
                    message = "Bicicleta conectada"
                )
            }
            DeviceWebSocketSessionRegistry.CLIENT_TYPE_STATION -> {
                val stationId = session.attributes[DeviceWebSocketSessionRegistry.STATION_ID_ATTRIBUTE] as? Long ?: return
                deviceWebSocketSessionRegistry.registerStation(stationId, session)
                DeviceSocketMessage(
                    type = "CONNECTED",
                    stationId = stationId,
                    message = "Estacion conectada"
                )
            }
            else -> return
        }

        session.sendMessage(TextMessage(objectMapper.writeValueAsString(payload)))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        if (message.payload.equals("PING", ignoreCase = true)) {
            session.sendMessage(TextMessage("PONG"))
            return
        }

        val clientType = session.attributes[DeviceWebSocketSessionRegistry.CLIENT_TYPE_ATTRIBUTE] as? String ?: return
        val payload = try {
            objectMapper.readValue(message.payload, DeviceSocketMessage::class.java)
        } catch (ex: Exception) {
            log.warn("Mensaje websocket inválido recibido", ex)
            session.sendMessage(
                TextMessage(
                    objectMapper.writeValueAsString(
                        DeviceSocketMessage(
                            type = "ERROR",
                            message = "Payload websocket inválido"
                        )
                    )
                )
            )
            return
        }

        if (clientType == DeviceWebSocketSessionRegistry.CLIENT_TYPE_BIKE && payload.type.equals("LOCATION_UPDATE", ignoreCase = true)) {
            handleBikeLocationUpdate(session, payload)
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        deviceWebSocketSessionRegistry.unregister(session)
        if (session.isOpen) {
            session.close(CloseStatus.SERVER_ERROR)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        deviceWebSocketSessionRegistry.unregister(session)
    }

    override fun supportsPartialMessages(): Boolean = false

    private fun handleBikeLocationUpdate(session: WebSocketSession, payload: DeviceSocketMessage) {
        val bikeId = session.attributes[DeviceWebSocketSessionRegistry.BIKE_ID_ATTRIBUTE] as? Long ?: return
        val latitude = payload.latitude
        val longitude = payload.longitude

        if (latitude == null || longitude == null) {
            session.sendMessage(
                TextMessage(
                    objectMapper.writeValueAsString(
                        DeviceSocketMessage(
                            type = "ERROR",
                            bikeId = bikeId,
                            message = "La ubicacion requiere latitude y longitude"
                        )
                    )
                )
            )
            return
        }

        val result = deviceLocationAuditService.auditLocationUpdate(
            bikeId = bikeId,
            latitude = latitude,
            longitude = longitude,
            recordedAt = payload.recordedAt ?: payload.sentAt
        )

        if (!result.persisted) {
            return
        }

        session.sendMessage(
            TextMessage(
                objectMapper.writeValueAsString(
                    DeviceSocketMessage(
                        type = "LOCATION_AUDITED",
                        bikeId = bikeId,
                        stationId = result.stationId,
                        tripId = result.tripId,
                        latitude = latitude,
                        longitude = longitude,
                        recordedAt = result.recordedAt,
                        message = "Ubicacion auditada"
                    )
                )
            )
        )
    }
}
