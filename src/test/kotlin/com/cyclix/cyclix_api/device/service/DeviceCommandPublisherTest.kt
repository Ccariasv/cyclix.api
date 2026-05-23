package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.device.dto.DeviceSocketMessage
import com.fasterxml.jackson.databind.ObjectMapper
import com.cyclix.cyclix_api.trip.entity.Trip
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.user.Role
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DeviceCommandPublisherTest {
    @Test
    fun `publishUnlockCommand sends websocket message for bike`() {
        val registry = FakeRegistry()
        val publisher = DeviceCommandPublisher(registry)
        val trip = Trip(
            id = 15L,
            user = User(
                id = 21L,
                firstName = "Diego",
                email = "diego@test.com",
                passwordHash = "hash",
                role = Role(id = 1L, name = "USER"),
                status = UserStatus(id = 1L, name = "ACTIVE")
            ),
            bikeId = 7L,
            status = TripStatus.ACTIVE,
            startLatitude = BigDecimal("14.9722"),
            startLongitude = BigDecimal("-89.5305")
        )

        publisher.publishUnlockCommand(trip)

        assertEquals(7L, registry.lastBikeId)
        assertEquals("UNLOCK", registry.lastPayload?.type)
        assertEquals(15L, registry.lastPayload?.tripId)
        assertEquals(21L, registry.lastPayload?.userId)
    }

    private class FakeRegistry : DeviceWebSocketSessionRegistry(ObjectMapper()) {
        var lastBikeId: Long? = null
        var lastPayload: DeviceSocketMessage? = null

        override fun sendToBike(bikeId: Long, payload: DeviceSocketMessage) {
            lastBikeId = bikeId
            lastPayload = payload
        }
    }
}
