package com.cyclix.cyclix_api.support.service

import com.cyclix.cyclix_api.support.dto.CreateFailureReportRequest
import com.cyclix.cyclix_api.support.dto.CreateSupportTicketRequest
import com.cyclix.cyclix_api.support.dto.SupportTicketResponse
import com.cyclix.cyclix_api.support.entity.SupportTicket
import com.cyclix.cyclix_api.support.entity.TicketCategory
import com.cyclix.cyclix_api.support.entity.TicketPriority
import com.cyclix.cyclix_api.support.entity.TicketStatus
import com.cyclix.cyclix_api.support.repository.SupportTicketRepository
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class SupportTicketService(
    private val supportTicketRepository: SupportTicketRepository,
    private val userRepository: UserRepository,
    private val supportReferenceValidator: SupportReferenceValidator
) {
    @Transactional
    fun createTicket(request: CreateSupportTicketRequest): SupportTicketResponse {
        val currentUser = getCurrentUser()
        val title = request.title.trim()
        val description = request.description.trim()

        if (title.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es obligatorio")
        }
        if (description.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria")
        }

        supportReferenceValidator.validateReferences(request.bikeId, request.tripId, request.paymentId)

        val resolvedPriority = when (request.category) {
            TicketCategory.EMERGENCY -> TicketPriority.CRITICAL
            else -> request.priority ?: TicketPriority.MEDIUM
        }

        val ticket = SupportTicket(
            user = currentUser,
            bikeId = request.bikeId,
            tripId = request.tripId,
            paymentId = request.paymentId,
            category = request.category,
            priority = resolvedPriority,
            status = TicketStatus.OPEN,
            title = title,
            description = description
        )

        return supportTicketRepository.save(ticket).toResponse()
    }

    @Transactional
    fun createFailureReport(request: CreateFailureReportRequest): SupportTicketResponse {
        val currentUser = getCurrentUser()
        val title = request.title.trim()
        val description = request.description.trim()

        if (title.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es obligatorio")
        }
        if (description.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria")
        }

        // Validar referencias: bikeId es obligatorio para una falla, tripId es opcional
        supportReferenceValidator.validateReferences(request.bikeId, request.tripId, null)

        val ticket = SupportTicket(
            user = currentUser,
            bikeId = request.bikeId,
            tripId = request.tripId,
            paymentId = null,
            category = TicketCategory.BIKE,
            priority = request.priority ?: TicketPriority.MEDIUM,
            status = TicketStatus.OPEN,
            title = title,
            description = description
        )

        return supportTicketRepository.save(ticket).toResponse()
    }

    @Transactional(readOnly = true)
    fun getMyTickets(): List<SupportTicketResponse> {
        val currentUser = getCurrentUser()
        return supportTicketRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.id)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getMyTicketById(ticketId: Long): SupportTicketResponse {
        val currentUser = getCurrentUser()
        return supportTicketRepository.findByIdAndUserId(ticketId, currentUser.id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado: $ticketId")
            }
            .toResponse()
    }

    @Transactional(readOnly = true)
    fun getAllTicketsForAdmin(): List<SupportTicketResponse> =
        supportTicketRepository.findAllByOrderByCreatedAtDesc()
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getMyFailureReports(): List<SupportTicketResponse> {
        val currentUser = getCurrentUser()
        return supportTicketRepository
            .findAllByUserIdAndCategoryOrderByCreatedAtDesc(currentUser.id, TicketCategory.BIKE)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getMyFailureReportById(reportId: Long): SupportTicketResponse {
        val currentUser = getCurrentUser()
        return supportTicketRepository
            .findByIdAndUserIdAndCategory(reportId, currentUser.id, TicketCategory.BIKE)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte de falla no encontrado: $reportId")
            }
            .toResponse()
    }

    @Transactional(readOnly = true)
    fun getAllFailureReportsForAdmin(): List<SupportTicketResponse> =
        supportTicketRepository.findAllByCategoryOrderByCreatedAtDesc(TicketCategory.BIKE)
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getFailureReportByIdForAdmin(reportId: Long): SupportTicketResponse =
        findFailureReportOrThrow(reportId).toResponse()

    @Transactional
    fun updateFailureReportStatusForAdmin(reportId: Long, rawStatus: String): SupportTicketResponse {
        val report = findFailureReportOrThrow(reportId)
        val newStatus = parseEnum<TicketStatus>(rawStatus, "status")
        report.status = newStatus
        return report.toResponse()
    }

    @Transactional
    fun resolveFailureReportForAdmin(reportId: Long): SupportTicketResponse {
        val report = findFailureReportOrThrow(reportId)
        report.status = TicketStatus.RESOLVED
        return report.toResponse()
    }

    @Transactional
    fun updateStatusForAdmin(ticketId: Long, rawStatus: String): SupportTicketResponse {
        val ticket = findTicketOrThrow(ticketId)
        val newStatus = parseEnum<TicketStatus>(rawStatus, "status")
        ticket.status = newStatus
        return ticket.toResponse()
    }

    @Transactional
    fun updatePriorityForAdmin(ticketId: Long, rawPriority: String): SupportTicketResponse {
        val ticket = findTicketOrThrow(ticketId)
        val newPriority = parseEnum<TicketPriority>(rawPriority, "priority")

        if (ticket.category == TicketCategory.EMERGENCY && newPriority != TicketPriority.CRITICAL) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Los tickets EMERGENCY siempre deben tener prioridad CRITICAL"
            )
        }

        ticket.priority = if (ticket.category == TicketCategory.EMERGENCY) {
            TicketPriority.CRITICAL
        } else {
            newPriority
        }

        return ticket.toResponse()
    }

    private fun findTicketOrThrow(ticketId: Long): SupportTicket =
        supportTicketRepository.findById(ticketId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado: $ticketId")
        }

    private fun findFailureReportOrThrow(reportId: Long): SupportTicket =
        supportTicketRepository.findByIdAndCategory(reportId, TicketCategory.BIKE).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte de falla no encontrado: $reportId")
        }

    private fun getCurrentUser(): User {
        val principalEmail = SecurityContextHolder.getContext().authentication?.name?.trim()?.lowercase()
        if (principalEmail.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")
        }

        return userRepository.findByEmail(principalEmail)
            .orElseThrow {
                ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado")
            }
    }

    private inline fun <reified E : Enum<E>> parseEnum(rawValue: String, fieldName: String): E {
        val normalized = rawValue.trim().uppercase()
        return try {
            enumValueOf<E>(normalized)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName inválido: $rawValue")
        }
    }

    private fun SupportTicket.toResponse(): SupportTicketResponse =
        SupportTicketResponse(
            id = id,
            userId = user.id,
            bikeId = bikeId,
            tripId = tripId,
            paymentId = paymentId,
            category = category,
            priority = priority,
            status = status,
            title = title,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
