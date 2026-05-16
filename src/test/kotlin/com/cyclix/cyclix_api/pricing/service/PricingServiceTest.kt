package com.cyclix.cyclix_api.pricing.service

import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.pricing.entity.HolidayMode
import com.cyclix.cyclix_api.pricing.entity.PricingRule
import com.cyclix.cyclix_api.pricing.repository.HolidayRepository
import com.cyclix.cyclix_api.pricing.repository.PricingRuleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDateTime

class PricingServiceTest {
    private lateinit var pricingRuleRepository: PricingRuleRepository
    private lateinit var holidayRepository: HolidayRepository
    private lateinit var auditService: AuditService
    private lateinit var pricingService: PricingService

    @BeforeEach
    fun setup() {
        pricingRuleRepository = mock(PricingRuleRepository::class.java)
        holidayRepository = mock(HolidayRepository::class.java)
        auditService = mock(AuditService::class.java)
        pricingService = PricingService(pricingRuleRepository, holidayRepository, auditService)
    }

    @Test
    fun `calculate returns base fare when billable minutes are inside included range`() {
        val now = LocalDateTime.of(2026, 5, 16, 10, 0)
        mockDefaultRule(now)

        val result = pricingService.calculate(now, 90)

        assertEquals(BigDecimal("20.00"), result.totalAmount)
        assertEquals(BigDecimal("0.00"), result.extraAmount)
    }

    @Test
    fun `calculate applies ceil blocks for extra minutes`() {
        val now = LocalDateTime.of(2026, 5, 16, 10, 0)
        mockDefaultRule(now)

        val result = pricingService.calculate(now, 121)

        assertEquals(BigDecimal("25.00"), result.totalAmount)
        assertEquals(BigDecimal("5.00"), result.extraAmount)
    }

    private fun mockDefaultRule(now: LocalDateTime) {
        val rule = PricingRule(
            id = 1L,
            name = "Tarifa estándar",
            priority = 1,
            active = true,
            baseFare = BigDecimal("20.00"),
            includedMinutes = 120,
            extraFarePerBlock = BigDecimal("5.00"),
            extraBlockMinutes = 30,
            holidayMode = HolidayMode.ANY
        )
        `when`(holidayRepository.existsByHolidayDateAndActiveTrue(now.toLocalDate())).thenReturn(false)
        `when`(pricingRuleRepository.findAllByActiveTrueOrderByPriorityDescIdDesc()).thenReturn(listOf(rule))
    }
}
