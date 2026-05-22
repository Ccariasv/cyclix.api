package com.cyclix.cyclix_api.trip.dto

import com.cyclix.cyclix_api.trip.entity.TripStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class TripHistoryResponse(
    val id: Long,
    val bikeId: Long,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val status: TripStatus,
    val cost: BigDecimal?,
    val durationSeconds: Long?
)
