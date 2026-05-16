package com.cyclix.cyclix_api.pricing.service

import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.pricing.dto.HolidayRequest
import com.cyclix.cyclix_api.pricing.dto.HolidayResponse
import com.cyclix.cyclix_api.pricing.dto.PricingRuleRequest
import com.cyclix.cyclix_api.pricing.dto.PricingRuleResponse
import com.cyclix.cyclix_api.pricing.entity.Holiday
import com.cyclix.cyclix_api.pricing.entity.HolidayMode
import com.cyclix.cyclix_api.pricing.entity.PricingRule
import com.cyclix.cyclix_api.pricing.repository.HolidayRepository
import com.cyclix.cyclix_api.pricing.repository.PricingRuleRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class PricingService(
    private val pricingRuleRepository: PricingRuleRepository,
    private val holidayRepository: HolidayRepository,
    private val auditService: AuditService
) {
    @Transactional(readOnly = true)
    fun listPricingRules(): List<PricingRuleResponse> =
        pricingRuleRepository.findAllByActiveTrueOrderByPriorityDescIdDesc().map { it.toResponse() }

    @Transactional
    fun createPricingRule(request: PricingRuleRequest): PricingRuleResponse {
        validateRuleWindow(request.startDate, request.endDate)
        val saved = pricingRuleRepository.save(
            PricingRule(
                name = request.name.trim(),
                priority = request.priority,
                active = request.active,
                baseFare = request.baseFare.setScale(2, RoundingMode.HALF_UP),
                includedMinutes = request.includedMinutes,
                extraFarePerBlock = request.extraFarePerBlock.setScale(2, RoundingMode.HALF_UP),
                extraBlockMinutes = request.extraBlockMinutes,
                startDate = request.startDate,
                endDate = request.endDate,
                startTime = request.startTime,
                endTime = request.endTime,
                daysOfWeek = request.daysOfWeek?.uppercase()?.trim(),
                holidayMode = request.holidayMode
            )
        )
        auditService.log("PRICING_RULE_CREATED", "pricing_rule", saved.id, "Regla ${saved.name} creada")
        return saved.toResponse()
    }

    @Transactional
    fun updatePricingRule(id: Long, request: PricingRuleRequest): PricingRuleResponse {
        validateRuleWindow(request.startDate, request.endDate)
        val rule = pricingRuleRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Regla de tarifa no encontrada: $id")
        }
        rule.name = request.name.trim()
        rule.priority = request.priority
        rule.active = request.active
        rule.baseFare = request.baseFare.setScale(2, RoundingMode.HALF_UP)
        rule.includedMinutes = request.includedMinutes
        rule.extraFarePerBlock = request.extraFarePerBlock.setScale(2, RoundingMode.HALF_UP)
        rule.extraBlockMinutes = request.extraBlockMinutes
        rule.startDate = request.startDate
        rule.endDate = request.endDate
        rule.startTime = request.startTime
        rule.endTime = request.endTime
        rule.daysOfWeek = request.daysOfWeek?.uppercase()?.trim()
        rule.holidayMode = request.holidayMode
        auditService.log("PRICING_RULE_UPDATED", "pricing_rule", rule.id, "Regla ${rule.name} actualizada")
        return rule.toResponse()
    }

    @Transactional(readOnly = true)
    fun listHolidays(): List<HolidayResponse> =
        holidayRepository.findAll().sortedBy { it.holidayDate }.map { it.toResponse() }

    @Transactional
    fun createHoliday(request: HolidayRequest): HolidayResponse {
        val date = request.holidayDate ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha es obligatoria")
        if (holidayRepository.findByHolidayDate(date) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un feriado para la fecha $date")
        }
        val holiday = holidayRepository.save(
            Holiday(
                holidayDate = date,
                name = request.name.trim(),
                active = request.active
            )
        )
        auditService.log("HOLIDAY_CREATED", "holiday", holiday.id, "Feriado ${holiday.name} creado")
        return holiday.toResponse()
    }

    @Transactional
    fun updateHoliday(id: Long, request: HolidayRequest): HolidayResponse {
        val holiday = holidayRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Feriado no encontrado: $id")
        }
        val newDate = request.holidayDate ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha es obligatoria")
        val dateOwner = holidayRepository.findByHolidayDate(newDate)
        if (dateOwner != null && dateOwner.id != id) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un feriado para la fecha $newDate")
        }
        holiday.holidayDate = newDate
        holiday.name = request.name.trim()
        holiday.active = request.active
        auditService.log("HOLIDAY_UPDATED", "holiday", holiday.id, "Feriado ${holiday.name} actualizado")
        return holiday.toResponse()
    }

    @Transactional(readOnly = true)
    fun calculate(endedAt: LocalDateTime, billableMinutes: Int): PricingCalculation {
        val rule = resolveApplicableRule(endedAt)
        if (billableMinutes <= 0) {
            return PricingCalculation(
                ruleId = rule.id,
                ruleName = rule.name,
                baseFareApplied = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                includedMinutesApplied = rule.includedMinutes,
                extraFarePerBlockApplied = rule.extraFarePerBlock,
                extraBlockMinutesApplied = rule.extraBlockMinutes,
                extraAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            )
        }
        val base = rule.baseFare

        if (billableMinutes <= rule.includedMinutes) {
            return PricingCalculation(
                ruleId = rule.id,
                ruleName = rule.name,
                baseFareApplied = base,
                includedMinutesApplied = rule.includedMinutes,
                extraFarePerBlockApplied = rule.extraFarePerBlock,
                extraBlockMinutesApplied = rule.extraBlockMinutes,
                extraAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                totalAmount = base
            )
        }

        val minutesOver = billableMinutes - rule.includedMinutes
        val blocks = kotlin.math.ceil(minutesOver.toDouble() / rule.extraBlockMinutes.toDouble()).toInt()
        val extra = rule.extraFarePerBlock.multiply(BigDecimal(blocks)).setScale(2, RoundingMode.HALF_UP)
        val total = base.add(extra).setScale(2, RoundingMode.HALF_UP)
        return PricingCalculation(
            ruleId = rule.id,
            ruleName = rule.name,
            baseFareApplied = base,
            includedMinutesApplied = rule.includedMinutes,
            extraFarePerBlockApplied = rule.extraFarePerBlock,
            extraBlockMinutesApplied = rule.extraBlockMinutes,
            extraAmount = extra,
            totalAmount = total
        )
    }

    @Transactional(readOnly = true)
    fun resolveApplicableRule(at: LocalDateTime): PricingRule {
        val isHoliday = holidayRepository.existsByHolidayDateAndActiveTrue(at.toLocalDate())
        return pricingRuleRepository.findAllByActiveTrueOrderByPriorityDescIdDesc()
            .firstOrNull { rule -> matchesRule(rule, at, isHoliday) }
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No existe una regla de tarifa aplicable")
    }

    private fun matchesRule(rule: PricingRule, at: LocalDateTime, isHoliday: Boolean): Boolean {
        if (!matchesDateRange(rule, at.toLocalDate())) return false
        if (!matchesHolidayMode(rule.holidayMode, isHoliday)) return false
        if (!matchesDayOfWeek(rule.daysOfWeek, at.dayOfWeek)) return false
        if (!matchesTimeWindow(rule.startTime, rule.endTime, at.toLocalTime())) return false
        return true
    }

    private fun matchesDateRange(rule: PricingRule, date: LocalDate): Boolean {
        val afterStart = rule.startDate?.let { !date.isBefore(it) } ?: true
        val beforeEnd = rule.endDate?.let { !date.isAfter(it) } ?: true
        return afterStart && beforeEnd
    }

    private fun matchesHolidayMode(mode: HolidayMode, isHoliday: Boolean): Boolean =
        when (mode) {
            HolidayMode.ANY -> true
            HolidayMode.HOLIDAY_ONLY -> isHoliday
            HolidayMode.NON_HOLIDAY -> !isHoliday
        }

    private fun matchesDayOfWeek(daysOfWeek: String?, day: DayOfWeek): Boolean {
        if (daysOfWeek.isNullOrBlank()) return true
        val allowed = daysOfWeek.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        return day.name in allowed
    }

    private fun matchesTimeWindow(start: LocalTime?, end: LocalTime?, candidate: LocalTime): Boolean {
        if (start == null || end == null) return true
        if (start == end) return true
        return if (start < end) {
            !candidate.isBefore(start) && candidate.isBefore(end)
        } else {
            !candidate.isBefore(start) || candidate.isBefore(end)
        }
    }

    private fun validateRuleWindow(startDate: LocalDate?, endDate: LocalDate?) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate no puede ser menor que startDate")
        }
    }

    private fun PricingRule.toResponse() = PricingRuleResponse(
        id = id,
        name = name,
        priority = priority,
        active = active,
        baseFare = baseFare,
        includedMinutes = includedMinutes,
        extraFarePerBlock = extraFarePerBlock,
        extraBlockMinutes = extraBlockMinutes,
        startDate = startDate,
        endDate = endDate,
        startTime = startTime,
        endTime = endTime,
        daysOfWeek = daysOfWeek,
        holidayMode = holidayMode
    )

    private fun Holiday.toResponse() = HolidayResponse(
        id = id,
        holidayDate = holidayDate,
        name = name,
        active = active
    )
}

data class PricingCalculation(
    val ruleId: Long,
    val ruleName: String,
    val baseFareApplied: BigDecimal,
    val includedMinutesApplied: Int,
    val extraFarePerBlockApplied: BigDecimal,
    val extraBlockMinutesApplied: Int,
    val extraAmount: BigDecimal,
    val totalAmount: BigDecimal
)
