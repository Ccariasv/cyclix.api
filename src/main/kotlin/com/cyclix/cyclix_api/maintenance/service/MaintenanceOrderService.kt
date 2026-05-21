package com.cyclix.cyclix_api.maintenance.service

import com.cyclix.cyclix_api.bicycle.dto.PuestoInfoDto
import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.maintenance.dto.AssignMaintenanceOrderRequest
import com.cyclix.cyclix_api.maintenance.dto.CreateMaintenanceFromTicketRequest
import com.cyclix.cyclix_api.maintenance.dto.CreateMaintenanceOrderRequest
import com.cyclix.cyclix_api.maintenance.dto.MaintenanceBikeInfoDto
import com.cyclix.cyclix_api.maintenance.dto.MaintenanceHistoryResponse
import com.cyclix.cyclix_api.maintenance.dto.MaintenanceOrderDetailResponse
import com.cyclix.cyclix_api.maintenance.dto.MaintenanceOrderSummaryResponse
import com.cyclix.cyclix_api.maintenance.dto.MaintenanceUserSummaryDto
import com.cyclix.cyclix_api.maintenance.dto.ResolveMaintenanceOrderRequest
import com.cyclix.cyclix_api.maintenance.dto.UpdateMaintenanceProgressRequest
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceHistoryAction
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceOrder
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceOrderHistory
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceResultStatus
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceStatus
import com.cyclix.cyclix_api.maintenance.repository.MaintenanceOrderHistoryRepository
import com.cyclix.cyclix_api.maintenance.repository.MaintenanceOrderRepository
import com.cyclix.cyclix_api.puesto.model.Puesto
import com.cyclix.cyclix_api.support.entity.TicketCategory
import com.cyclix.cyclix_api.support.entity.TicketStatus
import com.cyclix.cyclix_api.support.repository.SupportTicketRepository
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class MaintenanceOrderService(
    private val maintenanceOrderRepository: MaintenanceOrderRepository,
    private val maintenanceOrderHistoryRepository: MaintenanceOrderHistoryRepository,
    private val bicicletaRepository: BicicletaRepository,
    private val supportTicketRepository: SupportTicketRepository,
    private val userRepository: UserRepository
) {
    private val activeStatuses = listOf(
        MaintenanceStatus.PENDING,
        MaintenanceStatus.ASSIGNED,
        MaintenanceStatus.IN_REVIEW,
        MaintenanceStatus.IN_REPAIR,
        MaintenanceStatus.WAITING_PARTS,
        MaintenanceStatus.PAUSED
    )

    @Transactional(readOnly = true)
    fun getAllForAdmin(): List<MaintenanceOrderSummaryResponse> =
        maintenanceOrderRepository.findAllByOrderByCreatedAtDesc().map { it.toSummaryResponse() }

    @Transactional(readOnly = true)
    fun getByIdForAdmin(orderId: Long): MaintenanceOrderDetailResponse =
        findOrderOrThrow(orderId).toDetailResponse()

    @Transactional
    fun createManual(request: CreateMaintenanceOrderRequest): MaintenanceOrderDetailResponse {
        val currentUser = getCurrentUser()
        val bike = findBikeOrThrow(request.bikeId)

        validateBikeCanEnterMaintenance(bike)
        ensureNoActiveOrderForBike(bike.id)

        val assignedUser = request.assignedToUserId?.let { findMaintenanceUserOrThrow(it) }
        val now = LocalDateTime.now()

        val order = maintenanceOrderRepository.save(
            MaintenanceOrder(
                bike = bike,
                assignedTo = assignedUser,
                createdBy = currentUser,
                priority = request.priority,
                type = request.type,
                status = if (assignedUser != null) MaintenanceStatus.ASSIGNED else MaintenanceStatus.PENDING,
                reportedIssue = request.reportedIssue.trim(),
                currentLocation = normalizeOptionalText(request.currentLocation),
                estimatedMinutes = request.estimatedMinutes,
                assignedAt = if (assignedUser != null) now else null
            )
        )

        moveBikeToMaintenance(bike)
        saveHistory(
            order = order,
            changedBy = currentUser,
            action = MaintenanceHistoryAction.CREATED,
            previousStatus = null,
            newStatus = order.status,
            note = if (assignedUser != null) {
                "Orden creada y asignada a ${assignedUser.firstName}."
            } else {
                "Orden creada sin técnico asignado."
            }
        )

        return findOrderOrThrow(order.id).toDetailResponse()
    }

    @Transactional
    fun createFromTicket(ticketId: Long, request: CreateMaintenanceFromTicketRequest): MaintenanceOrderDetailResponse {
        val currentUser = getCurrentUser()
        val ticket = supportTicketRepository.findById(ticketId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado: $ticketId")
        }

        if (ticket.category != TicketCategory.BIKE) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo los tickets de categoría BIKE pueden iniciar mantenimiento"
            )
        }

        val bikeId = ticket.bikeId ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El ticket $ticketId no tiene bicicleta asociada"
        )

        if (maintenanceOrderRepository.existsByTicketId(ticketId)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "El ticket $ticketId ya tiene una orden de mantenimiento asociada"
            )
        }

        val bike = findBikeOrThrow(bikeId)
        validateBikeCanEnterMaintenance(bike)
        ensureNoActiveOrderForBike(bike.id)

        val assignedUser = request.assignedToUserId?.let { findMaintenanceUserOrThrow(it) }
        val now = LocalDateTime.now()

        val order = maintenanceOrderRepository.save(
            MaintenanceOrder(
                ticket = ticket,
                bike = bike,
                assignedTo = assignedUser,
                createdBy = currentUser,
                priority = request.priority,
                type = request.type,
                status = if (assignedUser != null) MaintenanceStatus.ASSIGNED else MaintenanceStatus.PENDING,
                reportedIssue = ticket.description.trim(),
                currentLocation = normalizeOptionalText(request.currentLocation) ?: buildBikeLocation(bike),
                estimatedMinutes = request.estimatedMinutes,
                assignedAt = if (assignedUser != null) now else null
            )
        )

        moveBikeToMaintenance(bike)
        ticket.status = TicketStatus.IN_PROGRESS

        saveHistory(
            order = order,
            changedBy = currentUser,
            action = MaintenanceHistoryAction.CREATED,
            previousStatus = null,
            newStatus = order.status,
            note = "Orden creada desde ticket #${ticket.id}."
        )

        return findOrderOrThrow(order.id).toDetailResponse()
    }

    @Transactional
    fun assignForAdmin(orderId: Long, request: AssignMaintenanceOrderRequest): MaintenanceOrderDetailResponse {
        val currentUser = getCurrentUser()
        val order = findOrderOrThrow(orderId)
        ensureOrderIsActive(order)

        val assignedUser = findMaintenanceUserOrThrow(request.assignedToUserId)
        val previousStatus = order.status

        order.assignedTo = assignedUser
        order.assignedAt = LocalDateTime.now()
        order.estimatedMinutes = request.estimatedMinutes ?: order.estimatedMinutes
        if (order.status == MaintenanceStatus.PENDING) {
            order.status = MaintenanceStatus.ASSIGNED
        }

        saveHistory(
            order = order,
            changedBy = currentUser,
            action = MaintenanceHistoryAction.ASSIGNED,
            previousStatus = previousStatus,
            newStatus = order.status,
            note = "Orden asignada a ${assignedUser.firstName}."
        )

        return findOrderOrThrow(order.id).toDetailResponse()
    }

    @Transactional(readOnly = true)
    fun getMyOrders(): List<MaintenanceOrderSummaryResponse> {
        val currentUser = getCurrentUser()
        return maintenanceOrderRepository.findAllByAssignedToIdOrderByCreatedAtDesc(currentUser.id)
            .map { it.toSummaryResponse() }
    }

    @Transactional(readOnly = true)
    fun getMyOrderById(orderId: Long): MaintenanceOrderDetailResponse {
        val currentUser = getCurrentUser()
        return maintenanceOrderRepository.findByIdAndAssignedToId(orderId, currentUser.id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada: $orderId")
            }
            .toDetailResponse()
    }

    @Transactional
    fun updateProgressForAdmin(orderId: Long, request: UpdateMaintenanceProgressRequest): MaintenanceOrderDetailResponse {
        val currentUser = getCurrentUser()
        val order = findOrderOrThrow(orderId)
        applyProgressUpdate(order, request, currentUser)
        return findOrderOrThrow(order.id).toDetailResponse()
    }

    @Transactional
    fun updateProgressForAssigned(orderId: Long, request: UpdateMaintenanceProgressRequest): MaintenanceOrderDetailResponse {
        val currentUser = getCurrentUser()
        val order = maintenanceOrderRepository.findByIdAndAssignedToId(orderId, currentUser.id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada: $orderId")
            }
        applyProgressUpdate(order, request, currentUser)
        return findOrderOrThrow(order.id).toDetailResponse()
    }

    @Transactional
    fun resolveForAdmin(orderId: Long, request: ResolveMaintenanceOrderRequest): MaintenanceOrderDetailResponse {
        val currentUser = getCurrentUser()
        val order = findOrderOrThrow(orderId)
        applyResolution(order, request, currentUser)
        return findOrderOrThrow(order.id).toDetailResponse()
    }

    @Transactional
    fun resolveForAssigned(orderId: Long, request: ResolveMaintenanceOrderRequest): MaintenanceOrderDetailResponse {
        val currentUser = getCurrentUser()
        val order = maintenanceOrderRepository.findByIdAndAssignedToId(orderId, currentUser.id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada: $orderId")
            }
        applyResolution(order, request, currentUser)
        return findOrderOrThrow(order.id).toDetailResponse()
    }

    private fun applyProgressUpdate(
        order: MaintenanceOrder,
        request: UpdateMaintenanceProgressRequest,
        changedBy: User
    ) {
        ensureOrderIsActive(order)

        if (request.status == MaintenanceStatus.FINALIZED) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Para finalizar una orden use el endpoint de resolución"
            )
        }

        val previousStatus = order.status
        val normalizedDiagnosis = normalizeOptionalText(request.diagnosis)
        val normalizedResolutionNotes = normalizeOptionalText(request.resolutionNotes)
        val normalizedLocation = normalizeOptionalText(request.currentLocation)
        val normalizedNote = normalizeOptionalText(request.note)

        request.status?.let {
            order.status = it
            if (order.assignedTo != null && order.assignedAt == null) {
                order.assignedAt = LocalDateTime.now()
            }
            if (it in listOf(MaintenanceStatus.IN_REVIEW, MaintenanceStatus.IN_REPAIR) && order.startedAt == null) {
                order.startedAt = LocalDateTime.now()
            }
        }

        if (normalizedDiagnosis != null) {
            order.diagnosis = normalizedDiagnosis
        }
        if (normalizedResolutionNotes != null) {
            order.resolutionNotes = normalizedResolutionNotes
        }
        if (normalizedLocation != null) {
            order.currentLocation = normalizedLocation
        }
        if (request.estimatedMinutes != null) {
            order.estimatedMinutes = request.estimatedMinutes
        }

        saveHistory(
            order = order,
            changedBy = changedBy,
            action = MaintenanceHistoryAction.PROGRESS_UPDATED,
            previousStatus = previousStatus,
            newStatus = order.status,
            note = buildProgressNote(
                explicitNote = normalizedNote,
                diagnosisUpdated = normalizedDiagnosis != null,
                resolutionNotesUpdated = normalizedResolutionNotes != null,
                locationUpdated = normalizedLocation != null,
                estimatedMinutesUpdated = request.estimatedMinutes != null,
                statusChanged = previousStatus != order.status
            )
        )
    }

    private fun applyResolution(
        order: MaintenanceOrder,
        request: ResolveMaintenanceOrderRequest,
        changedBy: User
    ) {
        ensureOrderIsActive(order)

        val previousStatus = order.status
        val resolutionNotes = request.resolutionNotes.trim()
        val location = normalizeOptionalText(request.currentLocation)
        val outOfServiceReason = normalizeOptionalText(request.outOfServiceReason)

        when (request.resultStatus) {
            MaintenanceResultStatus.STAYS_IN_MAINTENANCE -> {
                order.resultStatus = request.resultStatus
                order.resolutionNotes = resolutionNotes
                order.currentLocation = location ?: order.currentLocation
                order.outOfServiceReason = null
                moveBikeToMaintenance(order.bike)
            }

            MaintenanceResultStatus.AVAILABLE -> {
                order.resultStatus = request.resultStatus
                order.resolutionNotes = resolutionNotes
                order.currentLocation = location ?: order.currentLocation
                order.outOfServiceReason = null
                order.status = MaintenanceStatus.FINALIZED
                order.completedAt = LocalDateTime.now()
                updateBikeStatus(order.bike, EstadoBicicleta.DISPONIBLE)
                order.ticket?.status = TicketStatus.RESOLVED
            }

            MaintenanceResultStatus.OUT_OF_SERVICE -> {
                if (outOfServiceReason == null) {
                    throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Debe indicar el motivo para dejar la bicicleta fuera de servicio"
                    )
                }
                order.resultStatus = request.resultStatus
                order.resolutionNotes = resolutionNotes
                order.currentLocation = location ?: order.currentLocation
                order.outOfServiceReason = outOfServiceReason
                order.status = MaintenanceStatus.FINALIZED
                order.completedAt = LocalDateTime.now()
                updateBikeStatus(order.bike, EstadoBicicleta.FUERA_DE_SERVICIO)
                order.ticket?.status = TicketStatus.RESOLVED
            }
        }

        saveHistory(
            order = order,
            changedBy = changedBy,
            action = MaintenanceHistoryAction.RESOLVED,
            previousStatus = previousStatus,
            newStatus = order.status,
            note = when (request.resultStatus) {
                MaintenanceResultStatus.STAYS_IN_MAINTENANCE ->
                    "La bicicleta continúa en mantenimiento."
                MaintenanceResultStatus.AVAILABLE ->
                    "La bicicleta quedó disponible para uso."
                MaintenanceResultStatus.OUT_OF_SERVICE ->
                    "La bicicleta quedó fuera de servicio. Motivo: $outOfServiceReason"
            }
        )
    }

    private fun findOrderOrThrow(orderId: Long): MaintenanceOrder =
        maintenanceOrderRepository.findById(orderId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada: $orderId")
        }

    private fun findBikeOrThrow(bikeId: Long): Bicicleta =
        bicicletaRepository.findById(bikeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Bicicleta no encontrada: $bikeId")
        }

    private fun findMaintenanceUserOrThrow(userId: Long): User {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: $userId")
        }

        if (user.role.name.uppercase() != "MAINTENANCE") {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario $userId no tiene rol MAINTENANCE"
            )
        }

        if (user.status.name.uppercase() != "ACTIVE") {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario $userId no está activo"
            )
        }

        return user
    }

    private fun getCurrentUser(): User {
        val principalEmail = SecurityContextHolder.getContext().authentication?.name?.trim()?.lowercase()
        if (principalEmail.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")
        }

        return userRepository.findByEmail(principalEmail).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado")
        }
    }

    private fun ensureNoActiveOrderForBike(bikeId: Long) {
        if (maintenanceOrderRepository.existsByBikeIdAndStatusIn(bikeId, activeStatuses)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "La bicicleta $bikeId ya tiene una orden de mantenimiento activa"
            )
        }
    }

    private fun validateBikeCanEnterMaintenance(bike: Bicicleta) {
        if (bike.estado == EstadoBicicleta.EN_USO || bike.estado == EstadoBicicleta.RESERVADA) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La bicicleta ${bike.codigo} no puede entrar a mantenimiento mientras esté ${bike.estado}"
            )
        }
    }

    private fun ensureOrderIsActive(order: MaintenanceOrder) {
        if (order.status == MaintenanceStatus.FINALIZED) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La orden ${order.id} ya fue finalizada"
            )
        }
    }

    private fun moveBikeToMaintenance(bike: Bicicleta) {
        updateBikeStatus(bike, EstadoBicicleta.MANTENIMIENTO)
    }

    private fun updateBikeStatus(bike: Bicicleta, newStatus: EstadoBicicleta) {
        bicicletaRepository.save(
            bike.copy(
                estado = newStatus,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    private fun saveHistory(
        order: MaintenanceOrder,
        changedBy: User,
        action: MaintenanceHistoryAction,
        previousStatus: MaintenanceStatus?,
        newStatus: MaintenanceStatus?,
        note: String?
    ) {
        maintenanceOrderHistoryRepository.save(
            MaintenanceOrderHistory(
                order = order,
                changedBy = changedBy,
                action = action,
                previousStatus = previousStatus,
                newStatus = newStatus,
                note = note
            )
        )
    }

    private fun MaintenanceOrder.toSummaryResponse() = MaintenanceOrderSummaryResponse(
        id = id,
        ticketId = ticket?.id,
        bike = bike.toBikeInfoDto(),
        assignedTo = assignedTo?.toUserSummaryDto(),
        createdBy = createdBy.toUserSummaryDto(),
        priority = priority,
        type = type,
        status = status,
        resultStatus = resultStatus,
        reportedIssue = reportedIssue,
        diagnosis = diagnosis,
        resolutionNotes = resolutionNotes,
        currentLocation = currentLocation,
        estimatedMinutes = estimatedMinutes,
        outOfServiceReason = outOfServiceReason,
        assignedAt = assignedAt,
        startedAt = startedAt,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun MaintenanceOrder.toDetailResponse(): MaintenanceOrderDetailResponse =
        MaintenanceOrderDetailResponse(
            id = id,
            ticketId = ticket?.id,
            bike = bike.toBikeInfoDto(),
            assignedTo = assignedTo?.toUserSummaryDto(),
            createdBy = createdBy.toUserSummaryDto(),
            priority = priority,
            type = type,
            status = status,
            resultStatus = resultStatus,
            reportedIssue = reportedIssue,
            diagnosis = diagnosis,
            resolutionNotes = resolutionNotes,
            currentLocation = currentLocation,
            estimatedMinutes = estimatedMinutes,
            outOfServiceReason = outOfServiceReason,
            assignedAt = assignedAt,
            startedAt = startedAt,
            completedAt = completedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            history = maintenanceOrderHistoryRepository.findAllByOrderIdOrderByCreatedAtAsc(id).map { it.toResponse() }
        )

    private fun MaintenanceOrderHistory.toResponse() = MaintenanceHistoryResponse(
        id = id,
        action = action,
        previousStatus = previousStatus,
        newStatus = newStatus,
        note = note,
        changedBy = changedBy.toUserSummaryDto(),
        createdAt = createdAt
    )

    private fun User.toUserSummaryDto() = MaintenanceUserSummaryDto(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email
    )

    private fun Bicicleta.toBikeInfoDto() = MaintenanceBikeInfoDto(
        id = id,
        codigo = codigo,
        marca = marca,
        modelo = modelo,
        tipo = tipo,
        estado = estado,
        puesto = puesto?.toPuestoInfoDto()
    )

    private fun Puesto.toPuestoInfoDto() = PuestoInfoDto(
        id = id,
        nombre = nombre,
        codigo = codigo,
        direccion = direccion,
        latitud = latitud,
        longitud = longitud
    )

    private fun normalizeOptionalText(value: String?): String? {
        val trimmed = value?.trim()
        return if (trimmed.isNullOrEmpty()) null else trimmed
    }

    private fun buildBikeLocation(bike: Bicicleta): String? =
        bike.puesto?.let { "${it.nombre} (${it.codigo})" }

    private fun buildProgressNote(
        explicitNote: String?,
        diagnosisUpdated: Boolean,
        resolutionNotesUpdated: Boolean,
        locationUpdated: Boolean,
        estimatedMinutesUpdated: Boolean,
        statusChanged: Boolean
    ): String {
        val changes = mutableListOf<String>()
        if (statusChanged) changes += "estado actualizado"
        if (diagnosisUpdated) changes += "diagnóstico actualizado"
        if (resolutionNotesUpdated) changes += "notas actualizadas"
        if (locationUpdated) changes += "ubicación actualizada"
        if (estimatedMinutesUpdated) changes += "tiempo estimado actualizado"

        val autoText = if (changes.isEmpty()) {
            "Se registró una actualización."
        } else {
            "Se registró una actualización: ${changes.joinToString(", ")}."
        }

        return explicitNote?.let { "$autoText Nota: $it" } ?: autoText
    }
}
