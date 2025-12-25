package com.example.apopulis.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.apopulis.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object MarkerIconGenerator {

    private var cachedMarker: BitmapDescriptor? = null
    private var cachedDensity: Float = -1f

    fun createNewsMarker(context: Context): BitmapDescriptor {
        val density = context.resources.displayMetrics.density

        if (cachedMarker != null && cachedDensity == density) {
            return cachedMarker!!
        }

        val originalBitmap = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_pin
        )

        val targetWidthDp = 28f
        val targetHeightDp = 36f

        val targetWidthPx = (targetWidthDp * density).toInt()
        val targetHeightPx = (targetHeightDp * density).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            targetWidthPx,
            targetHeightPx,
            true
        )

        val descriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap)

        cachedMarker = descriptor
        cachedDensity = density

        return descriptor
    }
}
