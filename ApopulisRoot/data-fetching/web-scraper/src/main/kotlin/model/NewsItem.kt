package model

import java.time.LocalDateTime
import com.google.gson.annotations.SerializedName


data class NewsItem(
    @SerializedName("_id")
    var id: String? = null,
    val title: String,
    val content: String,
    val author: String?,
    val source: String?,
    val url: String,
    val publishedAt: LocalDateTime,
    val imageUrl: String? = null,
    val category: String? = null,
    val location: String? = null,
    val tags: List<String>? = emptyList(),
) 