package com.cyclix.cyclix_api.user.controller

import com.cyclix.cyclix_api.user.dto.AssignUserRoleRequest
import com.cyclix.cyclix_api.user.dto.UpdateUserStatusRequest
import com.cyclix.cyclix_api.user.dto.UserResponse
import com.cyclix.cyclix_api.user.service.UserService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/get/user", "/get/user")
@PreAuthorize("hasRole('ADMIN')")
class UserController(
    private val userService: UserService
) {
    @GetMapping
    fun getUsers(): List<UserResponse> =
        userService.getUsers()

    @PatchMapping("/{userId}/status")
    fun updateStatus(
        @PathVariable userId: Long,
        @Valid @RequestBody request: UpdateUserStatusRequest
    ): UserResponse =
        userService.updateUserStatus(userId, request.status)

    @PatchMapping("/{userId}/role")
    fun assignRole(
        @PathVariable userId: Long,
        @Valid @RequestBody request: AssignUserRoleRequest
    ): UserResponse =
        userService.assignUserRole(userId, request.role)
}
