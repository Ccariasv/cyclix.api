package com.cyclix.cyclix_api.device.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LocationAuditPolicyServiceTest {
    private val service = LocationAuditPolicyService(
        activeMinDistanceMeters = 15.0,
        activeMaxSecondsBetweenPoints = 10,
        idleMinDistanceMeters = 30.0,
        idleMaxSecondsBetweenPoints = 60
    )

    @Test
    fun `shouldPersist accepts first point`() {
        val now = LocalDateTime.now()
        val result = service.shouldPersist(
            lastPersistedPoint = null,
            candidatePoint = LocationAuditPoint(
                bikeId = 7L,
                latitude = 14.9722,
                longitude = -89.5305,
                recordedAt = now
            ),
            hasActiveTrip = true
        )

        assertEquals(true, result)
    }

    @Test
    fun `shouldPersist skips short active update under time and distance thresholds`() {
        val now = LocalDateTime.now()
        val result = service.shouldPersist(
            lastPersistedPoint = LocationAuditPoint(
                bikeId = 7L,
                latitude = 14.9722,
                longitude = -89.5305,
                recordedAt = now
            ),
            candidatePoint = LocationAuditPoint(
                bikeId = 7L,
                latitude = 14.97225,
                longitude = -89.5305,
                recordedAt = now.plusSeconds(5)
            ),
            hasActiveTrip = true
        )

        assertEquals(false, result)
    }

    @Test
    fun `shouldPersist accepts active update when time threshold is exceeded`() {
        val now = LocalDateTime.now()
        val result = service.shouldPersist(
            lastPersistedPoint = LocationAuditPoint(
                bikeId = 7L,
                latitude = 14.9722,
                longitude = -89.5305,
                recordedAt = now
            ),
            candidatePoint = LocationAuditPoint(
                bikeId = 7L,
                latitude = 14.97225,
                longitude = -89.5305,
                recordedAt = now.plusSeconds(11)
            ),
            hasActiveTrip = true
        )

        assertEquals(true, result)
    }

    @Test
    fun `shouldPersist accepts idle update when distance threshold is exceeded`() {
        val now = LocalDateTime.now()
        val result = service.shouldPersist(
            lastPersistedPoint = LocationAuditPoint(
                bikeId = 7L,
                latitude = 14.9722,
                longitude = -89.5305,
                recordedAt = now
            ),
            candidatePoint = LocationAuditPoint(
                bikeId = 7L,
                latitude = 14.9726,
                longitude = -89.5305,
                recordedAt = now.plusSeconds(15)
            ),
            hasActiveTrip = false
        )

        assertEquals(true, result)
    }
}
