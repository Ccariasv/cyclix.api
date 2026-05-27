package com.cyclix.cyclix_api.device.repository

import com.cyclix.cyclix_api.device.entity.BicycleLocationHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BicycleLocationHistoryRepository : JpaRepository<BicycleLocationHistory, Long> {
    fun findFirstByBikeIdOrderByRecordedAtDesc(bikeId: Long): BicycleLocationHistory?
}
