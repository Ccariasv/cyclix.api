package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.device.dto.DeviceSocketMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
open class DeviceWebSocketSessionRegistry(
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(DeviceWebSocketSessionRegistry::class.java)
    private val sessionsByBike = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    open fun register(bikeId: Long, session: WebSocketSession) {
        sessionsByBike.computeIfAbsent(bikeId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    open fun unregister(session: WebSocketSession) {
        val bikeId = session.attributes[BIKE_ID_ATTRIBUTE] as? Long ?: return
        sessionsByBike[bikeId]?.remove(session)
        if (sessionsByBike[bikeId].isNullOrEmpty()) {
            sessionsByBike.remove(bikeId)
        }
    }

    open fun sendToBike(bikeId: Long, payload: DeviceSocketMessage) {
        val sessions = sessionsByBike[bikeId].orEmpty().toList()
        if (sessions.isEmpty()) {
            log.info("No hay dispositivos websocket conectados para la bicicleta {}", bikeId)
            return
        }

        val message = TextMessage(objectMapper.writeValueAsString(payload))

        sessions.forEach { session ->
            try {
                if (session.isOpen) {
                    session.sendMessage(message)
                } else {
                    unregister(session)
                }
            } catch (ex: Exception) {
                log.warn("No se pudo enviar comando websocket a la bicicleta {}", bikeId, ex)
                unregister(session)
            }
        }
    }

    companion object {
        const val BIKE_ID_ATTRIBUTE = "bikeId"
    }
}
