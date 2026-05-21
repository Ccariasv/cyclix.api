package com.cyclix.cyclix_api.support.dto

import com.cyclix.cyclix_api.support.entity.TicketPriority
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateFailureReportRequest(
    @field:NotNull(message = "El ID de la bicicleta es obligatorio")
    val bikeId: Long,

    val tripId: Long? = null,

    val priority: TicketPriority? = null,

    @field:NotBlank(message = "El título es obligatorio")
    @field:Size(max = 180, message = "El título no puede exceder 180 caracteres")
    val title: String,

    @field:NotBlank(message = "La descripción es obligatoria")
    @field:Size(max = 4000, message = "La descripción no puede exceder 4000 caracteres")
    val description: String
)
