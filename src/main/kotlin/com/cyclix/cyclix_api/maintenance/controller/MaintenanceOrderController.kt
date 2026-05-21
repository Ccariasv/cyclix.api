package com.cyclix.cyclix_api.maintenance.controller

import com.cyclix.cyclix_api.maintenance.dto.MaintenanceOrderDetailResponse
import com.cyclix.cyclix_api.maintenance.dto.MaintenanceOrderSummaryResponse
import com.cyclix.cyclix_api.maintenance.dto.ResolveMaintenanceOrderRequest
import com.cyclix.cyclix_api.maintenance.dto.UpdateMaintenanceProgressRequest
import com.cyclix.cyclix_api.maintenance.service.MaintenanceOrderService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/maintenance/orders")
@PreAuthorize("hasRole('MAINTENANCE')")
class MaintenanceOrderController(
    private val maintenanceOrderService: MaintenanceOrderService
) {
    @GetMapping("/my")
    fun getMyOrders(): List<MaintenanceOrderSummaryResponse> =
        maintenanceOrderService.getMyOrders()

    @GetMapping("/{id}")
    fun getMyOrderById(@PathVariable id: Long): MaintenanceOrderDetailResponse =
        maintenanceOrderService.getMyOrderById(id)

    @PatchMapping("/{id}/progress")
    fun updateProgress(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateMaintenanceProgressRequest
    ): MaintenanceOrderDetailResponse =
        maintenanceOrderService.updateProgressForAssigned(id, request)

    @PatchMapping("/{id}/resolve")
    fun resolveOrder(
        @PathVariable id: Long,
        @Valid @RequestBody request: ResolveMaintenanceOrderRequest
    ): MaintenanceOrderDetailResponse =
        maintenanceOrderService.resolveForAssigned(id, request)
}
