package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.device.dto.DeviceUnlockRequest
import com.cyclix.cyclix_api.trip.entity.Trip
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.trip.repository.TripRepository
import com.cyclix.cyclix_api.user.Role
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class DeviceUnlockServiceTest {
    private lateinit var bicicletaRepository: BicicletaRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var deviceUnlockService: DeviceUnlockService

    @BeforeEach
    fun setup() {
        bicicletaRepository = mock(BicicletaRepository::class.java)
        tripRepository = mock(TripRepository::class.java)
        deviceUnlockService = DeviceUnlockService(
            bicicletaRepository = bicicletaRepository,
            tripRepository = tripRepository,
            deviceApiKey = "esp32-test-key"
        )
    }

    @Test
    fun `authorizeUnlock returns unlock command when bike and trip are active`() {
        val bike = buildBike(id = 7L, estado = EstadoBicicleta.EN_USO)
        val trip = buildTrip(id = 15L, bikeId = 7L, status = TripStatus.ACTIVE)

        `when`(bicicletaRepository.findById(7L)).thenReturn(Optional.of(bike))
        `when`(tripRepository.findByIdAndStatus(15L, TripStatus.ACTIVE)).thenReturn(Optional.of(trip))

        val response = deviceUnlockService.authorizeUnlock(
            bikeId = 7L,
            request = DeviceUnlockRequest(tripId = 15L),
            providedApiKey = "esp32-test-key"
        )

        assertEquals(true, response.authorized)
        assertEquals("UNLOCK", response.command)
        assertEquals(7L, response.bikeId)
        assertEquals(15L, response.tripId)
    }

    @Test
    fun `authorizeUnlock rejects invalid api key`() {
        val exception = assertThrows(ResponseStatusException::class.java) {
            deviceUnlockService.authorizeUnlock(
                bikeId = 7L,
                request = DeviceUnlockRequest(tripId = 15L),
                providedApiKey = "wrong-key"
            )
        }

        assertEquals(401, exception.statusCode.value())
    }

    @Test
    fun `authorizeUnlock rejects bike that is not in use`() {
        val bike = buildBike(id = 7L, estado = EstadoBicicleta.DISPONIBLE)
        `when`(bicicletaRepository.findById(7L)).thenReturn(Optional.of(bike))

        val exception = assertThrows(ResponseStatusException::class.java) {
            deviceUnlockService.authorizeUnlock(
                bikeId = 7L,
                request = DeviceUnlockRequest(tripId = 15L),
                providedApiKey = "esp32-test-key"
            )
        }

        assertEquals(409, exception.statusCode.value())
    }

    private fun buildBike(id: Long, estado: EstadoBicicleta) = Bicicleta(
        id = id,
        codigo = "BIC-$id",
        marca = "Trek",
        modelo = "FX",
        color = "Negro",
        tipo = TipoBicicleta.URBANA,
        tamanoLlanta = 29.0,
        precioPorHora = BigDecimal("10.00"),
        estado = estado,
        codigoQr = "CYCLIX-BICI-BIC-$id"
    )

    private fun buildTrip(id: Long, bikeId: Long, status: TripStatus) = Trip(
        id = id,
        user = buildUser(),
        bikeId = bikeId,
        status = status,
        startLatitude = BigDecimal("14.9722"),
        startLongitude = BigDecimal("-89.5305"),
        startedAt = LocalDateTime.now().minusMinutes(5)
    )

    private fun buildUser() = User(
        id = 1L,
        firstName = "Diego",
        lastName = "Carias",
        email = "diego@test.com",
        phone = "88880000",
        passwordHash = "hash",
        role = Role(id = 1L, name = "USER"),
        status = UserStatus(id = 1L, name = "ACTIVE"),
        emailVerified = true
    )
}
