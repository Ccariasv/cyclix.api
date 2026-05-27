package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.device.entity.BicycleLocationHistory
import com.cyclix.cyclix_api.device.repository.BicycleLocationHistoryRepository
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.trip.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

data class DeviceLocationAuditResult(
    val persisted: Boolean,
    val bikeId: Long,
    val tripId: Long? = null,
    val stationId: Long? = null,
    val recordedAt: LocalDateTime
)

@Service
class DeviceLocationAuditService(
    private val bicycleLocationHistoryRepository: BicycleLocationHistoryRepository,
    private val deviceLocationService: DeviceLocationService,
    private val locationAuditPolicyService: LocationAuditPolicyService,
    private val tripRepository: TripRepository
) {
    private val lastAcceptedPoints = ConcurrentHashMap<Long, LocationAuditPoint>()

    @Transactional
    fun auditLocationUpdate(
        bikeId: Long,
        latitude: Double,
        longitude: Double,
        recordedAt: LocalDateTime = LocalDateTime.now()
    ): DeviceLocationAuditResult {
        val activeTrip = tripRepository.findFirstByBikeIdAndStatusOrderByStartedAtDesc(bikeId, TripStatus.ACTIVE)
            .orElse(null)

        val candidatePoint = LocationAuditPoint(
            bikeId = bikeId,
            latitude = latitude,
            longitude = longitude,
            recordedAt = recordedAt
        )

        val lastAcceptedPoint = resolveLastAcceptedPoint(bikeId)
        val shouldPersist = locationAuditPolicyService.shouldPersist(
            lastPersistedPoint = lastAcceptedPoint,
            candidatePoint = candidatePoint,
            hasActiveTrip = activeTrip != null
        )

        if (!shouldPersist) {
            return DeviceLocationAuditResult(
                persisted = false,
                bikeId = bikeId,
                tripId = activeTrip?.id,
                recordedAt = recordedAt
            )
        }

        val receivedAt = LocalDateTime.now()
        val updatedBike = deviceLocationService.updateBikeLocation(
            bikeId = bikeId,
            latitude = latitude,
            longitude = longitude,
            updatedAt = receivedAt
        )

        bicycleLocationHistoryRepository.save(
            BicycleLocationHistory(
                bikeId = bikeId,
                tripId = activeTrip?.id,
                stationId = updatedBike.puesto?.id,
                eventType = EVENT_TYPE_LOCATION_UPDATE,
                source = SOURCE_BIKE_WEBSOCKET,
                latitude = BigDecimal.valueOf(latitude),
                longitude = BigDecimal.valueOf(longitude),
                recordedAt = recordedAt,
                receivedAt = receivedAt
            )
        )

        lastAcceptedPoints[bikeId] = candidatePoint

        return DeviceLocationAuditResult(
            persisted = true,
            bikeId = bikeId,
            tripId = activeTrip?.id,
            stationId = updatedBike.puesto?.id,
            recordedAt = recordedAt
        )
    }

    private fun resolveLastAcceptedPoint(bikeId: Long): LocationAuditPoint? {
        lastAcceptedPoints[bikeId]?.let { return it }

        val latestPoint = bicycleLocationHistoryRepository.findFirstByBikeIdOrderByRecordedAtDesc(bikeId)
            ?.toAuditPoint()

        if (latestPoint != null) {
            lastAcceptedPoints[bikeId] = latestPoint
        }

        return latestPoint
    }

    private fun BicycleLocationHistory.toAuditPoint(): LocationAuditPoint =
        LocationAuditPoint(
            bikeId = bikeId,
            latitude = latitude.toDouble(),
            longitude = longitude.toDouble(),
            recordedAt = recordedAt
        )

    companion object {
        const val EVENT_TYPE_LOCATION_UPDATE = "LOCATION_UPDATE"
        const val SOURCE_BIKE_WEBSOCKET = "BIKE_WEBSOCKET"
    }
}
