package com.cyclix.cyclix_api.analytics.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class AnalyticsPeriod {
    DAY,
    WEEK,
    MONTH
}

data class AnalyticsFiltersResponse(
    val days: Int,
    val period: AnalyticsPeriod,
    val dateFrom: LocalDateTime,
    val dateTo: LocalDateTime,
    val stationId: Long? = null
)

data class AnalyticsTimelinePointResponse(
    val label: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val tripCount: Long
)

data class BikeAnalyticsSummaryResponse(
    val activeBikes: Long,
    val registeredTrips: Long,
    val averageTripDurationMinutes: Double,
    val averageDistanceKm: Double,
    val averageTripsPerBike: Double
)

data class BikeRankingItemResponse(
    val bikeId: Long,
    val code: String,
    val displayName: String,
    val tripCount: Long,
    val averageDurationMinutes: Double,
    val totalDistanceKm: Double
)

data class BikeAnalyticsResponse(
    val summary: BikeAnalyticsSummaryResponse,
    val ranking: List<BikeRankingItemResponse>,
    val timeline: List<AnalyticsTimelinePointResponse>,
    val filters: AnalyticsFiltersResponse
)

data class UserAnalyticsSummaryResponse(
    val activeUsers: Long,
    val registeredTrips: Long,
    val averageTripsPerUser: Double,
    val frequentUsers: Long
)

data class UserRankingItemResponse(
    val userId: Long,
    val fullName: String,
    val email: String,
    val tripCount: Long,
    val averageDurationMinutes: Double,
    val totalDistanceKm: Double
)

data class UserAnalyticsResponse(
    val summary: UserAnalyticsSummaryResponse,
    val ranking: List<UserRankingItemResponse>,
    val timeline: List<AnalyticsTimelinePointResponse>,
    val filters: AnalyticsFiltersResponse
)

data class StationAnalyticsSummaryResponse(
    val activeStations: Long,
    val registeredTrips: Long,
    val currentOccupancyPercentage: Double,
    val averageTripsPerStation: Double
)

data class StationRankingItemResponse(
    val stationId: Long,
    val name: String,
    val code: String,
    val tripCount: Long,
    val occupancyPercentage: Double,
    val availableSlots: Int,
    val totalCapacity: Int
)

data class StationAnalyticsResponse(
    val summary: StationAnalyticsSummaryResponse,
    val ranking: List<StationRankingItemResponse>,
    val timeline: List<AnalyticsTimelinePointResponse>,
    val filters: AnalyticsFiltersResponse,
    val selectedStation: StationRankingItemResponse? = null
)
