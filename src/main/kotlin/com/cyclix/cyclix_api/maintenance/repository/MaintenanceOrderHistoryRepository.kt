package com.cyclix.cyclix_api.maintenance.repository

import com.cyclix.cyclix_api.maintenance.entity.MaintenanceOrderHistory
import org.springframework.data.jpa.repository.JpaRepository

interface MaintenanceOrderHistoryRepository : JpaRepository<MaintenanceOrderHistory, Long> {
    fun findAllByOrderIdOrderByCreatedAtAsc(orderId: Long): List<MaintenanceOrderHistory>
}
