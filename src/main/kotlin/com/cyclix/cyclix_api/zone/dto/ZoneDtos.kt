package com.cyclix.cyclix_api.zone.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateZoneRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String,
    val description: String? = null,
    @field:NotNull(message = "La latitud central es obligatoria")
    @field:DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @field:DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    val centerLatitude: BigDecimal,
    @field:NotNull(message = "La longitud central es obligatoria")
    @field:DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
    @field:DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180")
    val centerLongitude: BigDecimal,
    @field:NotNull(message = "El radio es obligatorio")
    @field:Positive(message = "El radio debe ser mayor que 0")
    val radiusMeters: Int,
    @field:NotNull(message = "Debe indicar si está activa")
    val active: Boolean = true
)

data class UpdateZoneRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String,
    val description: String? = null,
    @field:NotNull(message = "La latitud central es obligatoria")
    @field:DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @field:DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    val centerLatitude: BigDecimal,
    @field:NotNull(message = "La longitud central es obligatoria")
    @field:DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
    @field:DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180")
    val centerLongitude: BigDecimal,
    @field:NotNull(message = "El radio es obligatorio")
    @field:Positive(message = "El radio debe ser mayor que 0")
    val radiusMeters: Int,
    @field:NotNull(message = "Debe indicar si está activa")
    val active: Boolean
)

data class UpdateZoneStatusRequest(
    @field:NotNull(message = "Debe indicar si está activa")
    val active: Boolean
)

data class ZoneResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val centerLatitude: BigDecimal,
    val centerLongitude: BigDecimal,
    val radiusMeters: Int,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ZoneValidationRequest(
    @field:NotNull(message = "La latitud es obligatoria")
    @field:DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @field:DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    val latitude: BigDecimal,
    @field:NotNull(message = "La longitud es obligatoria")
    @field:DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180")
    @field:DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180")
    val longitude: BigDecimal
)

data class ZoneValidationResponse(
    val allowed: Boolean,
    val zoneId: Long?,
    val zoneName: String?,
    val distanceMeters: Double?,
    val message: String
)
