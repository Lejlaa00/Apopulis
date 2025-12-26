package com.example.apopulis.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Location(
    val _id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
) : Parcelable
