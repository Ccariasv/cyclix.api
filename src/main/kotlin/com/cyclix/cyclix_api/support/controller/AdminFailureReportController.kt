package com.cyclix.cyclix_api.support.controller

import com.cyclix.cyclix_api.support.dto.SupportTicketResponse
import com.cyclix.cyclix_api.support.dto.UpdateTicketStatusRequest
import com.cyclix.cyclix_api.support.service.SupportTicketService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/support/failure-reports")
@PreAuthorize("hasRole('ADMIN')")
class AdminFailureReportController(
    private val supportTicketService: SupportTicketService
) {
    @GetMapping
    fun getAllFailureReports(): List<SupportTicketResponse> =
        supportTicketService.getAllFailureReportsForAdmin()

    @GetMapping("/{id}")
    fun getFailureReportById(@PathVariable id: Long): SupportTicketResponse =
        supportTicketService.getFailureReportByIdForAdmin(id)

    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTicketStatusRequest
    ): SupportTicketResponse =
        supportTicketService.updateFailureReportStatusForAdmin(id, request.status)
}
