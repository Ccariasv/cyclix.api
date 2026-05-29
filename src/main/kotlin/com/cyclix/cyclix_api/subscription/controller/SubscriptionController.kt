package com.cyclix.cyclix_api.subscription.controller

import com.cyclix.cyclix_api.subscription.dto.SubscriptionAutoRenewRequest
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPlanResponse
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPurchaseRequest
import com.cyclix.cyclix_api.subscription.dto.UserSubscriptionResponse
import com.cyclix.cyclix_api.subscription.service.SubscriptionService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {
    @GetMapping("/plans")
    fun listPlans(): List<SubscriptionPlanResponse> = subscriptionService.listActivePlans()

    @GetMapping("/my/active")
    fun getMyActiveSubscription(): UserSubscriptionResponse = subscriptionService.getMyActiveSubscription()

    @GetMapping("/my")
    fun listMySubscriptions(): List<UserSubscriptionResponse> = subscriptionService.listMySubscriptions()

    @PostMapping("/purchase")
    fun purchase(@Valid @RequestBody request: SubscriptionPurchaseRequest): UserSubscriptionResponse =
        subscriptionService.purchaseMySubscription(request)

    @PostMapping("/my/{id}/cancel")
    fun cancelMySubscription(@PathVariable id: Long): UserSubscriptionResponse =
        subscriptionService.cancelMySubscription(id)

    @PatchMapping("/my/{id}/auto-renew")
    fun updateMyAutoRenew(
        @PathVariable id: Long,
        @Valid @RequestBody request: SubscriptionAutoRenewRequest
    ): UserSubscriptionResponse = subscriptionService.updateMyAutoRenew(id, request)
}
