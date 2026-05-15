package com.cyclix.cyclix_api.trip.repository

import com.cyclix.cyclix_api.trip.entity.Trip
import com.cyclix.cyclix_api.trip.entity.TripStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TripRepository : JpaRepository<Trip, Long> {
    fun findAllByUserIdOrderByStartedAtDesc(userId: Long): List<Trip>

    fun findByIdAndUserId(id: Long, userId: Long): Optional<Trip>

    fun findAllByOrderByStartedAtDesc(): List<Trip>

    fun existsByUserIdAndStatus(userId: Long, status: TripStatus): Boolean
}