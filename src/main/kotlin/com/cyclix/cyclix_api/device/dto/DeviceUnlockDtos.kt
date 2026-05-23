package com.cyclix.cyclix_api.device.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class DeviceUnlockRequest(
    @field:NotNull(message = "El ID del viaje es obligatorio")
    @field:Positive(message = "El ID del viaje debe ser positivo")
    val tripId: Long?
)

data class DeviceUnlockResponse(
    val authorized: Boolean,
    val bikeId: Long,
    val tripId: Long,
    val command: String,
    val authorizedAt: LocalDateTime,
    val message: String
)
