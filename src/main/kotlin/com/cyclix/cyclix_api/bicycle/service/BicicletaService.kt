package com.cyclix.cyclix_api.bicycle.service

import com.cyclix.cyclix_api.bicycle.dto.*
import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.puesto.model.Puesto
import com.cyclix.cyclix_api.puesto.repository.PuestoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * BicicletaService: toda la lógica del negocio para bicicletas.
 *
 * Reglas importantes que aplicamos aquí:
 *  - No puede existir dos bicis con el mismo código
 *  - Al asignar a un puesto, el puesto debe existir y estar ACTIVO
 *  - Cuando una bici llega a un puesto → capacidadDisponible del puesto baja
 *  - Cuando una bici sale del puesto  → capacidadDisponible del puesto sube
 *  - No se puede cambiar estado de una bici FUERA_DE_SERVICIO
 */
@Service
class BicicletaService(
    private val bicicletaRepository: BicicletaRepository,
    private val puestoRepository: PuestoRepository
) {
    private val zacapaCoords = listOf(
        Pair(14.9722, -89.5305), // Parque Central de Zacapa
        Pair(14.9772, -89.5248), // Calzada de la Revolución
        Pair(14.9654, -89.5398), // Pradera Zacapa
        Pair(14.9815, -89.5212), // Hospital Regional
        Pair(14.9712, -89.5265), // 3a Calle / 15 Avenida
        Pair(14.9610, -89.5420), // Boulevard Alvaro Arzú
        Pair(14.9785, -89.5350), // Barrio La Reforma
        Pair(14.9738, -89.5288)  // Barrio El Centro
    )

    // --------------------------------------------------
    //  LISTAR
    // --------------------------------------------------

    fun listarTodas(): List<BicicletaResumenResponse> =
        bicicletaRepository.findAll().map { it.toResumenResponse() }

    fun listar(
        estado: EstadoBicicleta? = null,
        tipo: TipoBicicleta? = null,
        puestoId: Long? = null,
        soloDisponibles: Boolean = false
    ): List<BicicletaResumenResponse> {
        val bicicletas = when {
            puestoId != null && soloDisponibles -> bicicletaRepository.findDisponiblesEnPuesto(puestoId)
            puestoId != null -> bicicletaRepository.findByPuestoId(puestoId)
            estado != null -> bicicletaRepository.findByEstado(estado)
            tipo != null -> bicicletaRepository.findByTipo(tipo)
            soloDisponibles -> bicicletaRepository.findByEstado(EstadoBicicleta.DISPONIBLE)
            else -> bicicletaRepository.findAll()
        }

        return bicicletas.map { it.toResumenResponse() }
    }

    fun listarPorEstado(estado: EstadoBicicleta): List<BicicletaResumenResponse> =
        bicicletaRepository.findByEstado(estado).map { it.toResumenResponse() }

    fun listarPorTipo(tipo: TipoBicicleta): List<BicicletaResumenResponse> =
        bicicletaRepository.findByTipo(tipo).map { it.toResumenResponse() }

    fun listarDisponiblesEnPuesto(puestoId: Long): List<BicicletaResumenResponse> =
        bicicletaRepository.findDisponiblesEnPuesto(puestoId).map { it.toResumenResponse() }

    fun listarPorPuesto(puestoId: Long): List<BicicletaResumenResponse> =
        bicicletaRepository.findByPuestoId(puestoId).map { it.toResumenResponse() }

    fun listarSinPuesto(): List<BicicletaResumenResponse> =
        bicicletaRepository.findSinPuesto().map { it.toResumenResponse() }

    // --------------------------------------------------
    //  OBTENER POR ID
    // --------------------------------------------------

    fun obtenerPorId(id: Long): BicicletaResponse {
        val bici = buscarBiciOFallar(id)
        return bici.toResponse()
    }

    fun obtenerPorQr(codigoQr: String): BicicletaResponse {
        val bici = bicicletaRepository.findByCodigoQr(codigoQr)
            ?: throw NoSuchElementException("No se encontró bicicleta con ese código QR")
        return bici.toResponse()
    }

    // --------------------------------------------------
    //  CREAR
    // --------------------------------------------------

    @Transactional
    fun crear(request: BicicletaRequest): BicicletaResponse {
        // Validar código único
        if (bicicletaRepository.existsByCodigo(request.codigo.uppercase())) {
            throw IllegalArgumentException("Ya existe una bicicleta con el código '${request.codigo}'")
        }

        // Validar puesto si viene uno
        val puesto = request.puestoId?.let { resolverPuesto(it) }

        // Generar el código QR: guardamos el código de la bici como texto.
        // La app móvil lo lee con la cámara para identificar la bici.
        val codigoQr = "CYCLIX-BICI-${request.codigo.uppercase()}"

        val nueva = Bicicleta(
            codigo       = request.codigo.uppercase(),
            marca        = request.marca,
            modelo       = request.modelo,
            color        = request.color,
            tipo         = request.tipo,
            tamanoLlanta = request.tamanoLlanta,
            precioPorHora = request.precioPorHora,
            estado       = EstadoBicicleta.DISPONIBLE,
            codigoQr     = codigoQr,
            puesto       = puesto
        )

        // Si tiene puesto, reducir capacidad disponible
        puesto?.let { reducirCapacidadPuesto(it) }

        val guardada = bicicletaRepository.save(nueva)
        return guardada.toResponse()
    }

    // --------------------------------------------------
    //  ACTUALIZAR DATOS GENERALES
    // --------------------------------------------------

    @Transactional
    fun actualizar(id: Long, request: BicicletaRequest): BicicletaResponse {
        val biciExistente = buscarBiciOFallar(id)

        // Validar cambio de código
        if (request.codigo.uppercase() != biciExistente.codigo) {
            if (bicicletaRepository.existsByCodigo(request.codigo.uppercase())) {
                throw IllegalArgumentException("Ya existe otra bicicleta con el código '${request.codigo}'")
            }
        }

        // Manejar cambio de puesto
        val nuevoPuesto = request.puestoId?.let { resolverPuesto(it) }
        val puestoAnterior = biciExistente.puesto

        // Si cambió de puesto → ajustar capacidades
        if (puestoAnterior?.id != nuevoPuesto?.id) {
            puestoAnterior?.let { aumentarCapacidadPuesto(it) } // libera espacio en el anterior
            nuevoPuesto?.let { reducirCapacidadPuesto(it) }     // ocupa espacio en el nuevo
        }

        val actualizada = biciExistente.copy(
            codigo        = request.codigo.uppercase(),
            marca         = request.marca,
            modelo        = request.modelo,
            color         = request.color,
            tipo          = request.tipo,
            tamanoLlanta  = request.tamanoLlanta,
            precioPorHora = request.precioPorHora,
            puesto        = nuevoPuesto,
            updatedAt     = LocalDateTime.now()
        )

        return bicicletaRepository.save(actualizada).toResponse()
    }

    // --------------------------------------------------
    //  CAMBIAR ESTADO (la operación más común)
    // --------------------------------------------------

    @Transactional
    fun cambiarEstado(id: Long, request: CambiarEstadoRequest): BicicletaResponse {
        val bici = buscarBiciOFallar(id)

        // Una bici FUERA_DE_SERVICIO no puede cambiar de estado
        if (bici.estado == EstadoBicicleta.FUERA_DE_SERVICIO) {
            throw IllegalStateException("No se puede cambiar el estado de una bicicleta FUERA_DE_SERVICIO")
        }

        val nuevoPuesto = request.puestoId?.let { resolverPuesto(it) }
        val puestoAnterior = bici.puesto

        // Ajustar capacidades si hay cambio de puesto
        if (puestoAnterior?.id != nuevoPuesto?.id) {
            puestoAnterior?.let { aumentarCapacidadPuesto(it) }
            nuevoPuesto?.let { reducirCapacidadPuesto(it) }
        }

        val actualizada = bici.copy(
            estado    = request.nuevoEstado,
            puesto    = nuevoPuesto,
            updatedAt = LocalDateTime.now()
        )

        return bicicletaRepository.save(actualizada).toResponse()
    }

    // --------------------------------------------------
    //  DAR DE BAJA
    // --------------------------------------------------

    @Transactional
    fun darDeBaja(id: Long): BicicletaResponse {
        val bici = buscarBiciOFallar(id)

        bici.puesto?.let { aumentarCapacidadPuesto(it) }

        val actualizada = bici.copy(
            estado = EstadoBicicleta.FUERA_DE_SERVICIO,
            puesto = null,
            updatedAt = LocalDateTime.now()
        )

        return bicicletaRepository.save(actualizada).toResponse()
    }

    // --------------------------------------------------
    //  FUNCIONES PRIVADAS DE APOYO
    // --------------------------------------------------

    /** Busca una bici o lanza error si no existe */
    private fun buscarBiciOFallar(id: Long): Bicicleta =
        bicicletaRepository.findById(id)
            .orElseThrow { NoSuchElementException("Bicicleta con id $id no encontrada") }

    /** Busca un puesto, valida que exista y esté ACTIVO */
    private fun resolverPuesto(puestoId: Long): Puesto {
        val puesto = puestoRepository.findById(puestoId)
            .orElseThrow { NoSuchElementException("Puesto con id $puestoId no encontrado") }
        if (puesto.estado.name != "ACTIVO") {
            throw IllegalStateException("El puesto '${puesto.nombre}' no está ACTIVO")
        }
        return puesto
    }

    /** Cuando llega una bici al puesto → baja la capacidad disponible */
    private fun reducirCapacidadPuesto(puesto: Puesto) {
        if (puesto.capacidadDisponible <= 0) {
            throw IllegalStateException("El puesto '${puesto.nombre}' no tiene espacio disponible")
        }
        val actualizado = puesto.copy(capacidadDisponible = puesto.capacidadDisponible - 1)
        puestoRepository.save(actualizado)
    }

    /** Cuando sale una bici del puesto → sube la capacidad disponible */
    private fun aumentarCapacidadPuesto(puesto: Puesto) {
        if (puesto.capacidadDisponible < puesto.capacidadTotal) {
            val actualizado = puesto.copy(capacidadDisponible = puesto.capacidadDisponible + 1)
            puestoRepository.save(actualizado)
        }
    }

    // --------------------------------------------------
    //  CONVERSIONES Entity → DTO
    // --------------------------------------------------

    private fun getCoordenadasSimuladas(id: Long): Pair<Double, Double> {
        val index = (id % zacapaCoords.size).toInt()
        return zacapaCoords[index]
    }

    private fun Bicicleta.toResponse(): BicicletaResponse {
        val coords = if (latitud != null && longitud != null) {
            Pair(latitud, longitud)
        } else {
            getCoordenadasSimuladas(id)
        }
        return BicicletaResponse(
            id            = id,
            codigo        = codigo,
            marca         = marca,
            modelo        = modelo,
            color         = color,
            tipo          = tipo,
            tamanoLlanta  = tamanoLlanta,
            precioPorHora = precioPorHora,
            estado        = estado,
            codigoQr      = codigoQr,
            latitud       = coords.first,
            longitud      = coords.second,
            puesto        = puesto?.toPuestoInfoDto(),
            createdAt     = createdAt,
            updatedAt     = updatedAt
        )
    }

    private fun Bicicleta.toResumenResponse(): BicicletaResumenResponse {
        val coords = if (latitud != null && longitud != null) {
            Pair(latitud, longitud)
        } else {
            getCoordenadasSimuladas(id)
        }
        return BicicletaResumenResponse(
            id            = id,
            codigo        = codigo,
            marca         = marca,
            modelo        = modelo,
            color         = color,
            tipo          = tipo,
            tamanoLlanta  = tamanoLlanta,
            precioPorHora = precioPorHora,
            estado        = estado,
            latitud       = coords.first,
            longitud      = coords.second,
            puesto        = puesto?.toPuestoInfoDto()
        )
    }

    private fun Puesto.toPuestoInfoDto() = PuestoInfoDto(
        id        = id,
        nombre    = nombre,
        codigo    = codigo,
        direccion = direccion,
        latitud   = latitud,
        longitud  = longitud
    )
}
