package com.cyclix.cyclix_api.wallet.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "wallet_transactions")
class WalletTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    var wallet: Wallet,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: WalletTransactionType,

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal,

    @Column(name = "balance_before", nullable = false, precision = 12, scale = 2)
    var balanceBefore: BigDecimal,

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    var balanceAfter: BigDecimal,

    @Column(length = 255)
    var description: String? = null,

    @Column(name = "reference_type", length = 50)
    var referenceType: String? = null,

    @Column(name = "reference_id")
    var referenceId: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)

enum class WalletTransactionType {
    TOP_UP,
    TRIP_CHARGE,
    REFUND,
    ADJUSTMENT
}
