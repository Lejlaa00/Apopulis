package org.example.model

import java.time.LocalDateTime

data class NewsItem(
    val heading: String,
    val content: String,
    val author: String?,
    val publishedAt: LocalDateTime,
    val source: String,
    val category: String? = null,
    val url: String,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList()
)
