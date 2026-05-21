package com.cyclix.cyclix_api.auth.service

import com.cyclix.cyclix_api.auth.dto.AuthResponse
import com.cyclix.cyclix_api.auth.dto.LoginRequest
import com.cyclix.cyclix_api.auth.dto.RegisterRequest
import com.cyclix.cyclix_api.user.RoleRepository
import com.cyclix.cyclix_api.user.User
import com.cyclix.cyclix_api.user.UserRepository
import com.cyclix.cyclix_api.user.UserStatusRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userStatusRepository: UserStatusRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    @Transactional
    fun register(request: RegisterRequest): Long {
        val normalizedEmail = request.email.trim().lowercase()
        val rawPassword = request.password ?: throw IllegalArgumentException("La contraseña es obligatoria")

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw IllegalArgumentException("El correo ya está registrado")
        }

        val userRole = roleRepository.findByName("USER")
            .orElseThrow { IllegalStateException("No existe el rol USER") }

        val activeStatus = userStatusRepository.findByName("ACTIVE")
            .orElseThrow { IllegalStateException("No existe el estado ACTIVE") }

        val user = User(
            firstName = request.firstName.trim(),
            lastName = request.lastName?.trim()?.takeIf { it.isNotBlank() },
            email = normalizedEmail,
            phone = request.phone?.trim()?.takeIf { it.isNotBlank() },
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
            role = userRole,
            status = activeStatus
        )

        return userRepository.save(user).id
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()
        val user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow { IllegalArgumentException("Credenciales inválidas") }

        if (user.status.name.trim().uppercase() != "ACTIVE") {
            throw IllegalArgumentException("La cuenta está inactiva")
        }

        val rawPassword = request.password ?: throw IllegalArgumentException("La contraseña es obligatoria")
        if (!passwordEncoder.matches(rawPassword, user.passwordHash)) {
            throw IllegalArgumentException("Credenciales inválidas")
        }

        user.lastLoginAt = LocalDateTime.now()
        val token = jwtService.generateToken(user)

        return AuthResponse(
            token = token,
            expiresIn = jwtService.expirationSeconds,
            userId = user.id,
            email = user.email
        )
    }
}
