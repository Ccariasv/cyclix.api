package com.cyclix.cyclix_api.subscription.controller

import com.cyclix.cyclix_api.subscription.dto.AssignSubscriptionRequest
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPlanRequest
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPlanResponse
import com.cyclix.cyclix_api.subscription.dto.UserSubscriptionResponse
import com.cyclix.cyclix_api.subscription.service.SubscriptionService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@PreAuthorize("hasRole('ADMIN')")
class AdminSubscriptionController(
    private val subscriptionService: SubscriptionService
) {
    @GetMapping("/plans")
    fun listPlans(): List<SubscriptionPlanResponse> = subscriptionService.listPlans()

    @PostMapping("/plans")
    fun createPlan(@Valid @RequestBody request: SubscriptionPlanRequest): SubscriptionPlanResponse =
        subscriptionService.createPlan(request)

    @PutMapping("/plans/{id}")
    fun updatePlan(
        @PathVariable id: Long,
        @Valid @RequestBody request: SubscriptionPlanRequest
    ): SubscriptionPlanResponse = subscriptionService.updatePlan(id, request)

    @PostMapping("/assign")
    fun assignPlan(@Valid @RequestBody request: AssignSubscriptionRequest): UserSubscriptionResponse =
        subscriptionService.assignPlanToUser(request)
}
