package com.cyclix.cyclix_api.pricing.dto

import com.cyclix.cyclix_api.pricing.entity.HolidayMode
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class PricingRuleRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String,
    @field:NotNull(message = "La prioridad es obligatoria")
    val priority: Int,
    @field:NotNull(message = "Debe indicar si está activa")
    val active: Boolean,
    @field:NotNull(message = "La tarifa base es obligatoria")
    @field:PositiveOrZero(message = "La tarifa base no puede ser negativa")
    val baseFare: BigDecimal,
    @field:NotNull(message = "Los minutos incluidos son obligatorios")
    @field:Positive(message = "Los minutos incluidos deben ser mayores que 0")
    val includedMinutes: Int,
    @field:NotNull(message = "La tarifa extra es obligatoria")
    @field:PositiveOrZero(message = "La tarifa extra no puede ser negativa")
    val extraFarePerBlock: BigDecimal,
    @field:NotNull(message = "El bloque extra es obligatorio")
    @field:Positive(message = "El bloque extra debe ser mayor que 0")
    val extraBlockMinutes: Int,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val daysOfWeek: String? = null,
    @field:NotNull(message = "holidayMode es obligatorio")
    val holidayMode: HolidayMode
)

data class PricingRuleResponse(
    val id: Long,
    val name: String,
    val priority: Int,
    val active: Boolean,
    val baseFare: BigDecimal,
    val includedMinutes: Int,
    val extraFarePerBlock: BigDecimal,
    val extraBlockMinutes: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val daysOfWeek: String?,
    val holidayMode: HolidayMode
)

data class HolidayRequest(
    @field:NotNull(message = "La fecha es obligatoria")
    val holidayDate: LocalDate?,
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String,
    @field:NotNull(message = "Debe indicar si está activo")
    val active: Boolean
)

data class HolidayResponse(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
    val active: Boolean
)
