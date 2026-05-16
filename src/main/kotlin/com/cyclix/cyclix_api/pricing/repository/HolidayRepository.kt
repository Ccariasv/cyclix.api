package com.cyclix.cyclix_api.pricing.repository

import com.cyclix.cyclix_api.pricing.entity.Holiday
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface HolidayRepository : JpaRepository<Holiday, Long> {
    fun existsByHolidayDateAndActiveTrue(holidayDate: LocalDate): Boolean
    fun findByHolidayDate(holidayDate: LocalDate): Holiday?
}
