package com.cyclix.cyclix_api.subscription.repository

import com.cyclix.cyclix_api.subscription.entity.UserSubscription
import com.cyclix.cyclix_api.subscription.entity.UserSubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface UserSubscriptionRepository : JpaRepository<UserSubscription, Long> {
    fun findAllByUserIdOrderByStartsAtDesc(userId: Long): List<UserSubscription>

    fun findAllByUserIdAndStatusOrderByStartsAtAsc(
        userId: Long,
        status: UserSubscriptionStatus
    ): List<UserSubscription>

    fun findFirstByUserIdAndStatusAndStartsAtLessThanEqualAndExpiresAtGreaterThanEqualOrderByExpiresAtDesc(
        userId: Long,
        status: UserSubscriptionStatus,
        nowStart: LocalDateTime,
        nowEnd: LocalDateTime
    ): UserSubscription?

    fun findByIdAndUserId(id: Long, userId: Long): UserSubscription?
}
