package com.example.apopulis.network

import com.example.apopulis.model.CreateNewsRequest
import com.example.apopulis.model.NewsItem
import com.example.apopulis.model.NewsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface NewsApi {

    @GET("news")
    suspend fun getNews(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 300
    ): NewsResponse
    
    @POST("news")
    suspend fun createNews(
        @Body request: CreateNewsRequest
    ): Response<NewsItem>
    
    @Multipart
    @POST("news/upload")
    suspend fun uploadNewsWithImage(
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("category") category: RequestBody,
        @Part("location") location: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<NewsItem>
}