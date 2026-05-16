package com.cyclix.cyclix_api.subscription.dto

import com.cyclix.cyclix_api.subscription.entity.UserSubscriptionStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.LocalDateTime

data class SubscriptionPlanRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String,
    @field:NotNull(message = "El precio mensual es obligatorio")
    @field:PositiveOrZero(message = "El precio mensual no puede ser negativo")
    val monthlyPrice: BigDecimal,
    @field:NotNull(message = "Las horas incluidas son obligatorias")
    @field:Positive(message = "Las horas incluidas deben ser mayores que 0")
    val includedHours: Int,
    @field:NotNull(message = "Debe indicar si el plan está activo")
    val active: Boolean
)

data class SubscriptionPlanResponse(
    val id: Long,
    val name: String,
    val monthlyPrice: BigDecimal,
    val includedHours: Int,
    val active: Boolean
)

data class AssignSubscriptionRequest(
    @field:NotNull(message = "El id de usuario es obligatorio")
    @field:Positive(message = "El id de usuario debe ser positivo")
    val userId: Long?,
    @field:NotNull(message = "El id del plan es obligatorio")
    @field:Positive(message = "El id del plan debe ser positivo")
    val planId: Long?,
    @field:NotNull(message = "La fecha de inicio es obligatoria")
    val startsAt: LocalDateTime?,
    @field:NotNull(message = "La fecha de expiración es obligatoria")
    val expiresAt: LocalDateTime?,
    @field:NotNull(message = "Debe indicar auto renovación")
    val autoRenew: Boolean
)

data class UserSubscriptionResponse(
    val id: Long,
    val userId: Long,
    val planId: Long,
    val planName: String,
    val status: UserSubscriptionStatus,
    val startsAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val includedMinutes: Int,
    val consumedMinutes: Int,
    val remainingMinutes: Int,
    val autoRenew: Boolean
)
