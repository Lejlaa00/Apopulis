package com.example.apopulis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import com.example.apopulis.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng


class MapFragment : Fragment(), OnMapReadyCallback {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = SupportMapFragment.newInstance()

        childFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val slovenia = LatLng(46.1512, 14.9955)
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(slovenia, 7f)
        )
    }
}

