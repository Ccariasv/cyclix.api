package com.cyclix.cyclix_api.zone.service

import com.cyclix.cyclix_api.zone.dto.CreateZoneRequest
import com.cyclix.cyclix_api.zone.dto.UpdateZoneRequest
import com.cyclix.cyclix_api.zone.dto.ZoneResponse
import com.cyclix.cyclix_api.zone.entity.Zone
import com.cyclix.cyclix_api.zone.repository.ZoneRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ZoneService(
    private val zoneRepository: ZoneRepository
) {
    @Transactional(readOnly = true)
    fun listZones(): List<ZoneResponse> =
        zoneRepository.findAllByOrderByNameAsc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getZoneById(zoneId: Long): ZoneResponse =
        findZoneOrThrow(zoneId).toResponse()

    @Transactional
    fun createZone(request: CreateZoneRequest): ZoneResponse {
        val zone = Zone(
            name = request.name.trim(),
            description = request.description?.trim()?.takeIf { it.isNotBlank() },
            centerLatitude = request.centerLatitude,
            centerLongitude = request.centerLongitude,
            radiusMeters = request.radiusMeters,
            active = request.active
        )

        return zoneRepository.save(zone).toResponse()
    }

    @Transactional
    fun updateZone(zoneId: Long, request: UpdateZoneRequest): ZoneResponse {
        val zone = findZoneOrThrow(zoneId)

        zone.name = request.name.trim()
        zone.description = request.description?.trim()?.takeIf { it.isNotBlank() }
        zone.centerLatitude = request.centerLatitude
        zone.centerLongitude = request.centerLongitude
        zone.radiusMeters = request.radiusMeters
        zone.active = request.active

        return zone.toResponse()
    }

    @Transactional
    fun updateZoneStatus(zoneId: Long, active: Boolean): ZoneResponse {
        val zone = findZoneOrThrow(zoneId)
        zone.active = active
        return zone.toResponse()
    }

    private fun findZoneOrThrow(zoneId: Long): Zone =
        zoneRepository.findById(zoneId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Zona no encontrada: $zoneId")
        }

    private fun Zone.toResponse(): ZoneResponse =
        ZoneResponse(
            id = id,
            name = name,
            description = description,
            centerLatitude = centerLatitude,
            centerLongitude = centerLongitude,
            radiusMeters = radiusMeters,
            active = active,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
