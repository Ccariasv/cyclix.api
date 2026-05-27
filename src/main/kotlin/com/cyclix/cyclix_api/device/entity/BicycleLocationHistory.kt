package com.cyclix.cyclix_api.device.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "bicycle_location_history")
data class BicycleLocationHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "bike_id", nullable = false)
    val bikeId: Long,

    @Column(name = "trip_id")
    val tripId: Long? = null,

    @Column(name = "station_id")
    val stationId: Long? = null,

    @Column(name = "event_type", nullable = false, length = 40)
    val eventType: String,

    @Column(name = "source", nullable = false, length = 40)
    val source: String,

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    val latitude: BigDecimal,

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    val longitude: BigDecimal,

    @Column(name = "recorded_at", nullable = false)
    val recordedAt: LocalDateTime,

    @Column(name = "received_at", nullable = false)
    val receivedAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
