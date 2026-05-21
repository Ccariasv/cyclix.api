package com.cyclix.cyclix_api.maintenance.repository

import com.cyclix.cyclix_api.maintenance.entity.MaintenanceOrder
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MaintenanceOrderRepository : JpaRepository<MaintenanceOrder, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<MaintenanceOrder>
    fun findAllByAssignedToIdOrderByCreatedAtDesc(userId: Long): List<MaintenanceOrder>
    fun findByIdAndAssignedToId(id: Long, userId: Long): Optional<MaintenanceOrder>
    fun existsByBikeIdAndStatusIn(bikeId: Long, statuses: Collection<MaintenanceStatus>): Boolean
    fun existsByTicketId(ticketId: Long): Boolean
}
