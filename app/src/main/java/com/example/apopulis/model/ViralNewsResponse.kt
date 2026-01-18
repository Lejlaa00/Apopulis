package com.example.apopulis.model

data class ViralNewsResponse(
    val hasViralNews: Boolean,
    val count: Int,
    val viralNewsItems: List<ViralNewsItem>
)

data class ViralNewsItem(
    val _id: String,
    val title: String,
    val commentsCount: Int,
    val publishedAt: String
)
