package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.device.dto.DeviceSocketMessage
import com.cyclix.cyclix_api.trip.entity.Trip
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class DeviceCommandPublisher(
    private val deviceWebSocketSessionRegistry: DeviceWebSocketSessionRegistry
) {
    fun publishUnlockCommand(trip: Trip) {
        val message = DeviceSocketMessage(
            type = "UNLOCK",
            bikeId = trip.bikeId,
            tripId = trip.id,
            userId = trip.user.id,
            message = "Desbloqueo autorizado"
        )

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    deviceWebSocketSessionRegistry.sendToBike(trip.bikeId, message)
                }
            })
            return
        }

        deviceWebSocketSessionRegistry.sendToBike(trip.bikeId, message)
    }
}
