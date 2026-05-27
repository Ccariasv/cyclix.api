package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.Optional

class DeviceLocationServiceTest {
    private lateinit var bicicletaRepository: BicicletaRepository
    private lateinit var deviceLocationService: DeviceLocationService

    @BeforeEach
    fun setup() {
        bicicletaRepository = mock(BicicletaRepository::class.java)
        deviceLocationService = DeviceLocationService(bicicletaRepository)
    }

    @Test
    fun `updateBikeLocation persists latest coordinates`() {
        val bicicleta = buildBike(id = 7L)
        `when`(bicicletaRepository.findById(7L)).thenReturn(Optional.of(bicicleta))
        `when`(bicicletaRepository.save(any(Bicicleta::class.java)))
            .thenAnswer { it.arguments[0] as Bicicleta }

        deviceLocationService.updateBikeLocation(
            bikeId = 7L,
            latitude = 14.9722,
            longitude = -89.5305
        )

        val captor = ArgumentCaptor.forClass(Bicicleta::class.java)
        verify(bicicletaRepository).save(captor.capture())
        assertEquals(14.9722, captor.value.latitud)
        assertEquals(-89.5305, captor.value.longitud)
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
        estado = EstadoBicicleta.DISPONIBLE,
        codigoQr = "CYCLIX-BICI-BIC-$id"
    )
}
