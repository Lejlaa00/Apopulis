package com.example.apopulis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apopulis.R
import com.example.apopulis.network.RetrofitInstance
import com.example.apopulis.repository.NewsRepository
import com.example.apopulis.viewmodel.MapViewModel
import com.example.apopulis.viewmodel.MapViewModelFactory

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var viewModel: MapViewModel

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

        val repository = NewsRepository(
            RetrofitInstance.newsApi
        )

        val factory = MapViewModelFactory(repository)

        viewModel = ViewModelProvider(this, factory)
            .get(MapViewModel::class.java)

        // Test call
        viewModel.loadNews(null)
        viewModel.news.observe(viewLifecycleOwner) { newsList ->
            println("NEWS COUNT = ${newsList.size}")
            newsList.forEach {
                println("NEWS: ${it.title}")
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val slovenia = LatLng(46.1512, 14.9955)
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(slovenia, 7f)
        )
    }
}

