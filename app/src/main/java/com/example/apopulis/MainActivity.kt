package com.example.apopulis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.apopulis.network.RetrofitInstance
import com.example.apopulis.repository.CategoryRepository
import com.example.apopulis.repository.NewsRepository
import com.example.apopulis.viewmodel.MapViewModel
import com.example.apopulis.viewmodel.MapViewModelFactory

class MainActivity : AppCompatActivity() {

    val mapViewModelFactory: ViewModelProvider.Factory by lazy {
        val newsRepository = NewsRepository(RetrofitInstance.newsApi)
        val categoryRepository = CategoryRepository(RetrofitInstance.categoryApi)
        MapViewModelFactory(
            newsRepository = newsRepository,
            categoryRepository = categoryRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitInstance.init(this)
        setContentView(R.layout.activity_main)
    }
}
