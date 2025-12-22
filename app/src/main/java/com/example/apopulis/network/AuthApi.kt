package com.example.apopulis.network

import com.example.apopulis.model.LoginRequest
import com.example.apopulis.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/users/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}
