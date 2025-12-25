package com.example.apopulis.network

import com.example.apopulis.model.LoginRequest
import com.example.apopulis.model.LoginResponse
import com.example.apopulis.model.RegisterRequest
import com.example.apopulis.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("users/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("users/register")
    suspend fun register(
        @Body request: RegisterRequest
    ):Response<RegisterResponse>
}
