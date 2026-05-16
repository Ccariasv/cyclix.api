package com.cyclix.cyclix_api.wallet.repository

import com.cyclix.cyclix_api.wallet.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface WalletRepository : JpaRepository<Wallet, Long> {
    fun findByUserId(userId: Long): Wallet?
}
