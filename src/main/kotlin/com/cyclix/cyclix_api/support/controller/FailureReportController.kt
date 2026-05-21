package com.cyclix.cyclix_api.support.controller

import com.cyclix.cyclix_api.support.dto.CreateFailureReportRequest
import com.cyclix.cyclix_api.support.dto.SupportTicketResponse
import com.cyclix.cyclix_api.support.service.SupportTicketService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

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

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFailureReport(
        @Valid @RequestBody request: CreateFailureReportRequest
    ): SupportTicketResponse =
        supportTicketService.createFailureReport(request)
}
