package com.cyclix.cyclix_api.trip.service

import com.cyclix.cyclix_api.trip.dto.CreateTripRequest
import com.cyclix.cyclix_api.trip.dto.FinishTripRequest
import com.cyclix.cyclix_api.trip.dto.TripHistoryResponse
import com.cyclix.cyclix_api.trip.dto.TripResponse
import com.cyclix.cyclix_api.trip.entity.Trip
import com.cyclix.cyclix_api.trip.entity.TripStatus
import com.cyclix.cyclix_api.trip.repository.TripRepository
import com.cyclix.cyclix_api.pricing.service.PricingService
import com.cyclix.cyclix_api.subscription.service.SubscriptionService
import com.cyclix.cyclix_api.wallet.service.WalletService
import com.cyclix.cyclix_api.audit.service.AuditService
import com.cyclix.cyclix_api.bicycle.model.EstadoBicicleta
import com.cyclix.cyclix_api.bicycle.repository.BicicletaRepository
import com.cyclix.cyclix_api.device.service.DeviceCommandPublisher
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
    private val userRepository: UserRepository,
    private val bicicletaRepository: BicicletaRepository,
    private val pricingService: PricingService,
    private val subscriptionService: SubscriptionService,
    private val walletService: WalletService,
    private val auditService: AuditService,
    private val deviceCommandPublisher: DeviceCommandPublisher
) {
    @Transactional
    fun createTrip(request: CreateTripRequest): TripResponse {
        val currentUser = getCurrentUser()
        val bikeId = request.bikeId ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El ID de la bicicleta es obligatorio"
        )

        if (tripRepository.existsByUserIdAndStatus(currentUser.id, TripStatus.ACTIVE)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario ya tiene un viaje activo"
            )
        }

        val bicicleta = bicicletaRepository.findById(bikeId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.BAD_REQUEST, "La bicicleta indicada no existe")
            }

        if (bicicleta.estado != EstadoBicicleta.DISPONIBLE) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La bicicleta no está disponible para iniciar un viaje"
            )
        }

        val trip = Trip(
            user = currentUser,
            bikeId = bikeId,
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

        bicicletaRepository.save(
            bicicleta.copy(
                estado = EstadoBicicleta.EN_USO,
                updatedAt = LocalDateTime.now()
            )
        )

        val savedTrip = tripRepository.save(trip)
        deviceCommandPublisher.publishUnlockCommand(savedTrip)
        return savedTrip.toResponse()
    }

    @Transactional(readOnly = true)
    fun getMyTrips(): List<TripHistoryResponse> {
        val currentUser = getCurrentUser()

        return tripRepository.findAllByUserIdOrderByStartedAtDesc(currentUser.id)
            .map { it.toHistoryResponse() }
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
        val tripDurationMinutes = kotlin.math.ceil((trip.durationSeconds ?: 0L).toDouble() / 60.0).toInt().coerceAtLeast(0)

        val subscriptionResult = subscriptionService.consumeMinutes(currentUser.id, tripDurationMinutes, endedAt)
        val pricingCalculation = pricingService.calculate(endedAt, subscriptionResult.billableMinutes)
        val chargedAmount = walletService.debitForTrip(currentUser.id, trip.id, pricingCalculation.totalAmount)

        trip.subscriptionApplied = subscriptionResult.minutesCovered > 0
        trip.subscriptionMinutesCovered = subscriptionResult.minutesCovered
        trip.billableMinutes = subscriptionResult.billableMinutes
        trip.pricingRuleId = pricingCalculation.ruleId
        trip.pricingRuleName = pricingCalculation.ruleName
        trip.baseFareApplied = pricingCalculation.baseFareApplied
        trip.includedMinutesApplied = pricingCalculation.includedMinutesApplied
        trip.extraFarePerBlockApplied = pricingCalculation.extraFarePerBlockApplied
        trip.extraBlockMinutesApplied = pricingCalculation.extraBlockMinutesApplied
        trip.extraAmount = pricingCalculation.extraAmount
        trip.totalAmount = pricingCalculation.totalAmount
        trip.walletChargedAmount = chargedAmount

        val bicicleta = bicicletaRepository.findById(trip.bikeId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.BAD_REQUEST, "La bicicleta del viaje no existe")
            }

        bicicletaRepository.save(
            bicicleta.copy(
                estado = EstadoBicicleta.DISPONIBLE,
                updatedAt = LocalDateTime.now()
            )
        )

        auditService.log(
            eventType = "TRIP_FINISHED",
            entityType = "trip",
            entityId = trip.id,
            details = "Viaje finalizado. Total=${trip.totalAmount}, CobroWallet=${trip.walletChargedAmount}",
            user = currentUser
        )

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

        val bicicleta = bicicletaRepository.findById(trip.bikeId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.BAD_REQUEST, "La bicicleta del viaje no existe")
            }

        bicicletaRepository.save(
            bicicleta.copy(
                estado = EstadoBicicleta.DISPONIBLE,
                updatedAt = LocalDateTime.now()
            )
        )

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

    private fun Trip.toHistoryResponse(): TripHistoryResponse =
        TripHistoryResponse(
            id = id,
            bikeId = bikeId,
            startedAt = startedAt,
            endedAt = endedAt,
            status = status,
            cost = totalAmount,
            durationSeconds = durationSeconds
        )

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
            pricingRuleId = pricingRuleId,
            pricingRuleName = pricingRuleName,
            subscriptionApplied = subscriptionApplied,
            subscriptionMinutesCovered = subscriptionMinutesCovered,
            billableMinutes = billableMinutes,
            baseFareApplied = baseFareApplied,
            includedMinutesApplied = includedMinutesApplied,
            extraFarePerBlockApplied = extraFarePerBlockApplied,
            extraBlockMinutesApplied = extraBlockMinutesApplied,
            extraAmount = extraAmount,
            totalAmount = totalAmount,
            walletChargedAmount = walletChargedAmount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
