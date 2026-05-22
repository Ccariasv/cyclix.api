package com.cyclix.cyclix_api.user.controller

import com.cyclix.cyclix_api.user.dto.UserProfileResponse
import com.cyclix.cyclix_api.user.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/profile")
class ProfileController(
    private val userService: UserService
) {
    @GetMapping("/me")
    fun getMyProfile(): UserProfileResponse = userService.getMyProfile()
}
