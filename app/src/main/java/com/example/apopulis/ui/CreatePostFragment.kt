package com.example.apopulis.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apopulis.MainActivity
import com.example.apopulis.databinding.FragmentCreatePostBinding
import com.example.apopulis.network.RetrofitInstance
import com.example.apopulis.repository.CategoryRepository
import com.example.apopulis.repository.NewsRepository
import com.example.apopulis.viewmodel.MapViewModel
import com.example.apopulis.viewmodel.MapViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var selectedImageBitmap: Bitmap? = null
    private var hasSelectedImage: Boolean = false
    private val uiScope = CoroutineScope(Dispatchers.Main)

    // Shared MapViewModel to trigger news refresh after post creation
    private val mapViewModel: MapViewModel? by lazy {
        try {
            val activity = requireActivity() as? MainActivity
            val factory = activity?.mapViewModelFactory
                ?: MapViewModelFactory(
                    NewsRepository(RetrofitInstance.newsApi),
                    CategoryRepository(RetrofitInstance.categoryApi)
                )
            ViewModelProvider(requireActivity(), factory).get(MapViewModel::class.java)
        } catch (e: Exception) {
            Log.e("CreatePost", "Failed to access MapViewModel", e)
            null
        }
    }

    // GPS location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    // Location permission request
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission is required to post news", Toast.LENGTH_LONG).show()
            }
        }

    /* IMAGE PICKERS */

    // Gallery picker
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { showImage(it) }
        }

    // Camera preview picker
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                selectedImageBitmap = it
                binding.imgPhoto.setImageBitmap(it)
                selectedImageUri = null // Camera doesn't provide URI, only bitmap
                hasSelectedImage = true
                updateImageState()
            }
        }

    /* LIFECYCLE */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupClicks()
        setupTextWatcher()
        updatePublishState()

        // Request location permission and get current location
        requestLocationPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiScope.cancel()
        _binding = null
    }

    /* UI SETUP */

    private fun setupClicks() {

        // Add photo (Camera / Gallery) - only when no image is selected
        binding.photoContainer.setOnClickListener {
            if (!hasSelectedImage) {
                openImageSourceDialog()
            }
        }

        // Edit photo - opens image selection dialog
        binding.iconEdit.setOnClickListener {
            openImageSourceDialog()
        }

        // Delete photo - removes image and returns to initial state
        binding.iconDelete.setOnClickListener {
            removeImage()
        }

        // Publish
        binding.btnPublish.setOnClickListener {
            runAiCheckAndPublish()
        }
    }

    private fun setupTextWatcher() {
        binding.etPostText.doOnTextChanged { _, _, _, _ ->
            updatePublishState()
        }
    }

    /* IMAGE SOURCE DIALOG */

    private fun openImageSourceDialog() {
        val options = arrayOf("Camera", "Gallery")

        AlertDialog.Builder(requireContext())
            .setTitle("Add photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    private fun showImage(uri: Uri) {
        selectedImageUri = uri
        hasSelectedImage = true
        binding.imgPhoto.setImageURI(uri)
        updateImageState()
    }

    private fun updateImageState() {
        val paddingDp = 16
        val paddingPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            paddingDp.toFloat(),
            resources.displayMetrics
        ).toInt()

        if (hasSelectedImage) {
            // Show image, hide add layout
            binding.imgPhoto.visibility = View.VISIBLE
            binding.addPhotoLayout.visibility = View.GONE
            // Show edit and delete icons
            binding.iconEdit.visibility = View.VISIBLE
            binding.iconDelete.visibility = View.VISIBLE
            // Remove padding so image fills entire card area
            binding.photoContainer.setPadding(0, 0, 0, 0)
        } else {
            // Hide image, show add layout
            binding.imgPhoto.visibility = View.GONE
            binding.addPhotoLayout.visibility = View.VISIBLE
            // Hide edit and delete icons
            binding.iconEdit.visibility = View.GONE
            binding.iconDelete.visibility = View.GONE
            // Restore padding for add layout
            binding.photoContainer.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            // Clear image data
            selectedImageUri = null
            binding.imgPhoto.setImageURI(null)
            binding.imgPhoto.setImageBitmap(null)
        }
    }

    private fun removeImage() {
        hasSelectedImage = false
        selectedImageBitmap = null
        updateImageState()
    }

    /* PUBLISH + AI*/
    private fun updatePublishState() {
        val hasText = binding.etPostText.text?.isNotBlank() == true

        binding.btnPublish.isEnabled = hasText
        binding.btnPublish.alpha = if (hasText) 1f else 0.5f
    }

    private fun runAiCheckAndPublish() {
        // Check if we have location
        if (currentLatitude == null || currentLongitude == null) {
            Toast.makeText(requireContext(), "Getting your location...", Toast.LENGTH_SHORT).show()
            getCurrentLocation()
            return
        }

        binding.tvAiStatus.visibility = View.VISIBLE
        binding.tvAiStatus.text = "AI checking…"

        binding.btnPublish.isEnabled = false
        binding.btnPublish.alpha = 0.6f

        uiScope.launch {
            try {
                // If image exists, check it with AI first
                if (hasSelectedImage) {
                    val imageFile = prepareImageFile()
                    if (imageFile != null) {
                        val isFake = checkImageWithAI(imageFile)

                        if (isFake) {
                            binding.tvAiStatus.text = "⚠️ Potentially fake – review recommended"
                            binding.tvAiStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))

                            // Show warning dialog
                            withContext(Dispatchers.Main) {
                                showFakeImageWarningDialog {
                                    // User chose to proceed anyway
                                    uiScope.launch {
                                        publishPost(imageFile)
                                    }
                                }
                            }
                            return@launch
                        } else {
                            binding.tvAiStatus.text = "✅ Content verified"
                            binding.tvAiStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                        }
                    }
                }

                // Proceed with publishing
                publishPost(if (hasSelectedImage) prepareImageFile() else null)

            } catch (e: Exception) {
                Log.e("CreatePost", "Error during AI check", e)
                withContext(Dispatchers.Main) {
                    binding.tvAiStatus.text = "Error: ${e.message}"
                    binding.tvAiStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    updatePublishState()
                }
            }
        }
    }

    private suspend fun checkImageWithAI(imageFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestBody)

                val response = RetrofitInstance.mlApi.predictImage(imagePart)

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("CreatePost", "AI Result: ${result?.data?.prediction}, confidence: ${result?.data?.confidence}")
                    result?.data?.is_fake ?: false
                } else {
                    Log.e("CreatePost", "AI check failed: ${response.code()}")
                    // If AI check fails, allow posting (fail open)
                    false
                }
            } catch (e: Exception) {
                Log.e("CreatePost", "AI check error", e)
                // If AI check fails, allow posting (fail open)
                false
            }
        }
    }

    private fun showFakeImageWarningDialog(onProceed: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Warning: Potentially AI-Generated Image")
            .setMessage("Our AI system detected that this image might be artificially generated or manipulated. Are you sure you want to proceed with posting?")
            .setPositiveButton("Post Anyway") { _, _ ->
                onProceed()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                uiScope.launch {
                    withContext(Dispatchers.Main) {
                        binding.tvAiStatus.visibility = View.GONE
                        updatePublishState()
                    }
                }
            }
            .setCancelable(false)
            .show()
    }

    private suspend fun publishPost(imageFile: File?) {
        withContext(Dispatchers.IO) {
            try {
                val title = binding.etPostText.text.toString()
                val content = binding.etPostText.text.toString()

                // Prepare request parts
                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryBody = "user".toRequestBody("text/plain".toMediaTypeOrNull())
                val locationBody = "User Location".toRequestBody("text/plain".toMediaTypeOrNull())
                val latitudeBody = currentLatitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val longitudeBody = currentLongitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val imagePart = imageFile?.let {
                    val requestBody = it.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("image", it.name, requestBody)
                }

                // Upload news
                val response = RetrofitInstance.newsApi.uploadNewsWithImage(
                    titleBody,
                    contentBody,
                    categoryBody,
                    locationBody,
                    latitudeBody,
                    longitudeBody,
                    imagePart
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        binding.tvAiStatus.text = "✅ Post published successfully!"
                        binding.tvAiStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

                        Toast.makeText(requireContext(), "News posted successfully!", Toast.LENGTH_LONG).show()

                        // Refresh news list in MapFragment immediately
                        mapViewModel?.loadNews()

                        // Clear form after successful post
                        delay(1500)
                        clearForm()
                    } else {
                        binding.tvAiStatus.text = "❌ Failed to publish: ${response.code()}"
                        binding.tvAiStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                        updatePublishState()
                    }
                }

            } catch (e: Exception) {
                Log.e("CreatePost", "Error publishing post", e)
                withContext(Dispatchers.Main) {
                    binding.tvAiStatus.text = "❌ Error: ${e.message}"
                    binding.tvAiStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    updatePublishState()
                }
            }
        }
    }

    private fun prepareImageFile(): File? {
        return try {
            val context = requireContext()
            val cacheDir = context.cacheDir
            val imageFile = File(cacheDir, "upload_image_${System.currentTimeMillis()}.jpg")

            if (selectedImageBitmap != null) {
                // Use bitmap from camera
                val outputStream = FileOutputStream(imageFile)
                selectedImageBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()
            } else if (selectedImageUri != null) {
                // Use URI from gallery
                val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                val outputStream = FileOutputStream(imageFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
            } else {
                return null
            }

            imageFile
        } catch (e: Exception) {
            Log.e("CreatePost", "Error preparing image file", e)
            null
        }
    }

    private fun clearForm() {
        binding.etPostText.text?.clear()
        removeImage()
        binding.tvAiStatus.visibility = View.GONE
        Toast.makeText(requireContext(), "Form cleared", Toast.LENGTH_SHORT).show()
    }

    /* GPS LOCATION */
    private fun requestLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                Log.d("CreatePost", "Location obtained: $currentLatitude, $currentLongitude")
                Toast.makeText(
                    requireContext(),
                    "Location: ${String.format("%.4f", currentLatitude)}, ${String.format("%.4f", currentLongitude)}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("CreatePost", "Failed to get location", e)
            Toast.makeText(requireContext(), "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
