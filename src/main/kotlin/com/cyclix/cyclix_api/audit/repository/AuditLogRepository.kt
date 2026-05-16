package com.cyclix.cyclix_api.audit.repository

import com.cyclix.cyclix_api.audit.entity.AuditLog
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLog, Long> {
    fun findTop200ByOrderByCreatedAtDesc(): List<AuditLog>
    fun findTop200ByEventTypeOrderByCreatedAtDesc(eventType: String): List<AuditLog>
    fun findTop200ByEntityTypeOrderByCreatedAtDesc(entityType: String): List<AuditLog>
}
