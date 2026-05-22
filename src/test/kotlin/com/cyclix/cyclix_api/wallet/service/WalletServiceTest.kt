package com.cyclix.cyclix_api.wallet.service

import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.audit.repository.AuditLogRepository
import com.cyclix.cyclix_api.user.Role
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import com.cyclix.cyclix_api.user.UserStatus
import com.cyclix.cyclix_api.wallet.dto.SimulatedPaymentMethod
import com.cyclix.cyclix_api.wallet.dto.WalletSelfTopUpRequest
import com.cyclix.cyclix_api.wallet.entity.Wallet
import com.cyclix.cyclix_api.wallet.entity.WalletTransaction
import com.cyclix.cyclix_api.wallet.repository.WalletRepository
import com.cyclix.cyclix_api.wallet.repository.WalletTransactionRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.util.Optional

class WalletServiceTest {
    private lateinit var walletRepository: WalletRepository
    private lateinit var walletTransactionRepository: WalletTransactionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var auditLogRepository: AuditLogRepository
    private lateinit var auditService: AuditService
    private lateinit var walletService: WalletService

    @BeforeEach
    fun setup() {
        walletRepository = mock(WalletRepository::class.java)
        walletTransactionRepository = mock(WalletTransactionRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        auditLogRepository = mock(AuditLogRepository::class.java)
        auditService = AuditService(auditLogRepository)
        walletService = WalletService(
            walletRepository,
            walletTransactionRepository,
            userRepository,
            auditService
        )
    }

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `simulate my top up credits authenticated user wallet and stores simulated transaction`() {
        val user = buildUser()
        val wallet = Wallet(
            id = 12L,
            user = user,
            balance = BigDecimal("25.00"),
            currency = "GTQ"
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(user.email, null, emptyList())
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(walletRepository.findByUserId(user.id)).thenReturn(wallet)
        `when`(walletRepository.save(any(Wallet::class.java))).thenAnswer { it.arguments[0] as Wallet }
        `when`(walletTransactionRepository.save(any(WalletTransaction::class.java)))
            .thenAnswer { it.arguments[0] as WalletTransaction }

        val response = walletService.simulateMyTopUp(
            WalletSelfTopUpRequest(
                amount = BigDecimal("40"),
                paymentMethod = SimulatedPaymentMethod.TRANSFER
            )
        )

        assertEquals(BigDecimal("65.00"), response.balance)
        assertEquals(BigDecimal("65.00"), wallet.balance)
        verify(walletTransactionRepository).save(any(WalletTransaction::class.java))
        verify(auditLogRepository).save(any())
    }

    @Test
    fun `simulate my top up creates wallet when authenticated user has none`() {
        val user = buildUser()
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(user.email, null, emptyList())
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(walletRepository.findByUserId(user.id)).thenReturn(null)
        `when`(walletRepository.save(any(Wallet::class.java))).thenAnswer { it.arguments[0] as Wallet }
        `when`(walletTransactionRepository.save(any(WalletTransaction::class.java)))
            .thenAnswer { it.arguments[0] as WalletTransaction }

        val response = walletService.simulateMyTopUp(
            WalletSelfTopUpRequest(
                amount = BigDecimal("100.00"),
                paymentMethod = SimulatedPaymentMethod.CARD
            )
        )

        assertEquals(BigDecimal("100.00"), response.balance)
        assertEquals("GTQ", response.currency)
        verify(walletRepository, times(2)).save(any(Wallet::class.java))
    }

    private fun buildUser(): User {
        val role = Role(id = 1L, name = "USER", description = "User")
        val status = UserStatus(id = 1L, name = "ACTIVE", description = "Active")
        return User(
            id = 7L,
            firstName = "Laura",
            lastName = "Lopez",
            email = "laura@cyclix.test",
            phone = "55551234",
            passwordHash = "hash",
            role = role,
            status = status
        )
    }
}
