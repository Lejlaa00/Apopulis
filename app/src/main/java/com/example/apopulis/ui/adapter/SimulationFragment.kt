package com.example.apopulis.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.apopulis.R
import com.example.apopulis.databinding.FragmentSimulationBinding
import com.example.apopulis.simulation.SimulationConfig
import com.example.apopulis.simulation.SimulationViewModel
import com.example.apopulis.network.RetrofitInstance
import com.example.apopulis.viewmodel.MapViewModel
import com.example.apopulis.viewmodel.MapViewModelFactory
import com.example.apopulis.repository.NewsRepository
import com.example.apopulis.repository.CategoryRepository


class SimulationFragment : Fragment(R.layout.fragment_simulation) {

    private var _binding: FragmentSimulationBinding? = null
    private val binding get() = _binding!!

    private val simVm: SimulationViewModel by activityViewModels()
    private val mapVm: MapViewModel by activityViewModels {
        MapViewModelFactory(
            NewsRepository(RetrofitInstance.newsApi),
            CategoryRepository(RetrofitInstance.categoryApi)
        )
    }

    private var regionOptions = emptyList<com.example.apopulis.simulation.RegionOption>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSimulationBinding.bind(view)

        // default values
        binding.etPeriodMinutes.setText("5")
        binding.etCommentsPerPeriod.setText("5")
        binding.etBurstSpacing.setText("800")

        mapVm.loadNews()

        // regioni iz VM - spinner
        simVm.regions.observe(viewLifecycleOwner) { list ->
            regionOptions = list

            val labels = list.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.spRegion.adapter = adapter

            // ako VM već ima selektovan region, selektuj ga u spinneru
            val selectedId = simVm.selectedRegionId.value
            val idx = list.indexOfFirst { it.id == selectedId }
            if (idx >= 0) binding.spRegion.setSelection(idx)
        }

        // status , enable/disable
        simVm.isRunning.observe(viewLifecycleOwner) { running ->
            binding.tvStatus.text = if (running) "Status: running" else "Status: idle"
            binding.btnStart.isEnabled = !running
            binding.btnStop.isEnabled = running
        }

        binding.btnStart.setOnClickListener {
            if (regionOptions.isEmpty()) {
                Toast.makeText(requireContext(), "Regions not loaded yet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val regionIdx = binding.spRegion.selectedItemPosition
            val regionId = regionOptions.getOrNull(regionIdx)?.id
            if (regionId.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Select a region.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            simVm.setSelectedRegion(regionId)

            val periodMinutes = binding.etPeriodMinutes.text.toString().toIntOrNull() ?: 0
            val commentsPerPeriod = binding.etCommentsPerPeriod.text.toString().toIntOrNull() ?: 0
            val spacingMs = binding.etBurstSpacing.text.toString().toLongOrNull() ?: 0L

            if (periodMinutes <= 0 || commentsPerPeriod <= 0) {
                Toast.makeText(requireContext(), "Enter valid N minutes and N comments.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val config = SimulationConfig(
                periodMinutes = periodMinutes,
                commentsPerPeriod = commentsPerPeriod,
                burstSpacingMs = spacingMs.coerceAtLeast(100L)
            )

            simVm.requestStart(config) {
                mapVm.news.value
                    ?.mapNotNull { it._id }
                    ?.distinct()
                    ?: emptyList()
            }
            findNavController().popBackStack()
        }

        binding.btnStop.setOnClickListener {
            simVm.requestStop()
            findNavController().popBackStack()
        }

        simVm.ensureRegionsLoaded(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
