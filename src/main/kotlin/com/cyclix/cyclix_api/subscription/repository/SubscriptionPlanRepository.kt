package com.cyclix.cyclix_api.subscription.repository

import com.cyclix.cyclix_api.subscription.entity.SubscriptionPlan
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionPlanRepository : JpaRepository<SubscriptionPlan, Long>
