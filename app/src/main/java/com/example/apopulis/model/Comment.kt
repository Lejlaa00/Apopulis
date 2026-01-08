package com.example.apopulis.model

import com.google.gson.annotations.SerializedName

data class Comment(
    @SerializedName("_id") val id: String,
    val content: String,
    val parentCommentId: String?,
    val isSimulated: Boolean?,
    val simulationId: String?,
    val createdAt: String,
    val updatedAt: String,
    val newsItemId: String,
    val userId: UserMini? = null,
    val replies: List<Comment>? = emptyList()
)

data class UserMini(
    @SerializedName("_id") val id: String,
    val username: String
)
