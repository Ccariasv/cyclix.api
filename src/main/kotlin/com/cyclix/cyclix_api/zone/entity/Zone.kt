package com.cyclix.cyclix_api.zone.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "zones")
class Zone(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(name = "center_latitude", nullable = false, precision = 10, scale = 7)
    var centerLatitude: BigDecimal,

    @Column(name = "center_longitude", nullable = false, precision = 10, scale = 7)
    var centerLongitude: BigDecimal,

    @Column(name = "radius_meters", nullable = false)
    var radiusMeters: Int,

    @Column(nullable = false)
    var active: Boolean = true,

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
