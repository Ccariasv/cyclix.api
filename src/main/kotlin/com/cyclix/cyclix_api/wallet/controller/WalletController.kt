package com.cyclix.cyclix_api.wallet.controller

import com.cyclix.cyclix_api.wallet.dto.WalletBalanceResponse
import com.cyclix.cyclix_api.wallet.dto.WalletSelfTopUpRequest
import com.cyclix.cyclix_api.wallet.dto.WalletTopUpRequest
import com.cyclix.cyclix_api.wallet.dto.WalletTransactionResponse
import com.cyclix.cyclix_api.wallet.service.WalletService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/wallet")
class WalletController(
    private val walletService: WalletService
) {
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getMyWallet(): WalletBalanceResponse = walletService.getMyWallet()

    @GetMapping("/my/transactions")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getMyTransactions(): List<WalletTransactionResponse> = walletService.getMyTransactions()

    @PostMapping("/my/top-up")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun simulateMyTopUp(@Valid @RequestBody request: WalletSelfTopUpRequest): WalletBalanceResponse =
        walletService.simulateMyTopUp(request)

    @PostMapping("/top-up")
    @PreAuthorize("hasRole('ADMIN')")
    fun topUp(@Valid @RequestBody request: WalletTopUpRequest): WalletBalanceResponse =
        walletService.topUp(request)
}
