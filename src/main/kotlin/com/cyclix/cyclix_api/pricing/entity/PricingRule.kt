package com.cyclix.cyclix_api.pricing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "pricing_rules")
class PricingRule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false)
    var priority: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    var baseFare: BigDecimal,

    @Column(name = "included_minutes", nullable = false)
    var includedMinutes: Int,

    @Column(name = "extra_fare_per_block", nullable = false, precision = 12, scale = 2)
    var extraFarePerBlock: BigDecimal,

    @Column(name = "extra_block_minutes", nullable = false)
    var extraBlockMinutes: Int,

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "start_time")
    var startTime: LocalTime? = null,

    @Column(name = "end_time")
    var endTime: LocalTime? = null,

    @Column(name = "days_of_week", length = 120)
    var daysOfWeek: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_mode", nullable = false, length = 20)
    var holidayMode: HolidayMode = HolidayMode.ANY,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class HolidayMode {
    ANY,
    HOLIDAY_ONLY,
    NON_HOLIDAY
}
