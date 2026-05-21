package com.cyclix.cyclix_api.maintenance.dto

import com.cyclix.cyclix_api.bicycle.dto.PuestoInfoDto
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.model.TipoBicicleta
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceHistoryAction
import com.cyclix.cyclix_api.maintenance.entity.MaintenancePriority
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceResultStatus
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceStatus
import com.cyclix.cyclix_api.maintenance.entity.MaintenanceType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateMaintenanceOrderRequest(
    @field:NotNull(message = "La bicicleta es obligatoria")
    val bikeId: Long,

    val assignedToUserId: Long? = null,

    @field:NotNull(message = "La prioridad es obligatoria")
    val priority: MaintenancePriority,

    @field:NotNull(message = "El tipo de mantenimiento es obligatorio")
    val type: MaintenanceType,

    @field:NotBlank(message = "La descripción del problema es obligatoria")
    @field:Size(max = 5000, message = "La descripción supera el máximo permitido")
    val reportedIssue: String,

    @field:Min(value = 1, message = "El tiempo estimado debe ser mayor a 0")
    val estimatedMinutes: Int? = null,

    @field:Size(max = 180, message = "La ubicación actual no puede exceder 180 caracteres")
    val currentLocation: String? = null
)

data class CreateMaintenanceFromTicketRequest(
    val assignedToUserId: Long? = null,

    @field:NotNull(message = "La prioridad es obligatoria")
    val priority: MaintenancePriority,

    @field:NotNull(message = "El tipo de mantenimiento es obligatorio")
    val type: MaintenanceType,

    @field:Min(value = 1, message = "El tiempo estimado debe ser mayor a 0")
    val estimatedMinutes: Int? = null,

    @field:Size(max = 180, message = "La ubicación actual no puede exceder 180 caracteres")
    val currentLocation: String? = null
)

data class AssignMaintenanceOrderRequest(
    @field:NotNull(message = "El técnico es obligatorio")
    val assignedToUserId: Long,

    @field:Min(value = 1, message = "El tiempo estimado debe ser mayor a 0")
    val estimatedMinutes: Int? = null
)

data class UpdateMaintenanceProgressRequest(
    val status: MaintenanceStatus? = null,

    @field:Size(max = 5000, message = "El diagnóstico supera el máximo permitido")
    val diagnosis: String? = null,

    @field:Size(max = 5000, message = "Las notas de resolución superan el máximo permitido")
    val resolutionNotes: String? = null,

    @field:Size(max = 180, message = "La ubicación actual no puede exceder 180 caracteres")
    val currentLocation: String? = null,

    @field:Min(value = 1, message = "El tiempo estimado debe ser mayor a 0")
    val estimatedMinutes: Int? = null,

    @field:Size(max = 1000, message = "La nota supera el máximo permitido")
    val note: String? = null
)

data class ResolveMaintenanceOrderRequest(
    @field:NotNull(message = "El resultado final es obligatorio")
    val resultStatus: MaintenanceResultStatus,

    @field:NotBlank(message = "Las notas de resolución son obligatorias")
    @field:Size(max = 5000, message = "Las notas de resolución superan el máximo permitido")
    val resolutionNotes: String,

    @field:Size(max = 5000, message = "El motivo supera el máximo permitido")
    val outOfServiceReason: String? = null,

    @field:Size(max = 180, message = "La ubicación actual no puede exceder 180 caracteres")
    val currentLocation: String? = null
)

data class MaintenanceUserSummaryDto(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val email: String
)

data class MaintenanceBikeInfoDto(
    val id: Long,
    val codigo: String,
    val marca: String,
    val modelo: String,
    val tipo: TipoBicicleta,
    val estado: EstadoBicicleta,
    val puesto: PuestoInfoDto?
)

data class MaintenanceHistoryResponse(
    val id: Long,
    val action: MaintenanceHistoryAction,
    val previousStatus: MaintenanceStatus?,
    val newStatus: MaintenanceStatus?,
    val note: String?,
    val changedBy: MaintenanceUserSummaryDto,
    val createdAt: LocalDateTime
)

data class MaintenanceOrderSummaryResponse(
    val id: Long,
    val ticketId: Long?,
    val bike: MaintenanceBikeInfoDto,
    val assignedTo: MaintenanceUserSummaryDto?,
    val createdBy: MaintenanceUserSummaryDto,
    val priority: MaintenancePriority,
    val type: MaintenanceType,
    val status: MaintenanceStatus,
    val resultStatus: MaintenanceResultStatus?,
    val reportedIssue: String,
    val diagnosis: String?,
    val resolutionNotes: String?,
    val currentLocation: String?,
    val estimatedMinutes: Int?,
    val outOfServiceReason: String?,
    val assignedAt: LocalDateTime?,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class MaintenanceOrderDetailResponse(
    val id: Long,
    val ticketId: Long?,
    val bike: MaintenanceBikeInfoDto,
    val assignedTo: MaintenanceUserSummaryDto?,
    val createdBy: MaintenanceUserSummaryDto,
    val priority: MaintenancePriority,
    val type: MaintenanceType,
    val status: MaintenanceStatus,
    val resultStatus: MaintenanceResultStatus?,
    val reportedIssue: String,
    val diagnosis: String?,
    val resolutionNotes: String?,
    val currentLocation: String?,
    val estimatedMinutes: Int?,
    val outOfServiceReason: String?,
    val assignedAt: LocalDateTime?,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val history: List<MaintenanceHistoryResponse>
)
