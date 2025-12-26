package com.example.apopulis.repository

import com.example.apopulis.network.CategoryApi

class CategoryRepository(private val api: CategoryApi) {
    suspend fun getCategories() = api.getCategories()
}
