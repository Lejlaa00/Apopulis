package com.example.apopulis.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.apopulis.databinding.FragmentSettingsBinding
import com.example.apopulis.worker.ViralNewsWorker
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREFS_NAME = "ApopulisSettings"
        private const val KEY_VIRAL_THRESHOLD = "viral_threshold"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val DEFAULT_VIRAL_THRESHOLD = 5
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
            binding.switchNotifications.isChecked = true
        } else {
            Toast.makeText(
                requireContext(),
                "Notification permission denied. Enable in settings",
                Toast.LENGTH_LONG
            ).show()
            binding.switchNotifications.isChecked = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkNotificationPermission()
        loadSettings()
        setupListeners()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                binding.switchNotifications.isChecked = false
            }
        }
    }

    private fun loadSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val threshold = prefs.getInt(KEY_VIRAL_THRESHOLD, DEFAULT_VIRAL_THRESHOLD)
        binding.etViralThreshold.setText(threshold.toString())

        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            binding.switchNotifications.isChecked = notificationsEnabled && hasPermission
        } else {
            binding.switchNotifications.isChecked = notificationsEnabled
        }

        binding.tilViralThreshold.isEnabled = binding.switchNotifications.isChecked
    }

    private fun setupListeners() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }
                
                binding.tilViralThreshold.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Viral news notifications enabled",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                binding.tilViralThreshold.isEnabled = false
                Toast.makeText(
                    requireContext(),
                    "Viral news notifications disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val thresholdText = binding.etViralThreshold.text.toString()
        
        if (thresholdText.isBlank()) {
            binding.tilViralThreshold.error = "Please enter a threshold"
            return
        }

        val threshold = thresholdText.toIntOrNull()
        if (threshold == null || threshold < 1) {
            binding.tilViralThreshold.error = "Please enter a valid number (minimum 1)"
            return
        }

        if (threshold > 999) {
            binding.tilViralThreshold.error = "Maximum threshold is 999"
            return
        }

        binding.tilViralThreshold.error = null

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_VIRAL_THRESHOLD, threshold)
            putBoolean(KEY_NOTIFICATIONS_ENABLED, binding.switchNotifications.isChecked)
            apply()
        }

        if (binding.switchNotifications.isChecked) {
            setupViralNewsChecker()
            Toast.makeText(
                requireContext(),
                "Settings saved! Checking every 5 minutes",
                Toast.LENGTH_LONG
            ).show()
        } else {
            cancelViralNewsChecker()
            Toast.makeText(
                requireContext(),
                "Settings saved! Notifications disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        findNavController().navigateUp()
    }

    private fun setupViralNewsChecker() {
        val workRequest = PeriodicWorkRequestBuilder<ViralNewsWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "ViralNewsChecker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelViralNewsChecker() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("ViralNewsChecker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
