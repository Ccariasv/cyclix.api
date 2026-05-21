package com.cyclix.cyclix_api.support.repository

import com.cyclix.cyclix_api.support.entity.SupportTicket
import com.cyclix.cyclix_api.support.entity.TicketCategory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SupportTicketRepository : JpaRepository<SupportTicket, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<SupportTicket>
    fun findByIdAndUserId(id: Long, userId: Long): Optional<SupportTicket>
    fun findAllByOrderByCreatedAtDesc(): List<SupportTicket>
    fun findAllByUserIdAndCategoryOrderByCreatedAtDesc(
        userId: Long,
        category: TicketCategory
    ): List<SupportTicket>
    fun findByIdAndUserIdAndCategory(
        id: Long,
        userId: Long,
        category: TicketCategory
    ): Optional<SupportTicket>
    fun findAllByCategoryOrderByCreatedAtDesc(category: TicketCategory): List<SupportTicket>
    fun findByIdAndCategory(id: Long, category: TicketCategory): Optional<SupportTicket>
}
