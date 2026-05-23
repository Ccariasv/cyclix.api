package com.cyclix.cyclix_api.device.websocket

import com.cyclix.cyclix_api.device.dto.DeviceSocketMessage
import com.cyclix.cyclix_api.device.service.DeviceWebSocketSessionRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class DeviceWebSocketHandler(
    private val deviceWebSocketSessionRegistry: DeviceWebSocketSessionRegistry,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {
    override fun afterConnectionEstablished(session: WebSocketSession) {
        val bikeId = session.attributes[DeviceWebSocketSessionRegistry.BIKE_ID_ATTRIBUTE] as? Long ?: return
        deviceWebSocketSessionRegistry.register(bikeId, session)

        val payload = DeviceSocketMessage(
            type = "CONNECTED",
            bikeId = bikeId,
            message = "Dispositivo conectado"
        )
        session.sendMessage(TextMessage(objectMapper.writeValueAsString(payload)))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        if (message.payload.equals("PING", ignoreCase = true)) {
            session.sendMessage(TextMessage("PONG"))
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
}
