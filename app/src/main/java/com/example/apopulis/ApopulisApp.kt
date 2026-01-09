package com.example.apopulis

import android.app.Application
import com.example.apopulis.network.RetrofitInstance

class ApopulisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitInstance.init(this)
    }
}
