package com.example.apopulis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apopulis.repository.NewsRepository

class MapViewModelFactory(
    private val repository: NewsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
