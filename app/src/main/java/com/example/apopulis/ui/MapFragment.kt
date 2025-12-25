package com.example.apopulis.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.data.Feature
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonMultiPolygon
import com.google.maps.android.data.geojson.GeoJsonPolygon
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.apopulis.model.NewsItem
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.PolyUtil

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var viewModel: MapViewModel
    private lateinit var googleMap: GoogleMap

    private var geoJsonLayer: GeoJsonLayer? = null
    private var selectedFeature: GeoJsonFeature? = null
    private var selectedRegionId: String? = null
    private var isMapLoaded = false

    private var newsList: List<NewsItem> = emptyList()
    private val newsMarkers = mutableListOf<Marker>()


    private val boundsCache = mutableMapOf<String, LatLngBounds>()

    private val regionColors = mutableMapOf<String, RegionColors>()

    private val defaultFillAlpha = 100
    private val selectedFillAlpha = 200
    private val defaultStrokeWidth = 4f
    private val selectedStrokeWidth = 6f

    private val purpleHueBase = 270f
    private val hueRange = -18f..18f
    private val saturationRange = 0.65f..0.95f
    private val brightnessRange = 0.70f..0.95f


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

        val repository = NewsRepository(RetrofitInstance.newsApi)
        val factory = MapViewModelFactory(repository)

        viewModel = ViewModelProvider(this, factory)
            .get(MapViewModel::class.java)

        viewModel.news.observe(viewLifecycleOwner) { list ->
            newsList = list

            Log.e("PIN_DEBUG", "Observer received ${list.size} news")

            if (isMapLoaded) {
                redrawPins(googleMap.cameraPosition.zoom)
            }
        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.setOnMapLoadedCallback {
            isMapLoaded = true
            loadGeoJsonLayer()
            showAllSlovenia()
        }

        googleMap.setOnCameraIdleListener {
            redrawPins(googleMap.cameraPosition.zoom)
        }

        googleMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }

        viewModel.loadNews()
    }

    // Pins
    private fun redrawPins(zoom: Float) {
        if (!isMapLoaded) return

        newsMarkers.forEach { it.remove() }
        newsMarkers.clear()

        val feature = selectedFeature

        Log.e(
            "PIN_DEBUG",
            "REDRAW news=${newsList.size}, regionSelected=${feature != null}"
        )

        newsList.forEach { news ->

            val loc = news.locationId ?: return@forEach
            if (loc.latitude == 0.0 && loc.longitude == 0.0) return@forEach

            val realPoint = LatLng(loc.latitude, loc.longitude)

            if (feature != null) {
                val inside = isPointInsideFeature(realPoint, feature)
                if (!inside) return@forEach
            }

            // Pin positon random in region
            val position = if (feature != null) {
                randomPointInsideFeature(feature)
            } else {
                realPoint
            }

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(news.title)
                    .snippet(loc.name)
                    .icon(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory
                            .defaultMarker(
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET
                            )
                    )
            )

            marker?.let { newsMarkers.add(it) }
        }

        Log.e("PIN_DEBUG", "DRAWN markers=${newsMarkers.size}")
    }

    private fun randomPointInsideFeature(feature: GeoJsonFeature): LatLng {
        val geometry = feature.geometry ?: error("No geometry")

        val bounds = calculateBounds(feature) ?: error("No bounds")

        val polygons: List<List<LatLng>> = when (geometry) {
            is GeoJsonPolygon -> {
                listOf(geometry.coordinates[0])
            }
            is GeoJsonMultiPolygon -> {
                geometry.polygons.map { it.coordinates[0] }
            }
            else -> emptyList()
        }

        repeat(300) {
            val point = randomPointInBounds(bounds)

            if (polygons.any { polygon ->
                    PolyUtil.containsLocation(point, polygon, true)
                }) {
                return point
            }
        }

        return bounds.center
    }

    private fun randomPointInBounds(bounds: LatLngBounds): LatLng {
        val lat = bounds.southwest.latitude +
                Math.random() * (bounds.northeast.latitude - bounds.southwest.latitude)

        val lng = bounds.southwest.longitude +
                Math.random() * (bounds.northeast.longitude - bounds.southwest.longitude)

        return LatLng(lat, lng)
    }

    private fun isPointInsideFeature(
        point: LatLng,
        feature: GeoJsonFeature
    ): Boolean {
        return when (val geometry = feature.geometry) {

            is GeoJsonPolygon -> {
                val polygon = geometry.coordinates[0]
                PolyUtil.containsLocation(point, polygon, true)
            }

            is GeoJsonMultiPolygon -> {
                geometry.polygons.any { polygon ->
                    val outerRing = polygon.coordinates[0]
                    PolyUtil.containsLocation(point, outerRing, true)
                }
            }

            else -> false
        }
    }


    //Geo JSON
    private fun loadGeoJsonLayer() {
        val map = googleMap ?: return

        lifecycleScope.launch {
            try {
                val layer = withContext(Dispatchers.IO) {
                    GeoJsonLayer(
                        map,
                        R.raw.sr_regions,
                        requireContext()
                    )
                }

                withContext(Dispatchers.Main) {
                    geoJsonLayer = layer.apply {
                        addLayerToMap()

                        val featuresList = features.toList()
                        featuresList.forEachIndexed { index, feature ->
                            val geoFeature = feature as? GeoJsonFeature ?: return@forEachIndexed

                            val regionIdObj = geoFeature.getProperty("SR_ID")
                            val regionId = when (regionIdObj) {
                                is String -> regionIdObj
                                is Number -> regionIdObj.toString()
                                else -> null
                            }

                            if (regionId != null) {
                                val regionColors = getRegionColors(regionId)

                                val style = GeoJsonPolygonStyle().apply {
                                    fillColor = regionColors.fillColor
                                    strokeColor = regionColors.strokeColor
                                    strokeWidth = defaultStrokeWidth
                                }

                                geoFeature.polygonStyle = style

                            }

                            if (index % 50 == 0 && index > 0) {
                                kotlinx.coroutines.yield()
                            }
                        }

                        setOnFeatureClickListener { feature: Feature ->
                            val geoFeature = feature as? GeoJsonFeature ?: return@setOnFeatureClickListener
                            handleFeatureClick(geoFeature)
                        }
                    }
                }
            } catch (e: OutOfMemoryError) {
                android.util.Log.e("MapFragment", "Out of memory loading GeoJSON. Consider simplifying the file.", e)
            } catch (e: Exception) {
                android.util.Log.e("MapFragment", "Error loading GeoJSON layer", e)
                e.printStackTrace()
            }
        }
    }

    private fun handleFeatureClick(feature: GeoJsonFeature) {

        if (selectedFeature === feature) {
            deselectRegion()
            showAllSlovenia()
            return
        }

        selectedFeature?.let { resetStyle(it) }

        selectedFeature = feature
        highlightFeature(feature)

        lifecycleScope.launch {
            val bounds = withContext(Dispatchers.Default) {
                calculateBounds(feature)
            }

            if (bounds != null) {
                withContext(Dispatchers.Main) {
                    if (isMapLoaded) {
                        try {
                            googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, 120)
                            )
                        } catch (e: IllegalStateException) {
                            // fallback — SIGURAN
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(bounds.center, 8f)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun highlightFeature(feature: GeoJsonFeature) {
        val regionId = feature.getProperty("SR_ID")?.toString() ?: return
        val colors = getRegionColors(regionId)

        feature.polygonStyle = GeoJsonPolygonStyle().apply {
            fillColor = colors.selectedFillColor
            strokeColor = colors.strokeColor
            strokeWidth = selectedStrokeWidth
        }
    }

    private fun resetStyle(feature: GeoJsonFeature) {
        val regionId = feature.getProperty("SR_ID")?.toString() ?: return
        val colors = getRegionColors(regionId)

        feature.polygonStyle = GeoJsonPolygonStyle().apply {
            fillColor = colors.fillColor
            strokeColor = colors.strokeColor
            strokeWidth = defaultStrokeWidth
        }
    }

    private fun getRegionColors(regionId: String): RegionColors {
        return regionColors.getOrPut(regionId) {
            generateRegionColors(regionId)
        }
    }

    private fun generateRegionColors(regionId: String): RegionColors {

        val hash = regionId.hashCode()

        val h1 = ((hash and 0x7FFFFFFF).toFloat() / Int.MAX_VALUE)
        val h2 = (((hash * 31) and 0x7FFFFFFF).toFloat() / Int.MAX_VALUE)
        val h3 = (((hash * 131) and 0x7FFFFFFF).toFloat() / Int.MAX_VALUE)

        val hueOffset =
            hueRange.start + (hueRange.endInclusive - hueRange.start) * h1
        val hue = purpleHueBase + hueOffset

        val saturation =
            saturationRange.start +
                    (saturationRange.endInclusive - saturationRange.start) * h2

        val brightness =
            brightnessRange.start +
                    (brightnessRange.endInclusive - brightnessRange.start) * h3

        val rgb = hsvToRgb(hue, saturation, brightness)

        val fillColor =
            Color.argb(defaultFillAlpha, rgb[0], rgb[1], rgb[2])

        val selectedFillColor =
            Color.argb(selectedFillAlpha, rgb[0], rgb[1], rgb[2])

        val strokeColor =
            Color.argb(
                255,
                (rgb[0] * 0.8f).toInt(),
                (rgb[1] * 0.8f).toInt(),
                (rgb[2] * 0.8f).toInt()
            )

        return RegionColors(
            fillColor = fillColor,
            selectedFillColor = selectedFillColor,
            strokeColor = strokeColor
        )
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60f) % 2f - 1))
        val m = v - c

        val (r, g, b) = when {
            h < 60f -> floatArrayOf(c, x, 0f)
            h < 120f -> floatArrayOf(x, c, 0f)
            h < 180f -> floatArrayOf(0f, c, x)
            h < 240f -> floatArrayOf(0f, x, c)
            h < 300f -> floatArrayOf(x, 0f, c)
            else -> floatArrayOf(c, 0f, x)
        }

        return intArrayOf(
            ((r + m) * 255).toInt().coerceIn(0, 255),
            ((g + m) * 255).toInt().coerceIn(0, 255),
            ((b + m) * 255).toInt().coerceIn(0, 255)
        )
    }

    private fun randomBrightColor(): IntArray {
        return intArrayOf(
            (100..255).random(), // R
            (100..255).random(), // G
            (100..255).random()  // B
        )
    }

    private data class RegionColors(
        val fillColor: Int,
        val selectedFillColor: Int,
        val strokeColor: Int
    )

    private fun deselectRegion() {
        selectedFeature?.let { resetStyle(it) }
        selectedRegionId = null
        selectedFeature = null
        viewModel.loadNews()
    }

    private suspend fun zoomToFeatureAsync(feature: GeoJsonFeature, regionId: String) {
        val bounds = boundsCache[regionId] ?: withContext(Dispatchers.Default) {
            calculateBounds(feature)
        }.also { calculatedBounds ->
            if (calculatedBounds != null) {
                boundsCache[regionId] = calculatedBounds
            }
        }

        // Update camera on main thread after map is loaded
        bounds?.let {
            withContext(Dispatchers.Main) {
                if (isMapLoaded && googleMap != null) {
                    try {
                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(it, 150)
                        )
                    } catch (e: IllegalStateException) {
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(100)
                            if (isMapLoaded && googleMap != null) {
                                googleMap?.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(it, 150)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates LatLngBounds for a GeoJSON feature.
     * Handles both Polygon and MultiPolygon geometries.
     * This is an expensive operation and should run on a background thread.
     */
    private fun calculateBounds(feature: GeoJsonFeature): LatLngBounds? {
        val geometry = feature.geometry ?: return null
        val boundsBuilder = LatLngBounds.Builder()

        try {
            when (geometry) {
                is GeoJsonPolygon -> {
                    // Handle Polygon: iterate through all coordinate rings
                    geometry.coordinates.forEach { ring ->
                        ring.forEach { point ->
                            boundsBuilder.include(point)
                        }
                    }
                }
                is GeoJsonMultiPolygon -> {
                    // Handle MultiPolygon: iterate through all polygons and rings
                    geometry.polygons.forEach { polygon ->
                        polygon.coordinates.forEach { ring ->
                            ring.forEach { point ->
                                boundsBuilder.include(point)
                            }
                        }
                    }
                }
                else -> {
                    // Unsupported geometry type
                    return null
                }
            }

            return boundsBuilder.build()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Shows all of Slovenia by setting the camera to a default position.
     * Only called when map is fully loaded.
     */
    private fun showAllSlovenia() {
        if (!isMapLoaded) return

        val center = LatLng(46.1512, 14.9955)
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(center, 7f)
        )
    }

    /**
     * Returns the currently selected region ID (SR_ID from GeoJSON properties).
     * Can be used by other parts of the app to filter news by region.
     */
    fun getSelectedRegionId(): String? = selectedRegionId

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up resources
        geoJsonLayer?.removeLayerFromMap()
        geoJsonLayer = null
        newsMarkers.forEach { it.remove() }
        newsMarkers.clear()
        boundsCache.clear()
        isMapLoaded = false
    }
}