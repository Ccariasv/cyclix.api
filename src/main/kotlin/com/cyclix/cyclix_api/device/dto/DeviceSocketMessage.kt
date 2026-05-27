package com.cyclix.cyclix_api.device.dto

import java.time.LocalDateTime

data class DeviceSocketMessage(
    val type: String,
    val bikeId: Long? = null,
    val stationId: Long? = null,
    val tripId: Long? = null,
    val userId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val message: String? = null,
    val recordedAt: LocalDateTime? = null,
    val sentAt: LocalDateTime = LocalDateTime.now()
)
