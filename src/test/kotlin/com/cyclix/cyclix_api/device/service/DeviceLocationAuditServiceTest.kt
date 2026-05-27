package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.device.entity.BicycleLocationHistory
import com.cyclix.cyclix_api.device.repository.BicycleLocationHistoryRepository
import com.cyclix.cyclix_api.puesto.model.Puesto
import com.cyclix.cyclix_api.trip.entity.Trip
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.trip.repository.TripRepository
import com.cyclix.cyclix_api.user.Role
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class DeviceLocationAuditServiceTest {
    private lateinit var bicicletaRepository: BicicletaRepository
    private lateinit var bicycleLocationHistoryRepository: BicycleLocationHistoryRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var deviceLocationAuditService: DeviceLocationAuditService

    @BeforeEach
    fun setup() {
        bicicletaRepository = mock(BicicletaRepository::class.java)
        bicycleLocationHistoryRepository = mock(BicycleLocationHistoryRepository::class.java)
        tripRepository = mock(TripRepository::class.java)

        val deviceLocationService = DeviceLocationService(bicicletaRepository)
        val locationAuditPolicyService = LocationAuditPolicyService(
            activeMinDistanceMeters = 15.0,
            activeMaxSecondsBetweenPoints = 10,
            idleMinDistanceMeters = 30.0,
            idleMaxSecondsBetweenPoints = 60
        )

        deviceLocationAuditService = DeviceLocationAuditService(
            bicycleLocationHistoryRepository = bicycleLocationHistoryRepository,
            deviceLocationService = deviceLocationService,
            locationAuditPolicyService = locationAuditPolicyService,
            tripRepository = tripRepository
        )
    }

    @Test
    fun `auditLocationUpdate persists first point and updates bike current location`() {
        val bike = buildBike(id = 7L)
        val trip = buildTrip(id = 15L, bikeId = 7L)
        val recordedAt = LocalDateTime.now()

        `when`(tripRepository.findFirstByBikeIdAndStatusOrderByStartedAtDesc(7L, TripStatus.ACTIVE))
            .thenReturn(Optional.of(trip))
        `when`(bicycleLocationHistoryRepository.findFirstByBikeIdOrderByRecordedAtDesc(7L))
            .thenReturn(null)
        `when`(bicicletaRepository.findById(7L)).thenReturn(Optional.of(bike))
        `when`(bicicletaRepository.save(any(Bicicleta::class.java)))
            .thenAnswer { it.arguments[0] as Bicicleta }
        `when`(bicycleLocationHistoryRepository.save(any(BicycleLocationHistory::class.java)))
            .thenAnswer { it.arguments[0] as BicycleLocationHistory }

        val result = deviceLocationAuditService.auditLocationUpdate(
            bikeId = 7L,
            latitude = 14.9722,
            longitude = -89.5305,
            recordedAt = recordedAt
        )

        assertEquals(true, result.persisted)
        assertEquals(15L, result.tripId)
        assertEquals(3L, result.stationId)

        val historyCaptor = ArgumentCaptor.forClass(BicycleLocationHistory::class.java)
        verify(bicycleLocationHistoryRepository).save(historyCaptor.capture())
        assertEquals(7L, historyCaptor.value.bikeId)
        assertEquals(15L, historyCaptor.value.tripId)
        assertEquals(3L, historyCaptor.value.stationId)
        assertEquals("LOCATION_UPDATE", historyCaptor.value.eventType)
    }

    @Test
    fun `auditLocationUpdate skips close point under thresholds`() {
        val bike = buildBike(id = 7L)
        val trip = buildTrip(id = 15L, bikeId = 7L)
        val baseTime = LocalDateTime.now()

        `when`(tripRepository.findFirstByBikeIdAndStatusOrderByStartedAtDesc(7L, TripStatus.ACTIVE))
            .thenReturn(Optional.of(trip))
        `when`(bicycleLocationHistoryRepository.findFirstByBikeIdOrderByRecordedAtDesc(7L))
            .thenReturn(
                BicycleLocationHistory(
                    bikeId = 7L,
                    tripId = 15L,
                    stationId = 3L,
                    eventType = "LOCATION_UPDATE",
                    source = "BIKE_WEBSOCKET",
                    latitude = BigDecimal("14.9722000"),
                    longitude = BigDecimal("-89.5305000"),
                    recordedAt = baseTime,
                    receivedAt = baseTime
                )
            )
        `when`(bicicletaRepository.findById(7L)).thenReturn(Optional.of(bike))

        val result = deviceLocationAuditService.auditLocationUpdate(
            bikeId = 7L,
            latitude = 14.97225,
            longitude = -89.5305,
            recordedAt = baseTime.plusSeconds(5)
        )

        assertEquals(false, result.persisted)
        verify(bicicletaRepository, never()).save(any(Bicicleta::class.java))
        verify(bicycleLocationHistoryRepository, never()).save(any(BicycleLocationHistory::class.java))
    }

    private fun buildBike(id: Long) = Bicicleta(
        id = id,
        codigo = "BIC-$id",
        marca = "Trek",
        modelo = "FX",
        color = "Negro",
        tipo = TipoBicicleta.URBANA,
        tamanoLlanta = 29.0,
        precioPorHora = BigDecimal("10.00"),
        estado = EstadoBicicleta.EN_USO,
        codigoQr = "CYCLIX-BICI-BIC-$id",
        puesto = Puesto(
            id = 3L,
            nombre = "Centro",
            codigo = "PST-003",
            direccion = "Zona 1",
            latitud = 14.9722,
            longitud = -89.5305
        )
    )

    private fun buildTrip(id: Long, bikeId: Long) = Trip(
        id = id,
        user = User(
            id = 21L,
            firstName = "Diego",
            lastName = "Carias",
            email = "diego@test.com",
            phone = "88880000",
            passwordHash = "hash",
            role = Role(id = 1L, name = "USER"),
            status = UserStatus(id = 1L, name = "ACTIVE"),
            emailVerified = true
        ),
        bikeId = bikeId,
        status = TripStatus.ACTIVE,
        startLatitude = BigDecimal("14.9722"),
        startLongitude = BigDecimal("-89.5305"),
        startedAt = LocalDateTime.now().minusMinutes(3)
    )
}
