package com.cyclix.cyclix_api.zone.controller

import com.cyclix.cyclix_api.zone.dto.CreateZoneRequest
import com.cyclix.cyclix_api.zone.dto.UpdateZoneRequest
import com.cyclix.cyclix_api.zone.dto.UpdateZoneStatusRequest
import com.cyclix.cyclix_api.zone.dto.ZoneResponse
import com.cyclix.cyclix_api.zone.service.ZoneService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/zones")
@PreAuthorize("hasRole('ADMIN')")
class AdminZoneController(
    private val zoneService: ZoneService
) {
    @GetMapping
    fun listZones(): List<ZoneResponse> = zoneService.listZones()

    @GetMapping("/{id}")
    fun getZoneById(@PathVariable id: Long): ZoneResponse = zoneService.getZoneById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createZone(@Valid @RequestBody request: CreateZoneRequest): ZoneResponse =
        zoneService.createZone(request)

    @PutMapping("/{id}")
    fun updateZone(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateZoneRequest
    ): ZoneResponse = zoneService.updateZone(id, request)

    @PatchMapping("/{id}/status")
    fun updateZoneStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateZoneStatusRequest
    ): ZoneResponse = zoneService.updateZoneStatus(id, request.active)
}
