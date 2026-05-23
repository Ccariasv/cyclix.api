package com.cyclix.cyclix_api.device.dto

import java.time.LocalDateTime

data class DeviceSocketMessage(
    val type: String,
    val bikeId: Long,
    val tripId: Long? = null,
    val userId: Long? = null,
    val message: String,
    val sentAt: LocalDateTime = LocalDateTime.now()
)
