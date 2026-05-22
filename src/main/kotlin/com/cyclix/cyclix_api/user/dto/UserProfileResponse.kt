package com.cyclix.cyclix_api.user.dto

data class UserProfileResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String,
    val status: String
)
