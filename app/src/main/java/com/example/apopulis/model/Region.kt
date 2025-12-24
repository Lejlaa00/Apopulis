package com.example.apopulis.model

import com.google.android.gms.maps.model.LatLng

data class Region(
    val id: String,
    val name: String,
    val polygonCoordinates: List<LatLng>
)

