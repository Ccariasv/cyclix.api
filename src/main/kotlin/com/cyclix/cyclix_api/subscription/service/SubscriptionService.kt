package com.cyclix.cyclix_api.subscription.service

import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.subscription.dto.AssignSubscriptionRequest
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPlanRequest
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPlanResponse
import com.cyclix.cyclix_api.subscription.dto.UserSubscriptionResponse
import com.cyclix.cyclix_api.subscription.entity.SubscriptionPlan
import com.cyclix.cyclix_api.subscription.entity.UserSubscription
import com.cyclix.cyclix_api.subscription.entity.UserSubscriptionStatus
import com.cyclix.cyclix_api.subscription.repository.SubscriptionPlanRepository
import com.cyclix.cyclix_api.subscription.repository.UserSubscriptionRepository
import com.cyclix.cyclix_api.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class SubscriptionService(
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService
) {
    @Transactional(readOnly = true)
    fun listPlans(): List<SubscriptionPlanResponse> =
        subscriptionPlanRepository.findAll().map { it.toResponse() }

    @Transactional
    fun createPlan(request: SubscriptionPlanRequest): SubscriptionPlanResponse {
        val saved = subscriptionPlanRepository.save(
            SubscriptionPlan(
                name = request.name.trim(),
                monthlyPrice = request.monthlyPrice.setScale(2, RoundingMode.HALF_UP),
                includedHours = request.includedHours,
                active = request.active
            )
        )
        auditService.log("SUBSCRIPTION_PLAN_CREATED", "subscription_plan", saved.id, "Plan ${saved.name} creado")
        return saved.toResponse()
    }

    @Transactional
    fun updatePlan(id: Long, request: SubscriptionPlanRequest): SubscriptionPlanResponse {
        val plan = subscriptionPlanRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado: $id")
        }
        plan.name = request.name.trim()
        plan.monthlyPrice = request.monthlyPrice.setScale(2, RoundingMode.HALF_UP)
        plan.includedHours = request.includedHours
        plan.active = request.active
        auditService.log("SUBSCRIPTION_PLAN_UPDATED", "subscription_plan", plan.id, "Plan ${plan.name} actualizado")
        return plan.toResponse()
    }

    @Transactional
    fun assignPlanToUser(request: AssignSubscriptionRequest): UserSubscriptionResponse {
        val userId = request.userId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userId es obligatorio")
        val planId = request.planId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "planId es obligatorio")
        val startsAt = request.startsAt ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "startsAt es obligatorio")
        val expiresAt = request.expiresAt ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt es obligatorio")
        if (!expiresAt.isAfter(startsAt)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt debe ser mayor que startsAt")
        }

        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: $userId")
        }
        val plan = subscriptionPlanRepository.findById(planId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado: $planId")
        }
        val includedMinutes = plan.includedHours * 60
        val subscription = userSubscriptionRepository.save(
            UserSubscription(
                user = user,
                plan = plan,
                status = UserSubscriptionStatus.ACTIVE,
                startsAt = startsAt,
                expiresAt = expiresAt,
                includedMinutes = includedMinutes,
                consumedMinutes = 0,
                remainingMinutes = includedMinutes,
                autoRenew = request.autoRenew
            )
        )
        auditService.log(
            "SUBSCRIPTION_ASSIGNED",
            "user_subscription",
            subscription.id,
            "Plan ${plan.name} asignado al usuario ${user.id}",
            user
        )
        return subscription.toResponse()
    }

    @Transactional
    fun consumeMinutes(userId: Long, tripMinutes: Int, at: LocalDateTime): SubscriptionConsumptionResult {
        if (tripMinutes <= 0) return SubscriptionConsumptionResult(null, 0, 0)
        val subscription =
            userSubscriptionRepository.findFirstByUserIdAndStatusAndStartsAtLessThanEqualAndExpiresAtGreaterThanEqualOrderByExpiresAtDesc(
                userId = userId,
                status = UserSubscriptionStatus.ACTIVE,
                nowStart = at,
                nowEnd = at
            ) ?: return SubscriptionConsumptionResult(null, 0, tripMinutes)

        if (subscription.expiresAt.isBefore(at) || subscription.remainingMinutes <= 0) {
            subscription.status = UserSubscriptionStatus.EXPIRED
            return SubscriptionConsumptionResult(subscription, 0, tripMinutes)
        }

        val consumedNow = minOf(subscription.remainingMinutes, tripMinutes)
        subscription.consumedMinutes += consumedNow
        subscription.remainingMinutes -= consumedNow
        if (subscription.remainingMinutes <= 0) {
            subscription.status = UserSubscriptionStatus.EXPIRED
        }

        auditService.log(
            "SUBSCRIPTION_MINUTES_CONSUMED",
            "user_subscription",
            subscription.id,
            "Consumidos $consumedNow minutos en viaje",
            subscription.user
        )
        return SubscriptionConsumptionResult(subscription, consumedNow, tripMinutes - consumedNow)
    }

    private fun SubscriptionPlan.toResponse() = SubscriptionPlanResponse(
        id = id,
        name = name,
        monthlyPrice = monthlyPrice,
        includedHours = includedHours,
        active = active
    )

    private fun UserSubscription.toResponse() = UserSubscriptionResponse(
        id = id,
        userId = user.id,
        planId = plan.id,
        planName = plan.name,
        status = status,
        startsAt = startsAt,
        expiresAt = expiresAt,
        includedMinutes = includedMinutes,
        consumedMinutes = consumedMinutes,
        remainingMinutes = remainingMinutes,
        autoRenew = autoRenew
    )
}

data class SubscriptionConsumptionResult(
    val subscription: UserSubscription?,
    val minutesCovered: Int,
    val billableMinutes: Int
)
