package com.example.apopulis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.apopulis.network.RetrofitInstance

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitInstance.init(this)
        setContentView(R.layout.activity_main)
    }
}
