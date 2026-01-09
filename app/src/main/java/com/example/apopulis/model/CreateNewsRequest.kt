package com.example.apopulis.model

data class CreateNewsRequest(
    val title: String,
    val summary: String?,
    val content: String,
    val category: String,
    val location: String,
    val source: String = "User",
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val author: String? = null
)

