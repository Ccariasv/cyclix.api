package com.cyclix.cyclix_api.puesto.dto

import com.cyclix.cyclix_api.puesto.model.EstadoPuesto
import jakarta.validation.constraints.*
import java.time.LocalDateTime

// =====================================================
//  DTO de ENTRADA — lo que el frontend nos manda
// =====================================================

/**
 * PuestoRequest: datos que el frontend envía cuando quiere
 * CREAR o ACTUALIZAR un puesto. Solo incluimos lo que el
 * cliente puede enviar, nunca el id ni las fechas.
 */
data class PuestoRequest(

    @field:NotBlank(message = "El nombre no puede estar vacío")
    @field:Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    val nombre: String,

    @field:NotBlank(message = "El código no puede estar vacío")
    @field:Size(max = 50, message = "El código no puede superar 50 caracteres")
    val codigo: String,

    @field:NotBlank(message = "La dirección no puede estar vacía")
    val direccion: String,

    @field:NotNull(message = "La latitud es obligatoria")
    @field:DecimalMin(value = "-90.0", message = "Latitud inválida")
    @field:DecimalMax(value = "90.0", message = "Latitud inválida")
    val latitud: Double,

    @field:NotNull(message = "La longitud es obligatoria")
    @field:DecimalMin(value = "-180.0", message = "Longitud inválida")
    @field:DecimalMax(value = "180.0", message = "Longitud inválida")
    val longitud: Double,

    @field:Min(value = 1, message = "La capacidad mínima es 1")
    @field:Max(value = 100, message = "La capacidad máxima es 100")
    val capacidadTotal: Int = 10
)

// =====================================================
//  DTO de SALIDA — lo que le devolvemos al frontend
// =====================================================

/**
 * PuestoResponse: lo que la API responde.
 * Aquí SÍ incluimos id, fechas y espacios disponibles.
 */
data class PuestoResponse(
    val id: Long,
    val nombre: String,
    val codigo: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val capacidadTotal: Int,
    val capacidadDisponible: Int,
    val estado: EstadoPuesto,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * PuestoResumenResponse: versión corta para listas.
 * El frontend no siempre necesita todos los campos.
 */
data class PuestoResumenResponse(
    val id: Long,
    val nombre: String,
    val codigo: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val capacidadDisponible: Int,
    val capacidadTotal: Int,
    val estado: EstadoPuesto
)

/**
 * Respuesta estándar de la API — siempre enviamos
 * el mismo formato para que el frontend sepa qué esperar.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)