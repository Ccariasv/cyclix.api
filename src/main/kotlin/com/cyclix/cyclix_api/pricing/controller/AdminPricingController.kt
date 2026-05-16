package com.cyclix.cyclix_api.pricing.controller

import com.cyclix.cyclix_api.pricing.dto.HolidayRequest
import com.cyclix.cyclix_api.pricing.dto.HolidayResponse
import com.cyclix.cyclix_api.pricing.dto.PricingRuleRequest
import com.cyclix.cyclix_api.pricing.dto.PricingRuleResponse
import com.cyclix.cyclix_api.pricing.service.PricingService
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
@RequestMapping("/api/v1/admin/pricing")
@PreAuthorize("hasRole('ADMIN')")
class AdminPricingController(
    private val pricingService: PricingService
) {
    @GetMapping("/rules")
    fun listRules(): List<PricingRuleResponse> = pricingService.listPricingRules()

    @PostMapping("/rules")
    fun createRule(@Valid @RequestBody request: PricingRuleRequest): PricingRuleResponse =
        pricingService.createPricingRule(request)

    @PutMapping("/rules/{id}")
    fun updateRule(
        @PathVariable id: Long,
        @Valid @RequestBody request: PricingRuleRequest
    ): PricingRuleResponse = pricingService.updatePricingRule(id, request)

    @GetMapping("/holidays")
    fun listHolidays(): List<HolidayResponse> = pricingService.listHolidays()

    @PostMapping("/holidays")
    fun createHoliday(@Valid @RequestBody request: HolidayRequest): HolidayResponse =
        pricingService.createHoliday(request)

    @PutMapping("/holidays/{id}")
    fun updateHoliday(
        @PathVariable id: Long,
        @Valid @RequestBody request: HolidayRequest
    ): HolidayResponse = pricingService.updateHoliday(id, request)
}
