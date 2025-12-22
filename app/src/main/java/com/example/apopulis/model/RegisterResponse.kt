package com.example.apopulis.model


data class RegisterResponse(
    val msg: String,
    val token: String,
    val user: RegisterUserDto
)

data class RegisterUserDto(
    val id: String,
    val username: String,
    val email: String
)
