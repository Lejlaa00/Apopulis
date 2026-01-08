package com.example.apopulis.model

data class CreateCommentRequest(
    val content: String,
    val parentCommentId: String? = null,
    val isSimulated: Boolean = false,
    val simulationId: String? = null
)
