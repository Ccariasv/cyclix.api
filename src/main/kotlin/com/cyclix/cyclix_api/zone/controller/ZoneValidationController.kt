package com.cyclix.cyclix_api.zone.controller

import com.cyclix.cyclix_api.zone.dto.ZoneValidationRequest
import com.cyclix.cyclix_api.zone.dto.ZoneValidationResponse
import com.cyclix.cyclix_api.zone.service.ZoneService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/zones")
class ZoneValidationController(
    private val zoneService: ZoneService
) {
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun validateLocation(@Valid @RequestBody request: ZoneValidationRequest): ZoneValidationResponse =
        zoneService.validateLocation(request.latitude, request.longitude)
}
