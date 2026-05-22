package com.cyclix.cyclix_api.user.service

import com.cyclix.cyclix_api.user.RoleRepository
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import com.cyclix.cyclix_api.user.UserStatusRepository
import com.cyclix.cyclix_api.user.dto.UserProfileResponse
import com.cyclix.cyclix_api.user.dto.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userStatusRepository: UserStatusRepository
) {
    @Transactional(readOnly = true)
    fun getUsers(): List<UserResponse> =
        userRepository.findAllByOrderByIdAsc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getMyProfile(): UserProfileResponse = getCurrentUser().toProfileResponse()

    @Transactional
    fun updateUserStatus(userId: Long, statusName: String): UserResponse {
        val user = findUserOrThrow(userId)
        val normalizedStatus = statusName.trim().uppercase()
        val status = userStatusRepository.findByName(normalizedStatus)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Estado no encontrado: $normalizedStatus")
            }

        user.status = status
        return user.toResponse()
    }

    @Transactional
    fun assignUserRole(userId: Long, roleName: String): UserResponse {
        val user = findUserOrThrow(userId)
        val normalizedRole = roleName.trim().uppercase()
        val role = roleRepository.findByName(normalizedRole)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado: $normalizedRole")
            }

        user.role = role
        return user.toResponse()
    }

    private fun findUserOrThrow(userId: Long): User =
        userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: $userId")
        }

    private fun getCurrentUser(): User {
        val principalEmail = SecurityContextHolder.getContext().authentication?.name?.trim()?.lowercase()
        if (principalEmail.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")
        }

        return userRepository.findByEmail(principalEmail)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")
            }
    }

    private fun User.toProfileResponse(): UserProfileResponse {
        val fullName = listOfNotNull(firstName.trim(), lastName?.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return UserProfileResponse(
            id = id,
            email = email,
            fullName = fullName,
            role = role.name,
            status = status.name
        )
    }

    private fun User.toResponse(): UserResponse =
        UserResponse(
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            role = role.name,
            status = status.name,
            emailVerified = emailVerified,
            lastLoginAt = lastLoginAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
