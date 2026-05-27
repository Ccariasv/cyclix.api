package com.cyclix.cyclix_api.device.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationAuditPoint(
    val bikeId: Long,
    val latitude: Double,
    val longitude: Double,
    val recordedAt: LocalDateTime
)

@Service
class LocationAuditPolicyService(
    @Value("\${app.device.location.audit.active.min-distance-meters:15.0}")
    private val activeMinDistanceMeters: Double,
    @Value("\${app.device.location.audit.active.max-seconds-between-points:10}")
    private val activeMaxSecondsBetweenPoints: Long,
    @Value("\${app.device.location.audit.idle.min-distance-meters:30.0}")
    private val idleMinDistanceMeters: Double,
    @Value("\${app.device.location.audit.idle.max-seconds-between-points:60}")
    private val idleMaxSecondsBetweenPoints: Long
) {
    fun shouldPersist(
        lastPersistedPoint: LocationAuditPoint?,
        candidatePoint: LocationAuditPoint,
        hasActiveTrip: Boolean
    ): Boolean {
        if (lastPersistedPoint == null) {
            return true
        }

        val distanceMeters = haversineMeters(
            latitude1 = lastPersistedPoint.latitude,
            longitude1 = lastPersistedPoint.longitude,
            latitude2 = candidatePoint.latitude,
            longitude2 = candidatePoint.longitude
        )

        val elapsedSeconds = Duration.between(lastPersistedPoint.recordedAt, candidatePoint.recordedAt)
            .seconds
            .coerceAtLeast(0)

        val minDistanceMeters = if (hasActiveTrip) activeMinDistanceMeters else idleMinDistanceMeters
        val maxSecondsBetweenPoints = if (hasActiveTrip) {
            activeMaxSecondsBetweenPoints
        } else {
            idleMaxSecondsBetweenPoints
        }

        return distanceMeters >= minDistanceMeters || elapsedSeconds >= maxSecondsBetweenPoints
    }

    private fun haversineMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val lat1Rad = Math.toRadians(latitude1)
        val lat2Rad = Math.toRadians(latitude2)
        val deltaLatRad = Math.toRadians(latitude2 - latitude1)
        val deltaLonRad = Math.toRadians(longitude2 - longitude1)

        val a = sin(deltaLatRad / 2).pow(2) +
            cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }
}
