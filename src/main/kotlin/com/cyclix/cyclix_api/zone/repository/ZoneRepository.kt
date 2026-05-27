package com.cyclix.cyclix_api.zone.repository

import com.cyclix.cyclix_api.zone.entity.Zone
import org.springframework.data.jpa.repository.JpaRepository

interface ZoneRepository : JpaRepository<Zone, Long> {
    fun findAllByOrderByNameAsc(): List<Zone>

    fun findAllByActiveTrueOrderByNameAsc(): List<Zone>
}
