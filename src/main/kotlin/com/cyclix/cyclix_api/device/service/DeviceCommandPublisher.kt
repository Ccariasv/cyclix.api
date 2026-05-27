package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.device.dto.DeviceSocketMessage
import com.cyclix.cyclix_api.trip.entity.Trip
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class DeviceCommandPublisher(
    private val bicicletaRepository: BicicletaRepository,
    private val deviceWebSocketSessionRegistry: DeviceWebSocketSessionRegistry
) {
    fun publishUnlockCommand(trip: Trip) {
        val stationId = bicicletaRepository.findById(trip.bikeId)
            .orElse(null)
            ?.puesto
            ?.id

        val message = DeviceSocketMessage(
            type = "UNLOCK",
            bikeId = trip.bikeId,
            stationId = stationId,
            tripId = trip.id,
            userId = trip.user.id,
            message = "Desbloqueo autorizado"
        )

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    publish(trip.bikeId, stationId, message)
                }
            })
            return
        }

        publish(trip.bikeId, stationId, message)
    }

    private fun publish(bikeId: Long, stationId: Long?, message: DeviceSocketMessage) {
        if (stationId != null && deviceWebSocketSessionRegistry.hasStationSession(stationId)) {
            deviceWebSocketSessionRegistry.sendToStation(stationId, message)
            return
        }

        deviceWebSocketSessionRegistry.sendToBike(bikeId, message)
    }
}
