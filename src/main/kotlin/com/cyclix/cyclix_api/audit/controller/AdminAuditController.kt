package com.cyclix.cyclix_api.audit.controller

import com.cyclix.cyclix_api.audit.dto.AuditLogResponse
import com.cyclix.cyclix_api.audit.service.AuditService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
class AdminAuditController(
    private val auditService: AuditService
) {
    @GetMapping
    fun getAuditLogs(
        @RequestParam(required = false) eventType: String?,
        @RequestParam(required = false) entityType: String?
    ): List<AuditLogResponse> =
        auditService.getRecent(eventType, entityType).map {
            AuditLogResponse(
                id = it.id,
                eventType = it.eventType,
                userId = it.user?.id,
                entityType = it.entityType,
                entityId = it.entityId,
                details = it.details,
                createdAt = it.createdAt
            )
        }
}
