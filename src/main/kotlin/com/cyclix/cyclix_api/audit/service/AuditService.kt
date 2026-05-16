package com.cyclix.cyclix_api.audit.service

import com.cyclix.cyclix_api.audit.entity.AuditLog
import com.cyclix.cyclix_api.audit.repository.AuditLogRepository
import com.cyclix.cyclix_api.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository
) {
    fun log(
        eventType: String,
        entityType: String,
        entityId: Long? = null,
        details: String? = null,
        user: User? = null
    ) {
        auditLogRepository.save(
            AuditLog(
                eventType = eventType,
                entityType = entityType,
                entityId = entityId,
                details = details,
                user = user
            )
        )
    }

    @Transactional(readOnly = true)
    fun getRecent(eventType: String?, entityType: String?): List<AuditLog> =
        when {
            !eventType.isNullOrBlank() -> auditLogRepository.findTop200ByEventTypeOrderByCreatedAtDesc(eventType.trim())
            !entityType.isNullOrBlank() -> auditLogRepository.findTop200ByEntityTypeOrderByCreatedAtDesc(entityType.trim())
            else -> auditLogRepository.findTop200ByOrderByCreatedAtDesc()
        }
}
