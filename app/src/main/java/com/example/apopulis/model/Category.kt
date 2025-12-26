package com.example.apopulis.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category (
    val _id: String,
    val name: String
) : Parcelable