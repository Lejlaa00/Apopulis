package com.example.apopulis.model

data class NewsResponse(
    val news: List<NewsItem>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int
)
