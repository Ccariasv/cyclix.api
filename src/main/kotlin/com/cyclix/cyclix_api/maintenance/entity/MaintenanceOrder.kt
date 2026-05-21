package com.cyclix.cyclix_api.maintenance.entity

import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.support.entity.SupportTicket
import com.cyclix.cyclix_api.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "maintenance_orders")
class MaintenanceOrder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    var ticket: SupportTicket? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", nullable = false)
    var bike: Bicicleta,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    var assignedTo: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    var createdBy: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var priority: MaintenancePriority,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var type: MaintenanceType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: MaintenanceStatus = MaintenanceStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", length = 50)
    var resultStatus: MaintenanceResultStatus? = null,

    @Column(name = "reported_issue", nullable = false, columnDefinition = "TEXT")
    var reportedIssue: String,

    @Column(columnDefinition = "TEXT")
    var diagnosis: String? = null,

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    var resolutionNotes: String? = null,

    @Column(name = "current_location", length = 180)
    var currentLocation: String? = null,

    @Column(name = "estimated_minutes")
    var estimatedMinutes: Int? = null,

    @Column(name = "out_of_service_reason", columnDefinition = "TEXT")
    var outOfServiceReason: String? = null,

    @Column(name = "assigned_at")
    var assignedAt: LocalDateTime? = null,

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

enum class MaintenancePriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class MaintenanceType {
    CORRECTIVE,
    PREVENTIVE,
    INSPECTION,
    BRAKES,
    TIRES,
    CHAIN,
    ELECTRICAL,
    BATTERY,
    FRAME,
    GENERAL
}

enum class MaintenanceStatus {
    PENDING,
    ASSIGNED,
    IN_REVIEW,
    IN_REPAIR,
    WAITING_PARTS,
    PAUSED,
    FINALIZED
}

enum class MaintenanceResultStatus {
    STAYS_IN_MAINTENANCE,
    AVAILABLE,
    OUT_OF_SERVICE
}
