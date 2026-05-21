package com.cyclix.cyclix_api.support.controller

import com.cyclix.cyclix_api.support.dto.SupportTicketResponse
import com.cyclix.cyclix_api.support.service.SupportTicketService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/support/failure-reports")
class FailureReportController(
    private val supportTicketService: SupportTicketService
) {
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getMyFailureReports(): List<SupportTicketResponse> =
        supportTicketService.getMyFailureReports()

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getMyFailureReportById(@PathVariable id: Long): SupportTicketResponse =
        supportTicketService.getMyFailureReportById(id)
}
