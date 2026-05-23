package com.cyclix.cyclix_api.device.controller

import com.cyclix.cyclix_api.device.dto.DeviceUnlockRequest
import com.cyclix.cyclix_api.device.dto.DeviceUnlockResponse
import com.cyclix.cyclix_api.device.service.DeviceUnlockService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/device/bikes")
class DeviceUnlockController(
    private val deviceUnlockService: DeviceUnlockService
) {
    @PostMapping("/{bikeId}/unlock")
    fun authorizeUnlock(
        @PathVariable bikeId: Long,
        @Valid @RequestBody request: DeviceUnlockRequest,
        @RequestHeader("X-Device-Api-Key", required = false) deviceApiKey: String?
    ): DeviceUnlockResponse =
        deviceUnlockService.authorizeUnlock(bikeId, request, deviceApiKey)
}
