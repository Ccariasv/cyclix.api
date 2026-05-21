package com.cyclix.cyclix_api.maintenance.entity

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
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "maintenance_order_history")
class MaintenanceOrderHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_order_id", nullable = false)
    var order: MaintenanceOrder,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    var changedBy: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var action: MaintenanceHistoryAction,

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 50)
    var previousStatus: MaintenanceStatus? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 50)
    var newStatus: MaintenanceStatus? = null,

    @Column(columnDefinition = "TEXT")
    var note: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}

enum class MaintenanceHistoryAction {
    CREATED,
    ASSIGNED,
    PROGRESS_UPDATED,
    RESOLVED
}
