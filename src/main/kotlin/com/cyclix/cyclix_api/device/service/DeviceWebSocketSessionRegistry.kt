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
    private val bikeSessionsByBikeId = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()
    private val stationSessionsByStationId = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    open fun registerBike(bikeId: Long, session: WebSocketSession) {
        bikeSessionsByBikeId.computeIfAbsent(bikeId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    open fun registerStation(stationId: Long, session: WebSocketSession) {
        stationSessionsByStationId.computeIfAbsent(stationId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    open fun unregister(session: WebSocketSession) {
        when (session.attributes[CLIENT_TYPE_ATTRIBUTE]) {
            CLIENT_TYPE_BIKE -> removeSession(
                sessions = bikeSessionsByBikeId,
                id = session.attributes[BIKE_ID_ATTRIBUTE] as? Long,
                session = session
            )
            CLIENT_TYPE_STATION -> removeSession(
                sessions = stationSessionsByStationId,
                id = session.attributes[STATION_ID_ATTRIBUTE] as? Long,
                session = session
            )
        }
    }

    open fun sendToBike(bikeId: Long, payload: DeviceSocketMessage) {
        send(
            sessions = bikeSessionsByBikeId[bikeId].orEmpty().toList(),
            payload = payload,
            emptyLogMessage = "No hay dispositivos websocket conectados para la bicicleta {}",
            targetId = bikeId,
            errorLogMessage = "No se pudo enviar mensaje websocket a la bicicleta {}"
        )
    }

    open fun sendToStation(stationId: Long, payload: DeviceSocketMessage) {
        send(
            sessions = stationSessionsByStationId[stationId].orEmpty().toList(),
            payload = payload,
            emptyLogMessage = "No hay estaciones websocket conectadas para el puesto {}",
            targetId = stationId,
            errorLogMessage = "No se pudo enviar mensaje websocket al puesto {}"
        )
    }

    open fun hasStationSession(stationId: Long): Boolean =
        stationSessionsByStationId[stationId].orEmpty().any { it.isOpen }

    private fun removeSession(
        sessions: ConcurrentHashMap<Long, MutableSet<WebSocketSession>>,
        id: Long?,
        session: WebSocketSession
    ) {
        if (id == null) {
            return
        }
        sessions[id]?.remove(session)
        if (sessions[id].isNullOrEmpty()) {
            sessions.remove(id)
        }
    }

    private fun send(
        sessions: List<WebSocketSession>,
        payload: DeviceSocketMessage,
        emptyLogMessage: String,
        targetId: Long,
        errorLogMessage: String
    ) {
        if (sessions.isEmpty()) {
            log.info(emptyLogMessage, targetId)
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
                log.warn(errorLogMessage, targetId, ex)
                unregister(session)
            }
        }
    }

    companion object {
        const val CLIENT_TYPE_ATTRIBUTE = "clientType"
        const val CLIENT_TYPE_BIKE = "BIKE"
        const val CLIENT_TYPE_STATION = "STATION"
        const val BIKE_ID_ATTRIBUTE = "bikeId"
        const val STATION_ID_ATTRIBUTE = "stationId"
    }
}
