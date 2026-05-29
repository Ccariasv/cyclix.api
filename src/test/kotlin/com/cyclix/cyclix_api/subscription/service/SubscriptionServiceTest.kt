package com.cyclix.cyclix_api.subscription.service

import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.audit.repository.AuditLogRepository
import com.cyclix.cyclix_api.subscription.dto.SubscriptionPurchaseRequest
import com.cyclix.cyclix_api.subscription.entity.SubscriptionPlan
import com.cyclix.cyclix_api.subscription.entity.UserSubscription
import com.cyclix.cyclix_api.subscription.entity.UserSubscriptionStatus
import com.cyclix.cyclix_api.subscription.repository.SubscriptionPlanRepository
import com.cyclix.cyclix_api.subscription.repository.UserSubscriptionRepository
import com.cyclix.cyclix_api.user.Role
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import com.cyclix.cyclix_api.user.UserStatus
import com.cyclix.cyclix_api.wallet.entity.Wallet
import com.cyclix.cyclix_api.wallet.entity.WalletTransaction
import com.cyclix.cyclix_api.wallet.repository.WalletRepository
import com.cyclix.cyclix_api.wallet.repository.WalletTransactionRepository
import com.cyclix.cyclix_api.wallet.service.WalletService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.math.BigDecimal
import java.time.LocalDateTime

class SubscriptionServiceTest {
    private lateinit var subscriptionPlanRepository: SubscriptionPlanRepository
    private lateinit var userSubscriptionRepository: UserSubscriptionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var walletTransactionRepository: WalletTransactionRepository
    private lateinit var auditService: AuditService
    private lateinit var walletService: WalletService
    private lateinit var subscriptionService: SubscriptionService

    @BeforeEach
    fun setup() {
        subscriptionPlanRepository = mock(SubscriptionPlanRepository::class.java)
        userSubscriptionRepository = mock(UserSubscriptionRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        auditLogRepository = mock(AuditLogRepository::class.java)
        walletRepository = mock(WalletRepository::class.java)
        walletTransactionRepository = mock(WalletTransactionRepository::class.java)
        auditService = AuditService(auditLogRepository)
        walletService = WalletService(
            walletRepository,
            walletTransactionRepository,
            userRepository,
            auditService
        )
        subscriptionService = SubscriptionService(
            subscriptionPlanRepository,
            userSubscriptionRepository,
            userRepository,
            auditService,
            walletService
        )
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
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

    @Test
    fun `purchase subscription charges wallet and returns created subscription`() {
        val user = buildUser()
        val plan = buildPlan(active = true)
        val savedSubscription = buildSubscription(user = user, plan = plan, remaining = 3000)
        val wallet = buildWallet(user = user, balance = BigDecimal("250.00"))

        authenticate(user.email)
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(subscriptionPlanRepository.findById(plan.id)).thenReturn(Optional.of(plan))
        `when`(userSubscriptionRepository.findAllByUserIdAndStatusOrderByStartsAtAsc(user.id, UserSubscriptionStatus.ACTIVE))
            .thenReturn(emptyList())
        `when`(userSubscriptionRepository.save(org.mockito.ArgumentMatchers.any(UserSubscription::class.java)))
            .thenReturn(savedSubscription)
        `when`(walletRepository.findByUserId(user.id)).thenReturn(wallet)
        `when`(walletTransactionRepository.save(org.mockito.ArgumentMatchers.any(WalletTransaction::class.java)))
            .thenAnswer { it.arguments[0] as WalletTransaction }

        val response = subscriptionService.purchaseMySubscription(
            SubscriptionPurchaseRequest(
                planId = plan.id,
                autoRenew = false
            )
        )

        assertEquals(savedSubscription.id, response.id)
        assertEquals(plan.monthlyPrice, response.monthlyPrice)
        assertEquals(BigDecimal("50.00"), wallet.balance)
        verify(walletTransactionRepository).save(org.mockito.ArgumentMatchers.any(WalletTransaction::class.java))
    }

    @Test
    fun `purchase subscription rejects inactive plan`() {
        val user = buildUser()
        val plan = buildPlan(active = false)

        authenticate(user.email)
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(subscriptionPlanRepository.findById(plan.id)).thenReturn(Optional.of(plan))

        val error = assertThrows(ResponseStatusException::class.java) {
            subscriptionService.purchaseMySubscription(
                SubscriptionPurchaseRequest(
                    planId = plan.id,
                    autoRenew = false
                )
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, error.statusCode)
    }

    @Test
    fun `purchase subscription rejects overlapping active subscription`() {
        val user = buildUser()
        val plan = buildPlan(active = true)
        val overlapping = buildSubscription(
            user = user,
            plan = plan,
            startsAt = LocalDateTime.now().minusDays(1),
            expiresAt = LocalDateTime.now().plusDays(20),
            remaining = 2500
        )

        authenticate(user.email)
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(subscriptionPlanRepository.findById(plan.id)).thenReturn(Optional.of(plan))
        `when`(userSubscriptionRepository.findAllByUserIdAndStatusOrderByStartsAtAsc(user.id, UserSubscriptionStatus.ACTIVE))
            .thenReturn(listOf(overlapping))

        val error = assertThrows(ResponseStatusException::class.java) {
            subscriptionService.purchaseMySubscription(
                SubscriptionPurchaseRequest(
                    planId = plan.id,
                    autoRenew = true
                )
            )
        }

        assertEquals(HttpStatus.CONFLICT, error.statusCode)
        verify(walletTransactionRepository, times(0)).save(org.mockito.ArgumentMatchers.any(WalletTransaction::class.java))
    }

    @Test
    fun `purchase subscription rejects insufficient wallet balance`() {
        val user = buildUser()
        val plan = buildPlan(active = true)
        val savedSubscription = buildSubscription(user = user, plan = plan, remaining = 3000)
        val wallet = buildWallet(user = user, balance = BigDecimal("50.00"))

        authenticate(user.email)
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(subscriptionPlanRepository.findById(plan.id)).thenReturn(Optional.of(plan))
        `when`(userSubscriptionRepository.findAllByUserIdAndStatusOrderByStartsAtAsc(user.id, UserSubscriptionStatus.ACTIVE))
            .thenReturn(emptyList())
        `when`(userSubscriptionRepository.save(org.mockito.ArgumentMatchers.any(UserSubscription::class.java)))
            .thenReturn(savedSubscription)
        `when`(walletRepository.findByUserId(user.id)).thenReturn(wallet)

        val error = assertThrows(ResponseStatusException::class.java) {
            subscriptionService.purchaseMySubscription(
                SubscriptionPurchaseRequest(
                    planId = plan.id,
                    autoRenew = false
                )
            )
        }

        assertEquals(HttpStatus.PAYMENT_REQUIRED, error.statusCode)
        verify(walletTransactionRepository, times(0)).save(org.mockito.ArgumentMatchers.any(WalletTransaction::class.java))
    }

    private fun buildSubscription(
        user: User = buildUser(),
        plan: SubscriptionPlan = buildPlan(),
        startsAt: LocalDateTime = LocalDateTime.of(2026, 5, 1, 0, 0),
        expiresAt: LocalDateTime = LocalDateTime.of(2026, 5, 31, 23, 59),
        remaining: Int
    ): UserSubscription {
        return UserSubscription(
            id = 1L,
            user = user,
            plan = plan,
            status = UserSubscriptionStatus.ACTIVE,
            startsAt = startsAt,
            expiresAt = expiresAt,
            includedMinutes = 3000,
            consumedMinutes = 3000 - remaining,
            remainingMinutes = remaining,
            autoRenew = false
        )
    }

    private fun buildPlan(active: Boolean = true) = SubscriptionPlan(
        id = 1L,
        name = "Plan 50h",
        monthlyPrice = BigDecimal("200.00"),
        includedHours = 50,
        active = active
    )

    private fun buildUser() = User(
        id = 1L,
        firstName = "Diego",
        lastName = "Carias",
        email = "test@cyclix.com",
        phone = "12345678",
        passwordHash = "hash",
        role = Role(id = 1, name = "USER", description = "User"),
        status = UserStatus(id = 1, name = "ACTIVE", description = "Active")
    )

    private fun buildWallet(user: User, balance: BigDecimal) = Wallet(
        id = 1L,
        user = user,
        balance = balance,
        currency = "GTQ"
    )

    private fun authenticate(email: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(email, null, emptyList())
    }
}
