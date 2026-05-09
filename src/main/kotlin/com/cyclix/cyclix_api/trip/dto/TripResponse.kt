package com.cyclix.cyclix_api.trip.dto

import com.cyclix.cyclix_api.trip.entity.TripStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class TripResponse(
    val id: Long,
    val userId: Long,
    val bikeId: Long,
    val status: TripStatus,
    val startLatitude: BigDecimal,
    val startLongitude: BigDecimal,
    val endLatitude: BigDecimal?,
    val endLongitude: BigDecimal?,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val distanceKm: BigDecimal?,
    val durationSeconds: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)