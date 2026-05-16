package com.cyclix.cyclix_api.audit.dto

import java.time.LocalDateTime

data class AuditLogResponse(
    val id: Long,
    val eventType: String,
    val userId: Long?,
    val entityType: String,
    val entityId: Long?,
    val details: String?,
    val createdAt: LocalDateTime
)
