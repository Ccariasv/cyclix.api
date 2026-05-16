package com.cyclix.cyclix_api.wallet.service

import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import com.cyclix.cyclix_api.wallet.dto.WalletBalanceResponse
import com.cyclix.cyclix_api.wallet.dto.WalletTopUpRequest
import com.cyclix.cyclix_api.wallet.dto.WalletTransactionResponse
import com.cyclix.cyclix_api.wallet.entity.Wallet
import com.cyclix.cyclix_api.wallet.entity.WalletTransaction
import com.cyclix.cyclix_api.wallet.entity.WalletTransactionType
import com.cyclix.cyclix_api.wallet.repository.WalletRepository
import com.cyclix.cyclix_api.wallet.repository.WalletTransactionRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val walletTransactionRepository: WalletTransactionRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService
) {
    @Transactional(readOnly = true)
    fun getMyWallet(): WalletBalanceResponse {
        val user = getCurrentUser()
        val wallet = getOrCreateWallet(user)
        return wallet.toBalanceResponse()
    }

    @Transactional(readOnly = true)
    fun getMyTransactions(): List<WalletTransactionResponse> {
        val user = getCurrentUser()
        val wallet = getOrCreateWallet(user)
        return walletTransactionRepository.findAllByWalletIdOrderByCreatedAtDesc(wallet.id).map { it.toResponse() }
    }

    @Transactional
    fun topUp(request: WalletTopUpRequest): WalletBalanceResponse {
        val userId = request.userId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userId es obligatorio")
        val amount = request.amount ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "amount es obligatorio")
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: $userId")
        }
        val wallet = getOrCreateWallet(user)
        val normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP)
        val before = wallet.balance
        val after = before.add(normalizedAmount).setScale(2, RoundingMode.HALF_UP)
        wallet.balance = after
        walletRepository.save(wallet)
        walletTransactionRepository.save(
            WalletTransaction(
                wallet = wallet,
                type = WalletTransactionType.TOP_UP,
                amount = normalizedAmount,
                balanceBefore = before,
                balanceAfter = after,
                description = "Recarga manual"
            )
        )
        auditService.log("WALLET_TOP_UP", "wallet", wallet.id, "Recarga de $normalizedAmount", user)
        return wallet.toBalanceResponse()
    }

    @Transactional
    fun debitForTrip(userId: Long, tripId: Long, amount: BigDecimal): BigDecimal {
        val normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP)
        if (normalizedAmount <= BigDecimal.ZERO) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: $userId")
        }
        val wallet = getOrCreateWallet(user)
        val before = wallet.balance
        if (before < normalizedAmount) {
            auditService.log(
                "WALLET_CHARGE_REJECTED",
                "wallet",
                wallet.id,
                "Cobro de viaje $tripId rechazado por fondos insuficientes",
                user
            )
            throw ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Saldo insuficiente en wallet para finalizar viaje")
        }
        val after = before.subtract(normalizedAmount).setScale(2, RoundingMode.HALF_UP)
        wallet.balance = after
        walletRepository.save(wallet)
        walletTransactionRepository.save(
            WalletTransaction(
                wallet = wallet,
                type = WalletTransactionType.TRIP_CHARGE,
                amount = normalizedAmount,
                balanceBefore = before,
                balanceAfter = after,
                description = "Cobro de viaje",
                referenceType = "trip",
                referenceId = tripId
            )
        )
        auditService.log("WALLET_TRIP_CHARGED", "wallet", wallet.id, "Cobro de viaje $tripId por $normalizedAmount", user)
        return normalizedAmount
    }

    private fun getOrCreateWallet(user: User): Wallet {
        val existing = walletRepository.findByUserId(user.id)
        if (existing != null) return existing
        return walletRepository.save(
            Wallet(
                user = user,
                balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                currency = "GTQ"
            )
        )
    }

    private fun Wallet.toBalanceResponse() = WalletBalanceResponse(
        userId = user.id,
        balance = balance,
        currency = currency
    )

    private fun WalletTransaction.toResponse() = WalletTransactionResponse(
        id = id,
        type = type,
        amount = amount,
        balanceBefore = balanceBefore,
        balanceAfter = balanceAfter,
        description = description,
        referenceType = referenceType,
        referenceId = referenceId,
        createdAt = createdAt
    )

    private fun getCurrentUser(): User {
        val principalEmail = SecurityContextHolder.getContext().authentication?.name?.trim()?.lowercase()
        if (principalEmail.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")
        }
        return userRepository.findByEmail(principalEmail)
            .orElseThrow {
                ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado")
            }
    }
}
