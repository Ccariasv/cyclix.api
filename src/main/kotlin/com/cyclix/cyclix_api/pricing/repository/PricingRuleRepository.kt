package com.cyclix.cyclix_api.pricing.repository

import com.cyclix.cyclix_api.pricing.entity.PricingRule
import org.springframework.data.jpa.repository.JpaRepository

interface PricingRuleRepository : JpaRepository<PricingRule, Long> {
    fun findAllByActiveTrueOrderByPriorityDescIdDesc(): List<PricingRule>
}
