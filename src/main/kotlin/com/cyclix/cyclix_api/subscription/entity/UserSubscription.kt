package com.cyclix.cyclix_api.subscription.entity

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
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_subscriptions")
class UserSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    var plan: SubscriptionPlan,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: UserSubscriptionStatus = UserSubscriptionStatus.ACTIVE,

    @Column(name = "starts_at", nullable = false)
    var startsAt: LocalDateTime,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "included_minutes", nullable = false)
    var includedMinutes: Int,

    @Column(name = "consumed_minutes", nullable = false)
    var consumedMinutes: Int = 0,

    @Column(name = "remaining_minutes", nullable = false)
    var remainingMinutes: Int,

    @Column(name = "auto_renew", nullable = false)
    var autoRenew: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class UserSubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
}
