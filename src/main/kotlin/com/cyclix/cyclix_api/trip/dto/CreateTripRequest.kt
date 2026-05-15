package com.cyclix.cyclix_api.trip.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class CreateTripRequest(
    @field:NotNull(message = "El ID de la bicicleta es obligatorio")
    @field:Positive(message = "El ID de la bicicleta debe ser positivo")
    val bikeId: Long?,

    @field:NotNull(message = "La latitud inicial es obligatoria")
    @field:DecimalMin(value = "-90.0", message = "La latitud inicial mínima es -90")
    @field:DecimalMax(value = "90.0", message = "La latitud inicial máxima es 90")
    val startLatitude: BigDecimal?,

    @field:NotNull(message = "La longitud inicial es obligatoria")
    @field:DecimalMin(value = "-180.0", message = "La longitud inicial mínima es -180")
    @field:DecimalMax(value = "180.0", message = "La longitud inicial máxima es 180")
    val startLongitude: BigDecimal?
)