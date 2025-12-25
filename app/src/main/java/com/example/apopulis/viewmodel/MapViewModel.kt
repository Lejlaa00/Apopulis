package com.example.apopulis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apopulis.model.NewsItem
import com.example.apopulis.repository.NewsRepository
import kotlinx.coroutines.launch


class MapViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _news = MutableLiveData<List<NewsItem>>()
    val news: LiveData<List<NewsItem>> = _news

    fun loadNews() {
        viewModelScope.launch {
            try {
                _news.value = repository.fetchNews()
            } catch (e: Exception) {
                _news.value = emptyList()
            }
        }
    }
}
