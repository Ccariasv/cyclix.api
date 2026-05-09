package com.cyclix.cyclix_api.trip.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class FinishTripRequest(
    @field:NotNull(message = "La latitud final es obligatoria")
    @field:DecimalMin(value = "-90.0", message = "La latitud final mínima es -90")
    @field:DecimalMax(value = "90.0", message = "La latitud final máxima es 90")
    val endLatitude: BigDecimal?,

    @field:NotNull(message = "La longitud final es obligatoria")
    @field:DecimalMin(value = "-180.0", message = "La longitud final mínima es -180")
    @field:DecimalMax(value = "180.0", message = "La longitud final máxima es 180")
    val endLongitude: BigDecimal?,

    @field:PositiveOrZero(message = "La distancia no puede ser negativa")
    val distanceKm: BigDecimal? = null
)