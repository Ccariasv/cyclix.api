package com.cyclix.cyclix_api.maintenance.controller

import com.cyclix.cyclix_api.maintenance.dto.AssignMaintenanceOrderRequest
import com.cyclix.cyclix_api.maintenance.dto.CreateMaintenanceFromTicketRequest
import com.cyclix.cyclix_api.maintenance.dto.CreateMaintenanceOrderRequest
import com.cyclix.cyclix_api.maintenance.dto.MaintenanceOrderDetailResponse
import com.cyclix.cyclix_api.maintenance.dto.MaintenanceOrderSummaryResponse
import com.cyclix.cyclix_api.maintenance.dto.ResolveMaintenanceOrderRequest
import com.cyclix.cyclix_api.maintenance.dto.UpdateMaintenanceProgressRequest
import com.cyclix.cyclix_api.maintenance.service.MaintenanceOrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/maintenance/orders")
@PreAuthorize("hasRole('ADMIN')")
class AdminMaintenanceOrderController(
    private val maintenanceOrderService: MaintenanceOrderService
) {
    @GetMapping
    fun getAllOrders(): List<MaintenanceOrderSummaryResponse> =
        maintenanceOrderService.getAllForAdmin()

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: Long): MaintenanceOrderDetailResponse =
        maintenanceOrderService.getByIdForAdmin(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @Valid @RequestBody request: CreateMaintenanceOrderRequest
    ): MaintenanceOrderDetailResponse =
        maintenanceOrderService.createManual(request)

    @PostMapping("/from-ticket/{ticketId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrderFromTicket(
        @PathVariable ticketId: Long,
        @Valid @RequestBody request: CreateMaintenanceFromTicketRequest
    ): MaintenanceOrderDetailResponse =
        maintenanceOrderService.createFromTicket(ticketId, request)

    @PutMapping("/{id}/assign")
    fun assignOrder(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignMaintenanceOrderRequest
    ): MaintenanceOrderDetailResponse =
        maintenanceOrderService.assignForAdmin(id, request)

    @PatchMapping("/{id}/progress")
    fun updateProgress(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateMaintenanceProgressRequest
    ): MaintenanceOrderDetailResponse =
        maintenanceOrderService.updateProgressForAdmin(id, request)

    @PatchMapping("/{id}/resolve")
    fun resolveOrder(
        @PathVariable id: Long,
        @Valid @RequestBody request: ResolveMaintenanceOrderRequest
    ): MaintenanceOrderDetailResponse =
        maintenanceOrderService.resolveForAdmin(id, request)
}
