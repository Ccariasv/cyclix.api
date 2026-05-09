package com.cyclix.cyclix_api.trip.controller

import com.cyclix.cyclix_api.trip.dto.TripResponse
import com.cyclix.cyclix_api.trip.service.TripService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/trips")
class AdminTripController(
    private val tripService: TripService
) {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllTrips(): List<TripResponse> =
        tripService.getAllTripsForAdmin()

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getTripById(@PathVariable id: Long): TripResponse =
        tripService.getTripByIdForAdmin(id)

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    fun cancelTrip(@PathVariable id: Long): TripResponse =
        tripService.cancelTripForAdmin(id)
}