package com.cyclix.cyclix_api.bicycle.service

import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.puesto.repository.PuestoRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.Optional

class BicicletaServiceTest {
    private lateinit var bicicletaRepository: BicicletaRepository
    private lateinit var puestoRepository: PuestoRepository
    private lateinit var bicicletaService: BicicletaService

    @BeforeEach
    fun setup() {
        bicicletaRepository = mock(BicicletaRepository::class.java)
        puestoRepository = mock(PuestoRepository::class.java)
        bicicletaService = BicicletaService(bicicletaRepository, puestoRepository)
    }

    @Test
    fun `listar returns all bicycles when no filters are provided`() {
        val bicicletas = listOf(buildBicicleta(id = 1L, codigo = "BIC-001"))
        `when`(bicicletaRepository.findAll()).thenReturn(bicicletas)

        val result = bicicletaService.listar()

        assertEquals(1, result.size)
        assertEquals("BIC-001", result.first().codigo)
        verify(bicicletaRepository).findAll()
    }

    @Test
    fun `listar filters by status`() {
        val bicicletas = listOf(buildBicicleta(id = 2L, codigo = "BIC-002"))
        `when`(bicicletaRepository.findByEstado(EstadoBicicleta.DISPONIBLE)).thenReturn(bicicletas)

        val result = bicicletaService.listar(estado = EstadoBicicleta.DISPONIBLE)

        assertEquals(1, result.size)
        assertEquals(EstadoBicicleta.DISPONIBLE, result.first().estado)
        verify(bicicletaRepository).findByEstado(EstadoBicicleta.DISPONIBLE)
    }

    @Test
    fun `listar filters available bicycles by station`() {
        val bicicletas = listOf(buildBicicleta(id = 3L, codigo = "BIC-003"))
        `when`(bicicletaRepository.findDisponiblesEnPuesto(5L)).thenReturn(bicicletas)

        val result = bicicletaService.listar(puestoId = 5L, soloDisponibles = true)

        assertEquals(1, result.size)
        assertEquals("BIC-003", result.first().codigo)
        verify(bicicletaRepository).findDisponiblesEnPuesto(5L)
    }

    @Test
    fun `darDeBaja marks bicycle as out of service`() {
        val bicicleta = buildBicicleta(id = 4L, codigo = "BIC-004")
        `when`(bicicletaRepository.findById(4L)).thenReturn(Optional.of(bicicleta))
        `when`(bicicletaRepository.save(org.mockito.ArgumentMatchers.any(Bicicleta::class.java)))
            .thenAnswer { it.arguments[0] as Bicicleta }

        val result = bicicletaService.darDeBaja(4L)

        assertEquals(EstadoBicicleta.FUERA_DE_SERVICIO, result.estado)
        assertEquals(null, result.puesto)
    }

    private fun buildBicicleta(id: Long, codigo: String) = Bicicleta(
        id = id,
        codigo = codigo,
        marca = "Trek",
        modelo = "FX",
        color = "Negro",
        tipo = TipoBicicleta.URBANA,
        tamanoLlanta = 29.0,
        precioPorHora = BigDecimal("10.00"),
        estado = EstadoBicicleta.DISPONIBLE,
        codigoQr = "CYCLIX-BICI-$codigo"
    )
}
