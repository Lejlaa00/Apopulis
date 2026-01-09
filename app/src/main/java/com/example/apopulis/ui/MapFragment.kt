package com.example.apopulis.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apopulis.R
import com.example.apopulis.databinding.BottomSheetNewsBinding
import com.example.apopulis.databinding.FragmentMapBinding
import com.example.apopulis.network.RetrofitInstance
import com.example.apopulis.repository.NewsRepository
import com.example.apopulis.ui.adapter.CategoryAdapter
import com.example.apopulis.ui.adapter.CategoryItem
import com.example.apopulis.ui.adapter.NewsAdapter
import com.example.apopulis.viewmodel.MapViewModel
import com.example.apopulis.viewmodel.MapViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
import android.content.res.Configuration
import android.util.TypedValue
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.apopulis.repository.CategoryRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBinding: BottomSheetNewsBinding

    private lateinit var viewModel: MapViewModel
    private lateinit var googleMap: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedCategoryId: String? = null

    private var geoJsonLayer: GeoJsonLayer? = null
    private var selectedFeature: GeoJsonFeature? = null
    private var selectedRegionId: String? = null
    private var isMapLoaded = false

    private var newsList: List<NewsItem> = emptyList()
    private val newsMarkers = mutableListOf<Marker>()
    // Map to store marker -> news item relationship
    private val markerToNewsMap = mutableMapOf<Marker, NewsItem>()

    // Cache for stable marker positions: key = "newsId_regionId" or "newsId_null"
    private val markerPositionCache = mutableMapOf<String, LatLng>()

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
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize bottom sheet binding
        bottomSheetBinding = BottomSheetNewsBinding.bind(binding.bottomSheetContent.root)

        // Setup UI components
        setupViewModel()
        viewModel.loadCategories()
        setupCategoryChips()
        setupBottomSheet()
        setupNewsRecyclerView()
        setupFloatingActionButton()

        binding.fabSimulation.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_simulationFragment)
        }

        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)
    }

    private fun setupViewModel() {
        val newsRepository = NewsRepository(RetrofitInstance.newsApi)
        val categoryRepository = CategoryRepository(RetrofitInstance.categoryApi)

        val factory = MapViewModelFactory(
            newsRepository = newsRepository,
            categoryRepository = categoryRepository
        )
        viewModel = ViewModelProvider(this, factory).get(MapViewModel::class.java)

        // Observe news data - update both markers and bottom sheet
        viewModel.news.observe(viewLifecycleOwner) { list ->
            newsList = list

            Log.e("PIN_DEBUG", "Observer received ${list.size} news")

            // Update markers
            if (isMapLoaded) {
                redrawPins(googleMap.cameraPosition.zoom)
            }

            // Update bottom sheet
            updateBottomSheetNews()
        }
        viewModel.loadNews()
        viewModel.loadCategories()
    }

    private fun setupCategoryChips() {

        categoryAdapter = CategoryAdapter { category ->
            handleCategoryClick(category)
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        viewModel.categories.observe(viewLifecycleOwner) { list ->
            val items = list.map {
                CategoryItem(
                    id = it._id,
                    name = it.name.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                    }
                )
            }
            categoryAdapter.submitList(items)
        }
    }

    private fun handleCategoryClick(category: CategoryItem) {
        // If clicking the same category, deselect it
        if (selectedCategoryId == category.id) {
            selectedCategoryId = null
            categoryAdapter.clearSelection()
        } else {
            // Select new category
            selectedCategoryId = category.id
            val position = categoryAdapter.currentList.indexOfFirst { it.id == category.id }
            if (position != -1) {
                categoryAdapter.selectCategory(position)
            }
        }

        // Update displayed news based on category filter
        updateBottomSheetNews()

        // Update markers based on category filter
        if (isMapLoaded) {
            redrawPins(googleMap.cameraPosition.zoom)
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        bottomSheetBehavior.apply {
            // Set peek height explicitly
            peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)

            // Configure behavior
            isHideable = false
            isDraggable = true
            isFitToContents = false
            halfExpandedRatio = 0.5f

            // Set initial state to COLLAPSED (this makes it visible with peek height)
            state = BottomSheetBehavior.STATE_COLLAPSED

            // Add callback to animate FAB based on bottom sheet state
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    updateFabVisibility(newState)
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // slideOffset: -1 (fully collapsed) to 1 (fully expanded)
                    // We want to hide FAB when sheet is expanded (slideOffset > 0)
                    animateFabVisibility(slideOffset)
                }
            })
        }

        // Ensure the bottom sheet is visible by posting state set after layout
        binding.bottomSheet.post {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        // Make header clickable to expand
        bottomSheetBinding.headerLayout.setOnClickListener {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
                BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
                else -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
    }

    private fun setupNewsRecyclerView() {
        newsAdapter = NewsAdapter { newsItem ->
            // Handle news item click from bottom sheet
            openNewsDetailDialog(newsItem)
        }
        bottomSheetBinding.rvNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
        }
    }

    private fun openNewsDetailDialog(newsItem: NewsItem) {
        val dialog = NewsDetailDialogFragment.newInstance(newsItem)
        dialog.show(parentFragmentManager, "NewsDetailDialog")
    }
    private fun setupFloatingActionButton() {
        val fab = binding.fabCreatePost
        val context = requireContext()

        // Detect dark/light mode
        val isDarkMode = isDarkMode(context)

        // Set icon based on theme
        val iconRes = if (isDarkMode) {
            R.drawable.ic_news_dark

        } else {
            R.drawable.ic_news_light
        }
        fab.setImageResource(iconRes)

        val iconTint = if (isDarkMode) {
            Color.WHITE
        } else {
            Color.BLACK
        }
        fab.imageTintList = ColorStateList.valueOf(iconTint)

        val backgroundColor = if (isDarkMode) {
            ContextCompat.getColor(context, R.color.apopulis_gray)
        } else {
            Color.WHITE
        }
        fab.backgroundTintList = android.content.res.ColorStateList.valueOf(backgroundColor)

        fab.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_createPost)
        }

        fab.alpha = 1f
        fab.scaleX = 1f
        fab.scaleY = 1f
    }

    private fun isDarkMode(context: android.content.Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateFabVisibility(state: Int) {
        val fab = binding.fabCreatePost
        when (state) {
            BottomSheetBehavior.STATE_COLLAPSED -> {
                // Show FAB when collapsed
                fab.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            BottomSheetBehavior.STATE_HALF_EXPANDED,
            BottomSheetBehavior.STATE_EXPANDED -> {
                // Hide FAB when half-expanded or expanded
                fab.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun animateFabVisibility(slideOffset: Float) {
        val fab = binding.fabCreatePost
        // slideOffset: -1 (fully collapsed) to 1 (fully expanded)
        // We want FAB visible when collapsed (slideOffset = -1) and hidden when expanded (slideOffset > 0)

        // Normalize offset: 0 = collapsed, 1 = expanded
        val normalizedOffset = (slideOffset + 1f) / 2f // Maps -1..1 to 0..1

        // Calculate alpha: 1 when collapsed (normalizedOffset = 0), 0 when expanded (normalizedOffset = 1)
        val alpha = 1f - normalizedOffset.coerceIn(0f, 1f)

        // Calculate scale: 1 when collapsed, 0.8 when expanded
        val scale = 1f - (normalizedOffset * 0.2f).coerceIn(0f, 0.2f)

        fab.alpha = alpha
        fab.scaleX = scale
        fab.scaleY = scale
    }

    private fun updateBottomSheetNews() {
        val filteredNews = newsList.filter { news ->
            if (selectedCategoryId != null) {
                if (news.categoryId?._id != selectedCategoryId) {
                    return@filter false
                }
            }

            val feature = selectedFeature
            if (feature != null) {
                val loc = news.locationId ?: return@filter false
                if (loc.latitude == 0.0 && loc.longitude == 0.0) return@filter false

                val realPoint = LatLng(loc.latitude, loc.longitude)
                val inside = isPointInsideFeature(realPoint, feature)
                if (!inside) return@filter false
            }

            true
        }

        newsAdapter.submitList(filteredNews)
        bottomSheetBinding.tvNewsCount.text = "${filteredNews.size} items"

        updateBottomSheetTitle()
    }

    private fun updateBottomSheetTitle() {
        val title = when {
            selectedFeature != null -> {
                // Try to get region name from feature properties
                val regionName = selectedFeature?.getProperty("SR_UIME") as? String
                regionName ?: "Selected Region"
            }
            else -> "All News"
        }
        bottomSheetBinding.tvBottomSheetTitle.text = title
    }

    private fun getCandidateNewsIdsForSelectedRegion(): List<String> {
        val feature = selectedFeature ?: return emptyList()

        return newsList.mapNotNull { news ->
            val loc = news.locationId ?: return@mapNotNull null
            if (loc.latitude == 0.0 && loc.longitude == 0.0) return@mapNotNull null

            val point = LatLng(loc.latitude, loc.longitude)
            if (isPointInsideFeature(point, feature)) news._id else null
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
            // Find the news item associated with this marker
            markerToNewsMap[marker]?.let { newsItem ->
                openNewsDetailDialog(newsItem)
            }
            true // Consume the event to prevent region click
        }

        viewModel.loadNews()
    }

    // Pins
    private fun redrawPins(zoom: Float) {
        if (!isMapLoaded) return

        newsMarkers.forEach { it.remove() }
        newsMarkers.clear()
        markerToNewsMap.clear()

        val feature = selectedFeature

        Log.e(
            "PIN_DEBUG",
            "REDRAW news=${newsList.size}, regionSelected=${feature != null}"
        )

        newsList.forEach { news ->
            // Filter by category if one is selected
            if (selectedCategoryId != null) {
                if (news.categoryId?._id != selectedCategoryId) {
                    return@forEach
                }
            }

            val loc = news.locationId ?: return@forEach
            if (loc.latitude == 0.0 && loc.longitude == 0.0) return@forEach

            val realPoint = LatLng(loc.latitude, loc.longitude)

            if (feature != null) {
                val inside = isPointInsideFeature(realPoint, feature)
                if (!inside) return@forEach
            }

            // Get stable position for this news item
            val position = getStableMarkerPosition(news, feature, realPoint)

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(news.title)
                    .snippet(loc.name)
                    .icon(MarkerIconGenerator.createNewsMarker(requireContext()))
                    .anchor(0.5f, 1.0f) // Anchor at bottom center (pin tip)
            )

            marker?.let {
                newsMarkers.add(it)
                markerToNewsMap[it] = news // Store marker -> news mapping
            }
        }

        Log.e("PIN_DEBUG", "DRAWN markers=${newsMarkers.size}")
    }

    private fun getStableMarkerPosition(
        news: NewsItem,
        feature: GeoJsonFeature?,
        realPoint: LatLng
    ): LatLng {
        // When no region is selected, use the real location
        if (feature == null) {
            val cacheKey = "${news._id}_null"
            return markerPositionCache.getOrPut(cacheKey) { realPoint }
        }

        // When a region is selected, use deterministic random position
        val regionId = feature.getProperty("SR_ID")?.toString() ?: "unknown"
        val cacheKey = "${news._id}_$regionId"

        // Return cached position if available
        markerPositionCache[cacheKey]?.let { return it }

        // Generate new deterministic position and cache it
        val position = generateDeterministicPointInFeature(feature, news._id, regionId)
        markerPositionCache[cacheKey] = position
        return position
    }

    private fun generateDeterministicPointInFeature(
        feature: GeoJsonFeature,
        newsId: String,
        regionId: String
    ): LatLng {
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

        // Create a deterministic seed from newsId + regionId
        val seed = (newsId + regionId).hashCode().toLong()
        val random = java.util.Random(seed)

        // Try up to 300 times to find a valid point inside the polygon
        repeat(300) {
            val point = deterministicPointInBounds(bounds, random)

            if (polygons.any { polygon ->
                    PolyUtil.containsLocation(point, polygon, true)
                }) {
                return point
            }
        }

        // Fallback to bounds center if no valid point found
        return bounds.center
    }

    private fun deterministicPointInBounds(bounds: LatLngBounds, random: java.util.Random): LatLng {
        val lat = bounds.southwest.latitude +
                random.nextDouble() * (bounds.northeast.latitude - bounds.southwest.latitude)

        val lng = bounds.southwest.longitude +
                random.nextDouble() * (bounds.northeast.longitude - bounds.southwest.longitude)

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

        // Update bottom sheet to show only news from selected region
        updateBottomSheetNews()

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

        // Update bottom sheet to show all news again
        updateBottomSheetNews()
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

    private fun showAllSlovenia() {
        if (!isMapLoaded) return

        val center = LatLng(46.1512, 14.9955)
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(center, 7f)
        )
    }

    fun getSelectedRegionId(): String? = selectedRegionId

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up resources
        geoJsonLayer?.removeLayerFromMap()
        geoJsonLayer = null
        newsMarkers.forEach { it.remove() }
        newsMarkers.clear()
        markerToNewsMap.clear()
        markerPositionCache.clear()
        boundsCache.clear()
        isMapLoaded = false
        _binding = null
    }
}