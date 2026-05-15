package com.cyclix.cyclix_api.trip.service

import com.cyclix.cyclix_api.trip.dto.CreateTripRequest
import com.cyclix.cyclix_api.trip.dto.FinishTripRequest
import com.cyclix.cyclix_api.trip.dto.TripResponse
import com.cyclix.cyclix_api.trip.entity.Trip
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.trip.repository.TripRepository
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.LocalDateTime

@Service
class TripService(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun createTrip(request: CreateTripRequest): TripResponse {
        val currentUser = getCurrentUser()

        if (tripRepository.existsByUserIdAndStatus(currentUser.id, TripStatus.ACTIVE)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario ya tiene un viaje activo"
            )
        }

        val trip = Trip(
            user = currentUser,
            bikeId = request.bikeId ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El ID de la bicicleta es obligatorio"
            ),
            status = TripStatus.ACTIVE,
            startLatitude = request.startLatitude ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La latitud inicial es obligatoria"
            ),
            startLongitude = request.startLongitude ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La longitud inicial es obligatoria"
            )
        )

        return tripRepository.save(trip).toResponse()
    }

    @Transactional(readOnly = true)
    fun getMyTrips(): List<TripResponse> {
        val currentUser = getCurrentUser()

        return tripRepository.findAllByUserIdOrderByStartedAtDesc(currentUser.id)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getMyTripById(tripId: Long): TripResponse {
        val currentUser = getCurrentUser()

        return tripRepository.findByIdAndUserId(tripId, currentUser.id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado: $tripId")
            }
            .toResponse()
    }

    @Transactional
    fun finishMyTrip(tripId: Long, request: FinishTripRequest): TripResponse {
        val currentUser = getCurrentUser()

        val trip = tripRepository.findByIdAndUserId(tripId, currentUser.id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado: $tripId")
            }

        if (trip.status != TripStatus.ACTIVE) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se pueden finalizar viajes activos"
            )
        }

        val endedAt = LocalDateTime.now()

        trip.status = TripStatus.COMPLETED
        trip.endLatitude = request.endLatitude ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La latitud final es obligatoria"
        )
        trip.endLongitude = request.endLongitude ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La longitud final es obligatoria"
        )
        trip.endedAt = endedAt
        trip.distanceKm = request.distanceKm
        trip.durationSeconds = Duration.between(trip.startedAt, endedAt).seconds

        return trip.toResponse()
    }

    @Transactional(readOnly = true)
    fun getAllTripsForAdmin(): List<TripResponse> =
        tripRepository.findAllByOrderByStartedAtDesc()
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getTripByIdForAdmin(tripId: Long): TripResponse =
        findTripOrThrow(tripId).toResponse()

    @Transactional
    fun cancelTripForAdmin(tripId: Long): TripResponse {
        val trip = findTripOrThrow(tripId)

        if (trip.status != TripStatus.ACTIVE) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se pueden cancelar viajes activos"
            )
        }

        val endedAt = LocalDateTime.now()

        trip.status = TripStatus.CANCELLED
        trip.endedAt = endedAt
        trip.durationSeconds = Duration.between(trip.startedAt, endedAt).seconds

        return trip.toResponse()
    }

    private fun findTripOrThrow(tripId: Long): Trip =
        tripRepository.findById(tripId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado: $tripId")
            }

    private fun getCurrentUser(): User {
        val principalEmail = SecurityContextHolder.getContext().authentication?.name?.trim()?.lowercase()

        if (principalEmail.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")
        }

        return userRepository.findByEmail(principalEmail)
            .orElseThrow {
                ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado")
            }
    }

    private fun Trip.toResponse(): TripResponse =
        TripResponse(
            id = id,
            userId = user.id,
            bikeId = bikeId,
            status = status,
            startLatitude = startLatitude,
            startLongitude = startLongitude,
            endLatitude = endLatitude,
            endLongitude = endLongitude,
            startedAt = startedAt,
            endedAt = endedAt,
            distanceKm = distanceKm,
            durationSeconds = durationSeconds,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}