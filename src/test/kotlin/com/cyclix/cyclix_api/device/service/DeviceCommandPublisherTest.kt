package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.device.dto.DeviceSocketMessage
import com.cyclix.cyclix_api.puesto.model.Puesto
import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.cyclix.cyclix_api.trip.entity.Trip
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.user.Role
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

class DeviceCommandPublisherTest {
    @Test
    fun `publishUnlockCommand sends websocket message for station when available`() {
        val bicicletaRepository = org.mockito.Mockito.mock(BicicletaRepository::class.java)
        val registry = FakeRegistry()
        registry.stationAvailable = true
        val publisher = DeviceCommandPublisher(bicicletaRepository, registry)
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

        org.mockito.Mockito.`when`(bicicletaRepository.findById(7L)).thenReturn(
            Optional.of(
                Bicicleta(
                    id = 7L,
                    codigo = "BIC-7",
                    marca = "Trek",
                    modelo = "FX",
                    color = "Negro",
                    tipo = TipoBicicleta.URBANA,
                    tamanoLlanta = 29.0,
                    precioPorHora = BigDecimal("10.00"),
                    estado = EstadoBicicleta.DISPONIBLE,
                    codigoQr = "CYCLIX-BICI-BIC-7",
                    puesto = Puesto(
                        id = 3L,
                        nombre = "Centro",
                        codigo = "PST-003",
                        direccion = "Zona 1",
                        latitud = 14.9722,
                        longitud = -89.5305
                    )
                )
            )
        )

        publisher.publishUnlockCommand(trip)

        assertEquals(3L, registry.lastStationId)
        assertEquals("UNLOCK", registry.lastPayload?.type)
        assertEquals(7L, registry.lastPayload?.bikeId)
        assertEquals(15L, registry.lastPayload?.tripId)
        assertEquals(21L, registry.lastPayload?.userId)
    }

    private class FakeRegistry : DeviceWebSocketSessionRegistry(ObjectMapper()) {
        var stationAvailable: Boolean = false
        var lastBikeId: Long? = null
        var lastStationId: Long? = null
        var lastPayload: DeviceSocketMessage? = null

        override fun sendToBike(bikeId: Long, payload: DeviceSocketMessage) {
            lastBikeId = bikeId
            lastPayload = payload
        }

        override fun sendToStation(stationId: Long, payload: DeviceSocketMessage) {
            lastStationId = stationId
            lastPayload = payload
        }

        override fun hasStationSession(stationId: Long): Boolean = stationAvailable
    }
}
