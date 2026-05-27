package com.cyclix.cyclix_api.device.service

import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.bicycle.model.Bicicleta
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class DeviceLocationService(
    private val bicicletaRepository: BicicletaRepository
) {
    @Transactional
    fun updateBikeLocation(
        bikeId: Long,
        latitude: Double,
        longitude: Double,
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): Bicicleta {
        val bicicleta = bicicletaRepository.findById(bikeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Bicicleta no encontrada: $bikeId")
        }

        return bicicletaRepository.save(
            bicicleta.copy(
                latitud = latitude,
                longitud = longitude,
                updatedAt = updatedAt
            )
        )
    }
}
