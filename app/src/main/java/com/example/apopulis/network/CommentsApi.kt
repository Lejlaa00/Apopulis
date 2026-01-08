package com.example.apopulis.network

import com.example.apopulis.model.Comment
import com.example.apopulis.model.CommentsResponse
import com.example.apopulis.model.CreateCommentRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CommentsApi {

    @GET("comments/news/{newsItemId}")
    suspend fun getComments(@Path("newsItemId") newsItemId: String): CommentsResponse

    @POST("comments/news/{newsItemId}")
    suspend fun createComment(
        @Path("newsItemId") newsItemId: String,
        @Body body: CreateCommentRequest
    ): Comment

}
