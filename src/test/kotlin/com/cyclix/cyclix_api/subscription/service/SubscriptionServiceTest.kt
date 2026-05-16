package com.cyclix.cyclix_api.subscription.service

import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.subscription.entity.SubscriptionPlan
import com.cyclix.cyclix_api.subscription.entity.UserSubscription
import com.cyclix.cyclix_api.subscription.entity.UserSubscriptionStatus
import com.cyclix.cyclix_api.subscription.repository.SubscriptionPlanRepository
import com.cyclix.cyclix_api.subscription.repository.UserSubscriptionRepository
import com.cyclix.cyclix_api.user.Role
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import com.cyclix.cyclix_api.user.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDateTime

class SubscriptionServiceTest {
    private lateinit var subscriptionPlanRepository: SubscriptionPlanRepository
    private lateinit var userSubscriptionRepository: UserSubscriptionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var auditService: AuditService
    private lateinit var subscriptionService: SubscriptionService

    @BeforeEach
    fun setup() {
        subscriptionPlanRepository = mock(SubscriptionPlanRepository::class.java)
        userSubscriptionRepository = mock(UserSubscriptionRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        auditService = mock(AuditService::class.java)
        subscriptionService = SubscriptionService(
            subscriptionPlanRepository,
            userSubscriptionRepository,
            userRepository,
            auditService
        )
    }

    @Test
    fun `consume minutes covers trip fully when enough remaining`() {
        val now = LocalDateTime.of(2026, 5, 16, 12, 0)
        val subscription = buildSubscription(remaining = 200)
        `when`(
            userSubscriptionRepository.findFirstByUserIdAndStatusAndStartsAtLessThanEqualAndExpiresAtGreaterThanEqualOrderByExpiresAtDesc(
                1L,
                UserSubscriptionStatus.ACTIVE,
                now,
                now
            )
        ).thenReturn(subscription)

        val result = subscriptionService.consumeMinutes(1L, 90, now)

        assertEquals(90, result.minutesCovered)
        assertEquals(0, result.billableMinutes)
        assertEquals(110, subscription.remainingMinutes)
    }

    @Test
    fun `consume minutes leaves billable remainder when subscription is not enough`() {
        val now = LocalDateTime.of(2026, 5, 16, 12, 0)
        val subscription = buildSubscription(remaining = 30)
        `when`(
            userSubscriptionRepository.findFirstByUserIdAndStatusAndStartsAtLessThanEqualAndExpiresAtGreaterThanEqualOrderByExpiresAtDesc(
                1L,
                UserSubscriptionStatus.ACTIVE,
                now,
                now
            )
        ).thenReturn(subscription)

        val result = subscriptionService.consumeMinutes(1L, 95, now)

        assertEquals(30, result.minutesCovered)
        assertEquals(65, result.billableMinutes)
        assertEquals(0, subscription.remainingMinutes)
        assertEquals(UserSubscriptionStatus.EXPIRED, subscription.status)
    }

    private fun buildSubscription(remaining: Int): UserSubscription {
        val role = Role(id = 1, name = "USER", description = "User")
        val userStatus = UserStatus(id = 1, name = "ACTIVE", description = "Active")
        val user = User(
            id = 1L,
            firstName = "Diego",
            lastName = "Carias",
            email = "test@cyclix.com",
            phone = "12345678",
            passwordHash = "hash",
            role = role,
            status = userStatus
        )
        val plan = SubscriptionPlan(
            id = 1L,
            name = "Plan 50h",
            monthlyPrice = BigDecimal("200.00"),
            includedHours = 50,
            active = true
        )
        return UserSubscription(
            id = 1L,
            user = user,
            plan = plan,
            status = UserSubscriptionStatus.ACTIVE,
            startsAt = LocalDateTime.of(2026, 5, 1, 0, 0),
            expiresAt = LocalDateTime.of(2026, 5, 31, 23, 59),
            includedMinutes = 3000,
            consumedMinutes = 3000 - remaining,
            remainingMinutes = remaining,
            autoRenew = false
        )
    }
}
