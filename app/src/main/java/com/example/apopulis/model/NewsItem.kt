package com.example.apopulis.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NewsItem(
    val _id: String,
    val title: String,
    val summary: String?,
    val content: String?,
    val publishedAt: String?,
    val imageUrl: String?,
    val views: Int,
    val likes: Int,
    val dislikes: Int,
    val commentsCount: Int,
    val bookmarks: Int,
    val locationId: Location?,
    val categoryId: Category?,
    val createdAt: String?,
    val updatedAt: String?
) : Parcelable
