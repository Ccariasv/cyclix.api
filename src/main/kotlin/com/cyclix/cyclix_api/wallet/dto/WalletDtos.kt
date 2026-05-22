package com.cyclix.cyclix_api.wallet.dto

import com.cyclix.cyclix_api.wallet.entity.WalletTransactionType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDateTime

data class WalletTopUpRequest(
    @field:NotNull(message = "El userId es obligatorio")
    @field:Positive(message = "El userId debe ser positivo")
    val userId: Long?,
    @field:NotNull(message = "El monto es obligatorio")
    @field:Positive(message = "El monto debe ser positivo")
    val amount: BigDecimal?
)

data class WalletSelfTopUpRequest(
    @field:NotNull(message = "El monto es obligatorio")
    @field:Positive(message = "El monto debe ser positivo")
    val amount: BigDecimal?,
    val paymentMethod: SimulatedPaymentMethod = SimulatedPaymentMethod.CARD
)

data class WalletBalanceResponse(
    val userId: Long,
    val balance: BigDecimal,
    val currency: String
)

data class WalletTransactionResponse(
    val id: Long,
    val type: WalletTransactionType,
    val amount: BigDecimal,
    val balanceBefore: BigDecimal,
    val balanceAfter: BigDecimal,
    val description: String?,
    val referenceType: String?,
    val referenceId: Long?,
    val createdAt: LocalDateTime
)

enum class SimulatedPaymentMethod {
    CARD,
    TRANSFER,
    CASH
}
