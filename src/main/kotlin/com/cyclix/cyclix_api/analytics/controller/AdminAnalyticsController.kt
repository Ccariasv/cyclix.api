package com.cyclix.cyclix_api.analytics.controller

import com.cyclix.cyclix_api.analytics.dto.AnalyticsPeriod
import com.cyclix.cyclix_api.analytics.dto.BikeAnalyticsResponse
import com.cyclix.cyclix_api.analytics.dto.StationAnalyticsResponse
import com.cyclix.cyclix_api.analytics.dto.UserAnalyticsResponse
import com.cyclix.cyclix_api.analytics.service.AdminAnalyticsService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
class AdminAnalyticsController(
    private val adminAnalyticsService: AdminAnalyticsService
) {
    @GetMapping("/bicycles", "/bicicletas")
    fun getBicycleAnalytics(
        @RequestParam(defaultValue = "30") days: Int,
        @RequestParam(defaultValue = "DAY") period: AnalyticsPeriod
    ): BikeAnalyticsResponse =
        adminAnalyticsService.getBikeAnalytics(days, period)

    @GetMapping("/users", "/usuarios")
    fun getUserAnalytics(
        @RequestParam(defaultValue = "30") days: Int,
        @RequestParam(defaultValue = "DAY") period: AnalyticsPeriod
    ): UserAnalyticsResponse =
        adminAnalyticsService.getUserAnalytics(days, period)

    @GetMapping("/stations", "/estaciones")
    fun getStationAnalytics(
        @RequestParam(defaultValue = "30") days: Int,
        @RequestParam(defaultValue = "DAY") period: AnalyticsPeriod,
        @RequestParam(required = false) stationId: Long?,
        @RequestParam(required = false) puestoId: Long?
    ): StationAnalyticsResponse =
        adminAnalyticsService.getStationAnalytics(days, period, stationId ?: puestoId)
}
