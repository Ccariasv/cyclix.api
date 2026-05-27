package com.cyclix.cyclix_api.zone.service

import com.cyclix.cyclix_api.zone.dto.CreateZoneRequest
import com.cyclix.cyclix_api.zone.dto.UpdateZoneRequest
import com.cyclix.cyclix_api.zone.dto.ZoneResponse
import com.cyclix.cyclix_api.zone.dto.ZoneValidationResponse
import com.cyclix.cyclix_api.zone.entity.Zone
import com.cyclix.cyclix_api.zone.repository.ZoneRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

    @Transactional(readOnly = true)
    fun validateLocation(latitude: BigDecimal, longitude: BigDecimal): ZoneValidationResponse {
        val activeZones = zoneRepository.findAllByActiveTrueOrderByNameAsc()
        if (activeZones.isEmpty()) {
            return ZoneValidationResponse(
                allowed = false,
                zoneId = null,
                zoneName = null,
                distanceMeters = null,
                message = "No hay zonas activas configuradas"
            )
        }

        val pointLat = latitude.toDouble()
        val pointLon = longitude.toDouble()

        val zonesWithDistance = activeZones.map { zone ->
            zone to haversineDistanceMeters(
                pointLat,
                pointLon,
                zone.centerLatitude.toDouble(),
                zone.centerLongitude.toDouble()
            )
        }

        val matchingZone = zonesWithDistance
            .filter { (zone, distance) -> distance <= zone.radiusMeters }
            .minByOrNull { (_, distance) -> distance }

        if (matchingZone != null) {
            val (zone, distance) = matchingZone
            return ZoneValidationResponse(
                allowed = true,
                zoneId = zone.id,
                zoneName = zone.name,
                distanceMeters = roundDistance(distance),
                message = "La ubicación se encuentra dentro de la zona permitida: ${zone.name}"
            )
        }

        val nearestDistance = zonesWithDistance.minOf { (_, distance) -> distance }

        return ZoneValidationResponse(
            allowed = false,
            zoneId = null,
            zoneName = null,
            distanceMeters = roundDistance(nearestDistance),
            message = "La ubicación se encuentra fuera de las zonas permitidas"
        )
    }

    private fun haversineDistanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val lat1Rad = Math.toRadians(latitude1)
        val lat2Rad = Math.toRadians(latitude2)
        val deltaLatRad = Math.toRadians(latitude2 - latitude1)
        val deltaLonRad = Math.toRadians(longitude2 - longitude1)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
            cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c
    }

    private fun roundDistance(distanceMeters: Double): Double =
        BigDecimal.valueOf(distanceMeters).setScale(2, RoundingMode.HALF_UP).toDouble()

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
