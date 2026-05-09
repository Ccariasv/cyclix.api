package com.cyclix.cyclix_api.trip.controller

import com.cyclix.cyclix_api.trip.dto.CreateTripRequest
import com.cyclix.cyclix_api.trip.dto.FinishTripRequest
import com.cyclix.cyclix_api.trip.dto.TripResponse
import com.cyclix.cyclix_api.trip.service.TripService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/trips")
class TripController(
    private val tripService: TripService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun createTrip(
        @Valid @RequestBody request: CreateTripRequest
    ): TripResponse =
        tripService.createTrip(request)

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getMyTrips(): List<TripResponse> =
        tripService.getMyTrips()

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun getMyTripById(@PathVariable id: Long): TripResponse =
        tripService.getMyTripById(id)

    @PutMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun finishMyTrip(
        @PathVariable id: Long,
        @Valid @RequestBody request: FinishTripRequest
    ): TripResponse =
        tripService.finishMyTrip(id, request)
}