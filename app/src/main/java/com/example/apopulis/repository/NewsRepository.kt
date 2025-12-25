package com.example.apopulis.repository

import android.util.Log
import com.example.apopulis.model.NewsItem
import com.example.apopulis.network.NewsApi

class NewsRepository(
    private val newsApi: NewsApi
) {
    suspend fun fetchNews(): List<NewsItem> {
        return newsApi.getNews().news
    }
}