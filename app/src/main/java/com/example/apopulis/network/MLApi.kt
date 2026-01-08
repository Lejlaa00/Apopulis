package com.example.apopulis.network

import com.example.apopulis.model.MLPredictionResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MLApi {
    
    @Multipart
    @POST("ml/predict")
    suspend fun predictImage(
        @Part image: MultipartBody.Part
    ): Response<MLPredictionResponse>
}

