package com.cyclix.cyclix_api.analytics.service

import com.cyclix.cyclix_api.analytics.dto.AnalyticsFiltersResponse
import com.cyclix.cyclix_api.analytics.dto.AnalyticsPeriod
import com.cyclix.cyclix_api.analytics.dto.AnalyticsTimelinePointResponse
import com.cyclix.cyclix_api.analytics.dto.BikeAnalyticsResponse
import com.cyclix.cyclix_api.analytics.dto.BikeAnalyticsSummaryResponse
import com.cyclix.cyclix_api.analytics.dto.BikeRankingItemResponse
import com.cyclix.cyclix_api.analytics.dto.StationAnalyticsResponse
import com.cyclix.cyclix_api.analytics.dto.StationAnalyticsSummaryResponse
import com.cyclix.cyclix_api.analytics.dto.StationRankingItemResponse
import com.cyclix.cyclix_api.analytics.dto.UserAnalyticsResponse
import com.cyclix.cyclix_api.analytics.dto.UserAnalyticsSummaryResponse
import com.cyclix.cyclix_api.analytics.dto.UserRankingItemResponse
import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.puesto.model.EstadoPuesto
import com.cyclix.cyclix_api.puesto.model.Puesto
import com.cyclix.cyclix_api.puesto.repository.PuestoRepository
import com.cyclix.cyclix_api.trip.entity.Trip
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.trip.repository.TripRepository
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class AdminAnalyticsService(
    private val tripRepository: TripRepository,
    private val bicicletaRepository: BicicletaRepository,
    private val userRepository: UserRepository,
    private val puestoRepository: PuestoRepository
) {
    private val frequentUserTripThreshold = 3L

    @Transactional(readOnly = true)
    fun getBikeAnalytics(days: Int, period: AnalyticsPeriod): BikeAnalyticsResponse {
        val window = resolveWindow(days, period)
        val trips = findTripsInWindow(window)
        val completedTrips = trips.filter { it.status == TripStatus.COMPLETED }
        val bikesById = bicicletaRepository.findAll().associateBy { it.id }
        val activeBikes = bikesById.values.count { it.estado != EstadoBicicleta.FUERA_DE_SERVICIO }.toLong()

        val ranking = trips.groupBy { it.bikeId }
            .map { (bikeId, bikeTrips) ->
                val bike = bikesById[bikeId]
                BikeRankingItemResponse(
                    bikeId = bikeId,
                    code = bike?.codigo ?: "BICI-$bikeId",
                    displayName = bike?.let { "${it.marca} ${it.modelo}" } ?: "Bicicleta $bikeId",
                    tripCount = bikeTrips.size.toLong(),
                    averageDurationMinutes = averageDurationMinutes(bikeTrips),
                    totalDistanceKm = totalDistanceKm(bikeTrips)
                )
            }
            .sortedByDescending { it.tripCount }
            .take(5)

        return BikeAnalyticsResponse(
            summary = BikeAnalyticsSummaryResponse(
                activeBikes = activeBikes,
                registeredTrips = trips.size.toLong(),
                averageTripDurationMinutes = averageDurationMinutes(completedTrips),
                averageDistanceKm = averageDistanceKm(completedTrips),
                averageTripsPerBike = safeAverage(trips.size.toDouble(), activeBikes.toDouble())
            ),
            ranking = ranking,
            timeline = buildTimeline(trips, period),
            filters = window.toFiltersResponse()
        )
    }

    @Transactional(readOnly = true)
    fun getUserAnalytics(days: Int, period: AnalyticsPeriod): UserAnalyticsResponse {
        val window = resolveWindow(days, period)
        val trips = findTripsInWindow(window)
        val completedTrips = trips.filter { it.status == TripStatus.COMPLETED }
        val users = userRepository.findAll()
        val activeUsers = users.count { it.status.name.equals("ACTIVE", ignoreCase = true) }.toLong()

        val groupedTrips = trips.groupBy { it.user.id }
        val ranking = groupedTrips.mapNotNull { (userId, userTrips) ->
            val user = userTrips.firstOrNull()?.user ?: users.find { it.id == userId }
            user?.let {
                UserRankingItemResponse(
                    userId = userId,
                    fullName = fullNameOf(it),
                    email = it.email,
                    tripCount = userTrips.size.toLong(),
                    averageDurationMinutes = averageDurationMinutes(userTrips),
                    totalDistanceKm = totalDistanceKm(userTrips)
                )
            }
        }
            .sortedByDescending { it.tripCount }
            .take(5)

        return UserAnalyticsResponse(
            summary = UserAnalyticsSummaryResponse(
                activeUsers = activeUsers,
                registeredTrips = trips.size.toLong(),
                averageTripsPerUser = safeAverage(trips.size.toDouble(), activeUsers.toDouble()),
                frequentUsers = groupedTrips.count { (_, userTrips) -> userTrips.size >= frequentUserTripThreshold }.toLong()
            ),
            ranking = ranking,
            timeline = buildTimeline(trips, period),
            filters = window.toFiltersResponse()
        )
    }

    @Transactional(readOnly = true)
    fun getStationAnalytics(days: Int, period: AnalyticsPeriod, stationId: Long?): StationAnalyticsResponse {
        val window = resolveWindow(days, period, stationId)
        val allStations = puestoRepository.findAll()
        val stationById = allStations.associateBy { it.id }
        val selectedStation = stationId?.let {
            stationById[it] ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Estacion no encontrada: $it")
        }

        val trips = findTripsInWindow(window)
        val tripsWithStation = trips.mapNotNull { trip ->
            inferStationId(trip, allStations)?.let { inferredStationId -> inferredStationId to trip }
        }
        val filteredTrips = selectedStation?.let { station ->
            tripsWithStation.filter { (resolvedStationId, _) -> resolvedStationId == station.id }
        } ?: tripsWithStation

        val targetStations = selectedStation?.let { listOf(it) } ?: allStations
        val activeStations = targetStations.count { it.estado == EstadoPuesto.ACTIVO }.toLong()
        val ranking = (selectedStation?.let { listOf(it.id) } ?: allStations.map { it.id })
            .mapNotNull { id ->
                stationById[id]?.let { station ->
                    station.toRankingItem(
                        tripCount = filteredTrips.count { (resolvedStationId, _) -> resolvedStationId == station.id }.toLong()
                    )
                }
            }
            .sortedByDescending { it.tripCount }
            .take(5)

        val occupancyPercentage = occupancyPercentage(targetStations)

        return StationAnalyticsResponse(
            summary = StationAnalyticsSummaryResponse(
                activeStations = activeStations,
                registeredTrips = filteredTrips.size.toLong(),
                currentOccupancyPercentage = occupancyPercentage,
                averageTripsPerStation = safeAverage(filteredTrips.size.toDouble(), activeStations.toDouble())
            ),
            ranking = ranking,
            timeline = buildTimeline(filteredTrips.map { it.second }, period),
            filters = window.toFiltersResponse(),
            selectedStation = selectedStation?.toRankingItem(
                tripCount = filteredTrips.size.toLong()
            )
        )
    }

    private fun resolveWindow(days: Int, period: AnalyticsPeriod, stationId: Long? = null): AnalyticsWindow {
        if (days !in 1..365) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "days debe estar entre 1 y 365")
        }

        val dateTo = LocalDateTime.now()
        val dateFrom = dateTo.minusDays(days.toLong())
        return AnalyticsWindow(days, period, dateFrom, dateTo, stationId)
    }

    private fun findTripsInWindow(window: AnalyticsWindow): List<Trip> =
        tripRepository.findAllByOrderByStartedAtDesc()
            .filter { it.startedAt >= window.dateFrom && it.startedAt <= window.dateTo }

    private fun buildTimeline(trips: List<Trip>, period: AnalyticsPeriod): List<AnalyticsTimelinePointResponse> {
        val grouped = trips.groupBy { startOfPeriod(it.startedAt.toLocalDate(), period) }

        return grouped.entries
            .sortedBy { it.key }
            .map { (periodStart, groupedTrips) ->
                val periodEnd = endOfPeriod(periodStart, period)
                AnalyticsTimelinePointResponse(
                    label = timelineLabel(periodStart, period),
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    tripCount = groupedTrips.size.toLong()
                )
            }
    }

    private fun startOfPeriod(date: LocalDate, period: AnalyticsPeriod): LocalDate =
        when (period) {
            AnalyticsPeriod.DAY -> date
            AnalyticsPeriod.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            AnalyticsPeriod.MONTH -> date.withDayOfMonth(1)
        }

    private fun endOfPeriod(periodStart: LocalDate, period: AnalyticsPeriod): LocalDate =
        when (period) {
            AnalyticsPeriod.DAY -> periodStart
            AnalyticsPeriod.WEEK -> periodStart.plusDays(6)
            AnalyticsPeriod.MONTH -> periodStart.withDayOfMonth(periodStart.lengthOfMonth())
        }

    private fun timelineLabel(periodStart: LocalDate, period: AnalyticsPeriod): String =
        when (period) {
            AnalyticsPeriod.DAY -> periodStart.toString()
            AnalyticsPeriod.WEEK -> "Semana ${periodStart}"
            AnalyticsPeriod.MONTH -> "${periodStart.year}-${periodStart.monthValue.toString().padStart(2, '0')}"
        }

    private fun averageDurationMinutes(trips: List<Trip>): Double {
        val durations = trips.mapNotNull { it.durationSeconds }
        if (durations.isEmpty()) return 0.0
        return roundTwoDecimals(durations.average() / 60.0)
    }

    private fun averageDistanceKm(trips: List<Trip>): Double {
        val distances = trips.mapNotNull { it.distanceKm?.toDouble() }
        if (distances.isEmpty()) return 0.0
        return roundTwoDecimals(distances.average())
    }

    private fun totalDistanceKm(trips: List<Trip>): Double =
        roundTwoDecimals(trips.mapNotNull { it.distanceKm?.toDouble() }.sum())

    private fun safeAverage(total: Double, divisor: Double): Double =
        if (divisor <= 0.0) 0.0 else roundTwoDecimals(total / divisor)

    private fun occupancyPercentage(stations: List<Puesto>): Double {
        val totalCapacity = stations.sumOf { it.capacidadTotal }
        if (totalCapacity <= 0) return 0.0
        val occupiedSlots = stations.sumOf { it.capacidadTotal - it.capacidadDisponible }
        return roundTwoDecimals((occupiedSlots.toDouble() / totalCapacity.toDouble()) * 100.0)
    }

    private fun inferStationId(trip: Trip, stations: List<Puesto>): Long? {
        if (stations.isEmpty()) return null

        val startLat = trip.startLatitude.toDouble()
        val startLon = trip.startLongitude.toDouble()

        return stations.minByOrNull { haversineKm(startLat, startLon, it.latitud, it.longitud) }?.id
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun fullNameOf(user: User): String =
        listOfNotNull(user.firstName.trim(), user.lastName?.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { user.email }

    private fun Puesto.toRankingItem(tripCount: Long): StationRankingItemResponse =
        StationRankingItemResponse(
            stationId = id,
            name = nombre,
            code = codigo,
            tripCount = tripCount,
            occupancyPercentage = if (capacidadTotal == 0) 0.0 else roundTwoDecimals(
                ((capacidadTotal - capacidadDisponible).toDouble() / capacidadTotal.toDouble()) * 100.0
            ),
            availableSlots = capacidadDisponible,
            totalCapacity = capacidadTotal
        )

    private fun roundTwoDecimals(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    private fun AnalyticsWindow.toFiltersResponse(): AnalyticsFiltersResponse =
        AnalyticsFiltersResponse(
            days = days,
            period = period,
            dateFrom = dateFrom,
            dateTo = dateTo,
            stationId = stationId
        )

    private data class AnalyticsWindow(
        val days: Int,
        val period: AnalyticsPeriod,
        val dateFrom: LocalDateTime,
        val dateTo: LocalDateTime,
        val stationId: Long? = null
    )
}
