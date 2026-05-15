package com.cyclix.cyclix_api.puesto.service

import com.cyclix.cyclix_api.puesto.dto.PuestoRequest
import com.cyclix.cyclix_api.puesto.dto.PuestoResumenResponse
import com.cyclix.cyclix_api.puesto.dto.PuestoResponse
import com.cyclix.cyclix_api.puesto.model.EstadoPuesto
import com.cyclix.cyclix_api.puesto.model.Puesto
import com.cyclix.cyclix_api.puesto.repository.PuestoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * PuestoService: aquí vive toda la lógica del negocio.
 *
 * El Controller solo recibe y responde peticiones.
 * El Service decide QUÉ hacer y CÓMO hacerlo.
 * El Repository guarda y consulta en la BD.
 */
@Service
class PuestoService(
    private val puestoRepository: PuestoRepository
) {

    // --------------------------------------------------
    //  LISTAR TODOS LOS PUESTOS
    // --------------------------------------------------

    fun listarTodos(): List<PuestoResumenResponse> {
        return puestoRepository.findAll().map { it.toResumenResponse() }
    }

    fun listarActivos(): List<PuestoResumenResponse> {
        return puestoRepository.findByEstado(EstadoPuesto.ACTIVO).map { it.toResumenResponse() }
    }

    fun listarConEspacioDisponible(): List<PuestoResumenResponse> {
        return puestoRepository.findPuestosConEspacioDisponible().map { it.toResumenResponse() }
    }

    // --------------------------------------------------
    //  OBTENER UN PUESTO POR ID
    // --------------------------------------------------

    fun obtenerPorId(id: Long): PuestoResponse {
        val puesto = puestoRepository.findById(id)
            .orElseThrow { NoSuchElementException("Puesto con id $id no encontrado") }
        return puesto.toResponse()
    }

    // --------------------------------------------------
    //  CREAR UN PUESTO NUEVO (solo ADMIN)
    // --------------------------------------------------

    @Transactional
    fun crear(request: PuestoRequest): PuestoResponse {
        val codigoNormalizado = request.codigo.uppercase()

        // RB-04: Validar que el código no se repita
        if (puestoRepository.existsByCodigo(codigoNormalizado)) {
            throw IllegalArgumentException("Ya existe un puesto con el código '${request.codigo}'")
        }

        val nuevoPuesto = Puesto(
            nombre              = request.nombre,
            codigo              = codigoNormalizado, // guardamos en mayúsculas
            direccion           = request.direccion,
            latitud             = request.latitud,
            longitud            = request.longitud,
            capacidadTotal      = request.capacidadTotal,
            capacidadDisponible = request.capacidadTotal, // arranca con toda la capacidad libre
            estado              = EstadoPuesto.ACTIVO
        )

        val guardado = puestoRepository.save(nuevoPuesto)
        return guardado.toResponse()
    }

    // --------------------------------------------------
    //  ACTUALIZAR UN PUESTO (solo ADMIN)
    // --------------------------------------------------

    @Transactional
    fun actualizar(id: Long, request: PuestoRequest): PuestoResponse {
        val puestoExistente = puestoRepository.findById(id)
            .orElseThrow { NoSuchElementException("Puesto con id $id no encontrado") }

        // Si el código cambió, verificar que el nuevo código no lo use otro puesto
        if (request.codigo.uppercase() != puestoExistente.codigo) {
            if (puestoRepository.existsByCodigo(request.codigo.uppercase())) {
                throw IllegalArgumentException("Ya existe otro puesto con el código '${request.codigo}'")
            }
        }

        // Creamos la copia del puesto con los datos actualizados
        // (data class en Kotlin tiene .copy() que hace esto fácil)
        val puestoActualizado = puestoExistente.copy(
            nombre    = request.nombre,
            codigo    = request.codigo.uppercase(),
            direccion = request.direccion,
            latitud   = request.latitud,
            longitud  = request.longitud,
            // capacidadTotal puede cambiar, pero capacidadDisponible
            // se ajusta proporcionalmente para no perder la info actual
            capacidadTotal      = request.capacidadTotal,
            capacidadDisponible = minOf(puestoExistente.capacidadDisponible, request.capacidadTotal)
        )

        val guardado = puestoRepository.save(puestoActualizado)
        return guardado.toResponse()
    }

    // --------------------------------------------------
    //  CAMBIAR ESTADO DE UN PUESTO (solo ADMIN)
    // --------------------------------------------------

    @Transactional
    fun cambiarEstado(id: Long, nuevoEstado: EstadoPuesto): PuestoResponse {
        val puesto = puestoRepository.findById(id)
            .orElseThrow { NoSuchElementException("Puesto con id $id no encontrado") }

        val actualizado = puesto.copy(estado = nuevoEstado)
        return puestoRepository.save(actualizado).toResponse()
    }

    // --------------------------------------------------
    //  FUNCIONES DE CONVERSIÓN (Entity → DTO)
    //  Están aquí como extensiones privadas para mantener
    //  el Service limpio y fácil de leer
    // --------------------------------------------------

    private fun Puesto.toResponse() = PuestoResponse(
        id                  = id,
        nombre              = nombre,
        codigo              = codigo,
        direccion           = direccion,
        latitud             = latitud,
        longitud            = longitud,
        capacidadTotal      = capacidadTotal,
        capacidadDisponible = capacidadDisponible,
        estado              = estado,
        createdAt           = createdAt,
        updatedAt           = updatedAt
    )

    private fun Puesto.toResumenResponse() = PuestoResumenResponse(
        id                  = id,
        nombre              = nombre,
        codigo              = codigo,
        direccion           = direccion,
        latitud             = latitud,
        longitud            = longitud,
        capacidadDisponible = capacidadDisponible,
        capacidadTotal      = capacidadTotal,
        estado              = estado
    )
}
