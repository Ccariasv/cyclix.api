package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.device.dto.DeviceUnlockRequest
import com.cyclix.cyclix_api.device.dto.DeviceUnlockResponse
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.trip.repository.TripRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class DeviceUnlockService(
    private val bicicletaRepository: BicicletaRepository,
    private val tripRepository: TripRepository,
    @Value("\${app.device.api-key}") private val deviceApiKey: String
) {
    @Transactional(readOnly = true)
    fun authorizeUnlock(
        bikeId: Long,
        request: DeviceUnlockRequest,
        providedApiKey: String?
    ): DeviceUnlockResponse {
        validateDeviceApiKey(providedApiKey)

        val tripId = request.tripId ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El ID del viaje es obligatorio"
        )

        val bicicleta = bicicletaRepository.findById(bikeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Bicicleta no encontrada: $bikeId")
        }

        if (bicicleta.estado != EstadoBicicleta.EN_USO) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "La bicicleta $bikeId no está en un estado válido para desbloqueo remoto"
            )
        }

        val trip = tripRepository.findByIdAndStatus(tripId, TripStatus.ACTIVE).orElseThrow {
            ResponseStatusException(
                HttpStatus.CONFLICT,
                "No existe un viaje activo con id $tripId para autorizar el desbloqueo"
            )
        }

        if (trip.bikeId != bikeId) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "El viaje $tripId no corresponde a la bicicleta $bikeId"
            )
        }

        return DeviceUnlockResponse(
            authorized = true,
            bikeId = bikeId,
            tripId = trip.id,
            command = "UNLOCK",
            authorizedAt = LocalDateTime.now(),
            message = "Desbloqueo autorizado"
        )
    }

    private fun validateDeviceApiKey(providedApiKey: String?) {
        if (deviceApiKey.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "La llave de dispositivos no está configurada"
            )
        }

        if (providedApiKey.isNullOrBlank() || providedApiKey != deviceApiKey) {
            throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales de dispositivo inválidas"
            )
        }
    }
}
