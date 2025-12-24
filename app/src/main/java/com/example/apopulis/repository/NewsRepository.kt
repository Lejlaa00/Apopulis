package com.example.apopulis.repository


import com.example.apopulis.model.NewsItem
import com.example.apopulis.network.NewsApi

class NewsRepository(
    private val newsApi: NewsApi
) {
    suspend fun fetchNews(locationId: String?): List<NewsItem> {
        return newsApi
            .getNews(locationId = locationId)
            .news
    }
}