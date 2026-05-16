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
    val pricingRuleId: Long?,
    val pricingRuleName: String?,
    val subscriptionApplied: Boolean,
    val subscriptionMinutesCovered: Int?,
    val billableMinutes: Int?,
    val baseFareApplied: BigDecimal?,
    val includedMinutesApplied: Int?,
    val extraFarePerBlockApplied: BigDecimal?,
    val extraBlockMinutesApplied: Int?,
    val extraAmount: BigDecimal?,
    val totalAmount: BigDecimal?,
    val walletChargedAmount: BigDecimal?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
