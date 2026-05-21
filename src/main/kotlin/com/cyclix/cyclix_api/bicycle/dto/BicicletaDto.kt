package com.cyclix.cyclix_api.bicycle.dto

import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * BicicletaRequest: datos para CREAR o ACTUALIZAR una bicicleta.
 * El puesto es opcional porque puede registrarse sin uno.
 */
data class BicicletaRequest(

    @field:NotBlank(message = "El código no puede estar vacío")
    @field:Size(max = 50, message = "El código no puede superar 50 caracteres")
    val codigo: String,

    @field:NotBlank(message = "La marca no puede estar vacía")
    @field:Size(max = 100)
    val marca: String,

    @field:NotBlank(message = "El modelo no puede estar vacío")
    @field:Size(max = 100)
    val modelo: String,

    @field:NotBlank(message = "El color no puede estar vacío")
    @field:Size(max = 50)
    val color: String,

    @field:NotNull(message = "El tipo es obligatorio")
    val tipo: TipoBicicleta,

    @field:NotNull(message = "El tamaño de llanta es obligatorio")
    @field:DecimalMin(value = "10.0", message = "Tamaño de llanta inválido")
    @field:DecimalMax(value = "36.0", message = "Tamaño de llanta inválido")
    val tamanoLlanta: Double,

    @field:NotNull(message = "El precio por hora es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    val precioPorHora: BigDecimal,

    /** ID del puesto donde se registra — puede ser null */
    val puestoId: Long? = null
)

/**
 * CambiarEstadoRequest: para cuando solo queremos cambiar
 * el estado de una bici (ej: mandarla a mantenimiento).
 * También permite moverla a otro puesto al mismo tiempo.
 */
data class CambiarEstadoRequest(
    @field:NotNull(message = "El nuevo estado es obligatorio")
    val nuevoEstado: EstadoBicicleta,

    /** Puesto destino — puede ser null si va a mantenimiento */
    val puestoId: Long? = null
)

// =====================================================
//  DTO de SALIDA — lo que la API responde
// =====================================================

/**
 * BicicletaResponse: respuesta completa con todos los datos.
 * Usada para el detalle de una bicicleta específica.
 */
data class BicicletaResponse(
    val id: Long,
    val codigo: String,
    val marca: String,
    val modelo: String,
    val color: String,
    val tipo: TipoBicicleta,
    val tamanoLlanta: Double,
    val precioPorHora: BigDecimal,
    val estado: EstadoBicicleta,
    val codigoQr: String?,
    val latitud: Double?,
    val longitud: Double?,
    /** Info del puesto — null si está en mantenimiento/tránsito */
    val puesto: PuestoInfoDto?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * BicicletaResumenResponse: versión corta para listas.
 * No incluye fechas ni QR para no sobrecargar la respuesta.
 */
data class BicicletaResumenResponse(
    val id: Long,
    val codigo: String,
    val marca: String,
    val modelo: String,
    val color: String,
    val tipo: TipoBicicleta,
    val tamanoLlanta: Double,
    val precioPorHora: BigDecimal,
    val estado: EstadoBicicleta,
    val latitud: Double?,
    val longitud: Double?,
    val puesto: PuestoInfoDto?
)

/**
 * UbicacionRequest: DTO para recibir la ubicación enviada por el ESP32 o por pruebas.
 */
data class UbicacionRequest(
    @field:NotNull(message = "La latitud es obligatoria")
    @field:DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @field:DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    val latitud: Double,

    @field:NotNull(message = "La longitud es obligatoria")
    @field:DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
    @field:DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180")
    val longitud: Double
)

/**
 * PuestoInfoDto: datos mínimos del puesto dentro de la respuesta
 * de una bicicleta. No queremos devolver el puesto completo,
 * solo lo necesario para que la app sepa dónde está la bici.
 */
data class PuestoInfoDto(
    val id: Long,
    val nombre: String,
    val codigo: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double
)

/**
 * Respuesta estándar de la API (igual que en puestos)
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)