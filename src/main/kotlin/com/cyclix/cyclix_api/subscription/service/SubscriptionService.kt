package com.cyclix.cyclix_api.subscription.service

import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.subscription.dto.AssignSubscriptionRequest
import com.cyclix.cyclix_api.subscription.dto.SubscriptionAutoRenewRequest
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPlanRequest
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPlanResponse
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPurchaseRequest
import com.cyclix.cyclix_api.subscription.dto.UserSubscriptionResponse
import com.cyclix.cyclix_api.subscription.entity.SubscriptionPlan
import com.cyclix.cyclix_api.subscription.entity.UserSubscription
import com.cyclix.cyclix_api.subscription.entity.UserSubscriptionStatus
import com.cyclix.cyclix_api.subscription.repository.SubscriptionPlanRepository
import com.cyclix.cyclix_api.subscription.repository.UserSubscriptionRepository
import com.cyclix.cyclix_api.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.RoundingMode
import java.time.LocalDateTime
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.wallet.service.WalletService

@Service
class SubscriptionService(
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
    private val walletService: WalletService
) {
    @Transactional(readOnly = true)
    fun listPlans(): List<SubscriptionPlanResponse> =
        subscriptionPlanRepository.findAllByOrderByIdAsc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun listActivePlans(): List<SubscriptionPlanResponse> =
        subscriptionPlanRepository.findAllByActiveTrueOrderByIdAsc().map { it.toResponse() }

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
        validatePlanIsActive(plan)
        expireStaleActiveSubscriptions(user.id, LocalDateTime.now())
        ensureNoOverlappingSubscription(user.id, startsAt, expiresAt)
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
    fun getMyActiveSubscription(): UserSubscriptionResponse {
        val user = getCurrentUser()
        val now = LocalDateTime.now()
        expireStaleActiveSubscriptions(user.id, now)
        val subscription =
            userSubscriptionRepository.findFirstByUserIdAndStatusAndStartsAtLessThanEqualAndExpiresAtGreaterThanEqualOrderByExpiresAtDesc(
                userId = user.id,
                status = UserSubscriptionStatus.ACTIVE,
                nowStart = now,
                nowEnd = now
            ) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No tienes una suscripción activa")

        return subscription.toResponse()
    }

    @Transactional
    fun listMySubscriptions(): List<UserSubscriptionResponse> {
        val user = getCurrentUser()
        expireStaleActiveSubscriptions(user.id, LocalDateTime.now())
        return userSubscriptionRepository.findAllByUserIdOrderByStartsAtDesc(user.id).map { it.toResponse() }
    }

    @Transactional
    fun purchaseMySubscription(request: SubscriptionPurchaseRequest): UserSubscriptionResponse {
        val user = getCurrentUser()
        val planId = request.planId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "planId es obligatorio")
        val plan = subscriptionPlanRepository.findById(planId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado: $planId")
        }
        validatePlanIsActive(plan)

        val startsAt = LocalDateTime.now()
        val expiresAt = startsAt.plusMonths(1)
        expireStaleActiveSubscriptions(user.id, startsAt)
        ensureNoOverlappingSubscription(user.id, startsAt, expiresAt)

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

        walletService.debitForSubscriptionPurchase(user.id, subscription.id, plan.monthlyPrice)
        auditService.log(
            "SUBSCRIPTION_PURCHASED",
            "user_subscription",
            subscription.id,
            "Plan ${plan.name} comprado por el usuario ${user.id}",
            user
        )
        return subscription.toResponse()
    }

    @Transactional
    fun cancelMySubscription(id: Long): UserSubscriptionResponse {
        val user = getCurrentUser()
        val now = LocalDateTime.now()
        expireStaleActiveSubscriptions(user.id, now)
        val subscription = userSubscriptionRepository.findByIdAndUserId(id, user.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Suscripción no encontrada: $id")

        if (subscription.status != UserSubscriptionStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes cancelar suscripciones activas")
        }

        subscription.status = UserSubscriptionStatus.CANCELLED
        subscription.autoRenew = false
        auditService.log(
            "SUBSCRIPTION_CANCELLED",
            "user_subscription",
            subscription.id,
            "Suscripción ${subscription.id} cancelada por el usuario ${user.id}",
            user
        )
        return subscription.toResponse()
    }

    @Transactional
    fun updateMyAutoRenew(id: Long, request: SubscriptionAutoRenewRequest): UserSubscriptionResponse {
        val user = getCurrentUser()
        val now = LocalDateTime.now()
        expireStaleActiveSubscriptions(user.id, now)
        val subscription = userSubscriptionRepository.findByIdAndUserId(id, user.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Suscripción no encontrada: $id")

        if (subscription.status != UserSubscriptionStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes modificar autoRenew de suscripciones activas")
        }

        subscription.autoRenew = request.autoRenew
        auditService.log(
            "SUBSCRIPTION_AUTO_RENEW_UPDATED",
            "user_subscription",
            subscription.id,
            "autoRenew=${request.autoRenew} para la suscripción ${subscription.id}",
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
        autoRenew = autoRenew,
        monthlyPrice = plan.monthlyPrice
    )

    private fun validatePlanIsActive(plan: SubscriptionPlan) {
        if (!plan.active) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El plan seleccionado está inactivo")
        }
    }

    private fun ensureNoOverlappingSubscription(userId: Long, startsAt: LocalDateTime, expiresAt: LocalDateTime) {
        val overlapping = userSubscriptionRepository.findAllByUserIdAndStatusOrderByStartsAtAsc(
            userId,
            UserSubscriptionStatus.ACTIVE
        ).firstOrNull { existing ->
            existing.startsAt.isBefore(expiresAt) && existing.expiresAt.isAfter(startsAt)
        }

        if (overlapping != null) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "El usuario ya tiene una suscripción activa o futura que se superpone con ese período"
            )
        }
    }

    private fun expireStaleActiveSubscriptions(userId: Long, now: LocalDateTime) {
        userSubscriptionRepository.findAllByUserIdAndStatusOrderByStartsAtAsc(userId, UserSubscriptionStatus.ACTIVE)
            .filter { it.expiresAt.isBefore(now) || it.remainingMinutes <= 0 }
            .forEach { it.status = UserSubscriptionStatus.EXPIRED }
    }

    private fun getCurrentUser(): User {
        val principalEmail = SecurityContextHolder.getContext().authentication?.name?.trim()?.lowercase()
        if (principalEmail.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")
        }
        return userRepository.findByEmail(principalEmail)
            .orElseThrow {
                ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado")
            }
    }
}

data class SubscriptionConsumptionResult(
    val subscription: UserSubscription?,
    val minutesCovered: Int,
    val billableMinutes: Int
)
