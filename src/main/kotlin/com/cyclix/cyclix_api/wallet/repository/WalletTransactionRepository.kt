package com.cyclix.cyclix_api.wallet.repository

import com.cyclix.cyclix_api.wallet.entity.WalletTransaction
import org.springframework.data.jpa.repository.JpaRepository

interface WalletTransactionRepository : JpaRepository<WalletTransaction, Long> {
    fun findAllByWalletIdOrderByCreatedAtDesc(walletId: Long): List<WalletTransaction>
}
