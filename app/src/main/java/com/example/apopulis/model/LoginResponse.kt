package com.example.apopulis.model

data class LoginResponse(
    val success: Boolean,
    val token: String,
    val user: LoginUserDto
)

data class LoginUserDto(
    val id: String,
    val username: String,
    val email: String,
    val avatarColor: String?,
    val role: String
)
