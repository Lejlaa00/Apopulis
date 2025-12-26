package com.example.apopulis.network

import com.example.apopulis.model.Category
import retrofit2.http.GET

interface CategoryApi {
    @GET("categories")
    suspend fun getCategories(): List<Category>
}
