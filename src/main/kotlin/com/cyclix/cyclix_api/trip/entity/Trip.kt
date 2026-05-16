package com.cyclix.cyclix_api.trip.entity

import com.cyclix.cyclix_api.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "trips")
class Trip(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "bike_id", nullable = false)
    var bikeId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: TripStatus = TripStatus.ACTIVE,

    @Column(name = "start_latitude", nullable = false, precision = 10, scale = 7)
    var startLatitude: BigDecimal,

    @Column(name = "start_longitude", nullable = false, precision = 10, scale = 7)
    var startLongitude: BigDecimal,

    @Column(name = "end_latitude", precision = 10, scale = 7)
    var endLatitude: BigDecimal? = null,

    @Column(name = "end_longitude", precision = 10, scale = 7)
    var endLongitude: BigDecimal? = null,

    @Column(name = "started_at", nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,

    @Column(name = "distance_km", precision = 10, scale = 2)
    var distanceKm: BigDecimal? = null,

    @Column(name = "duration_seconds")
    var durationSeconds: Long? = null,

    @Column(name = "pricing_rule_id")
    var pricingRuleId: Long? = null,

    @Column(name = "pricing_rule_name", length = 120)
    var pricingRuleName: String? = null,

    @Column(name = "subscription_applied", nullable = false)
    var subscriptionApplied: Boolean = false,

    @Column(name = "subscription_minutes_covered")
    var subscriptionMinutesCovered: Int? = null,

    @Column(name = "billable_minutes")
    var billableMinutes: Int? = null,

    @Column(name = "base_fare_applied", precision = 12, scale = 2)
    var baseFareApplied: BigDecimal? = null,

    @Column(name = "included_minutes_applied")
    var includedMinutesApplied: Int? = null,

    @Column(name = "extra_fare_per_block_applied", precision = 12, scale = 2)
    var extraFarePerBlockApplied: BigDecimal? = null,

    @Column(name = "extra_block_minutes_applied")
    var extraBlockMinutesApplied: Int? = null,

    @Column(name = "extra_amount", precision = 12, scale = 2)
    var extraAmount: BigDecimal? = null,

    @Column(name = "total_amount", precision = 12, scale = 2)
    var totalAmount: BigDecimal? = null,

    @Column(name = "wallet_charged_amount", precision = 12, scale = 2)
    var walletChargedAmount: BigDecimal? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
