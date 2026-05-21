package com.cyclix.cyclix_api.support.controller

import com.cyclix.cyclix_api.support.dto.CreateSupportTicketRequest
import com.cyclix.cyclix_api.support.dto.SupportTicketResponse
import com.cyclix.cyclix_api.support.service.SupportTicketService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/support/tickets")
class SupportTicketController(
    private val supportTicketService: SupportTicketService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun createTicket(
        @Valid @RequestBody request: CreateSupportTicketRequest
    ): SupportTicketResponse =
        supportTicketService.createTicket(request)

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getMyTickets(): List<SupportTicketResponse> =
        supportTicketService.getMyTickets()

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getMyTicketById(@PathVariable id: Long): SupportTicketResponse =
        supportTicketService.getMyTicketById(id)
    @GetMapping("/health")
    fun healthCheck(): String {
        return "Support Tickets API OK"
    }
}
