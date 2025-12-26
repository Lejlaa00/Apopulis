package com.example.apopulis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apopulis.model.Category
import com.example.apopulis.model.NewsItem
import com.example.apopulis.repository.NewsRepository
import com.example.apopulis.repository.CategoryRepository
import kotlinx.coroutines.launch


class MapViewModel(
    private val newsRepository: NewsRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _news = MutableLiveData<List<NewsItem>>()
    val news: LiveData<List<NewsItem>> = _news
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    fun loadNews() {
        viewModelScope.launch {
            try {
                _news.value = newsRepository.fetchNews()
            } catch (e: Exception) {
                _news.value = emptyList()
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = categoryRepository.getCategories()
        }
    }
}
