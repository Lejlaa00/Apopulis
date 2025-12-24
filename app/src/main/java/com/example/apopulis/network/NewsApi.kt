package com.example.apopulis.network

import com.example.apopulis.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {

    @GET("api/news")
    suspend fun getNews(
        @Query("location") locationId: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): NewsResponse
}