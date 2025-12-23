package com.example.apopulis.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.example.apopulis.databinding.FragmentCreatePostBinding
import kotlinx.coroutines.*

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)

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
                binding.imgPhoto.setImageBitmap(it)
                binding.imgPhoto.visibility = View.VISIBLE
                binding.addPhotoLayout.visibility = View.GONE
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

        setupClicks()
        setupTextWatcher()
        updatePublishState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiScope.cancel()
        _binding = null
    }

    /* UI SETUP */

    private fun setupClicks() {

        // Add photo (Camera / Gallery)
        binding.photoContainer.setOnClickListener {
            openImageSourceDialog()
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
        binding.imgPhoto.setImageURI(uri)
        binding.imgPhoto.visibility = View.VISIBLE
        binding.addPhotoLayout.visibility = View.GONE
    }

    /* PUBLISH + AI*/
    private fun updatePublishState() {
        val hasText = binding.etPostText.text?.isNotBlank() == true

        binding.btnPublish.isEnabled = hasText
        binding.btnPublish.alpha = if (hasText) 1f else 0.5f
    }

    private fun runAiCheckAndPublish() {
        binding.tvAiStatus.visibility = View.VISIBLE
        binding.tvAiStatus.text = "AI checking…"

        binding.btnPublish.isEnabled = false
        binding.btnPublish.alpha = 0.6f

        // AI simulation
        uiScope.launch {
            delay(2000)

            val isFake = false

            if (isFake) {
                binding.tvAiStatus.text = "⚠️ Potentially fake – review recommended"
            } else {
                binding.tvAiStatus.text = "✅ Content verified"
                publishPost()
            }

            updatePublishState()
        }
    }

    private fun publishPost() {
        // - upload image (ako postoji)
        // - upload text
        // - location/category auto

        binding.tvAiStatus.text = "Post published"
    }
}
