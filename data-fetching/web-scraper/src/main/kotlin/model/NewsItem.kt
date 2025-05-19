package model

import java.time.LocalDateTime

data class NewsItem(
    val heading: String,
    val content: String,
    val author: String?,
    val source: String,
    val url: String,
    val publishedAt: LocalDateTime,
    val imageUrl: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList()
)
