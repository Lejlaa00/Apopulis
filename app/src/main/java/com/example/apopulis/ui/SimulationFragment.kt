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
import com.example.apopulis.viewmodel.RegionOption
import com.example.apopulis.viewmodel.SimulationConfig
import com.example.apopulis.viewmodel.SimulationViewModel


class SimulationFragment : Fragment(R.layout.fragment_simulation) {

    private var _binding: FragmentSimulationBinding? = null

    private val binding get() = _binding!!

    private val simVm: SimulationViewModel by activityViewModels()

    private var regionOptions = emptyList<RegionOption>()

    private var ignoreSpinnerCallback = false
    private var spinnerReady = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSimulationBinding.bind(view)

        if (childFragmentManager.findFragmentById(R.id.region_map_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.region_map_container, MapFragment.newPickerInstance())
                .commit()
        }


        simVm.currentConfig.observe(viewLifecycleOwner) { cfg ->
            val period = cfg?.periodMinutes ?: 5
            val count = cfg?.commentsPerPeriod ?: 5
            val spacing = cfg?.burstSpacingMs ?: 800L

            if (!binding.etPeriodMinutes.hasFocus()) binding.etPeriodMinutes.setText(period.toString())
            if (!binding.etCommentsPerPeriod.hasFocus()) binding.etCommentsPerPeriod.setText(count.toString())
            if (!binding.etBurstSpacing.hasFocus()) binding.etBurstSpacing.setText(spacing.toString())
        }

        simVm.regions.observe(viewLifecycleOwner) { list ->
            regionOptions = list

            val labels = mutableListOf("Select a region…")
            labels.addAll(list.map { it.name })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.spRegion.adapter = adapter


            binding.spRegion.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {

                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (ignoreSpinnerCallback) return

                        if (position == 0) {
                            simVm.setSelectedRegion(null)
                            return
                        }

                        val pickedId = regionOptions.getOrNull(position - 1)?.id ?: return
                        simVm.setSelectedRegion(pickedId)
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                        simVm.setSelectedRegion(null)
                    }
                }


            spinnerReady = true

            val selectedId = simVm.selectedRegionId.value
            val idx = list.indexOfFirst { it.id == selectedId }

            binding.spRegion.setSelection(if (idx >= 0) idx + 1 else 0)
            spinnerReady = true
        }

        simVm.selectedRegionId.observe(viewLifecycleOwner) { id ->
            if (!spinnerReady) return@observe

            val targetPos = if (id.isNullOrBlank()) {
                0
            } else {
                val idx = regionOptions.indexOfFirst { it.id == id }
                if (idx >= 0) idx + 1 else 0
            }

            if (binding.spRegion.selectedItemPosition != targetPos) {
                ignoreSpinnerCallback = true
                binding.spRegion.setSelection(targetPos, true)
                ignoreSpinnerCallback = false
            }
        }



        // status , enable/disable
        simVm.isRunning.observe(viewLifecycleOwner) { running ->
            binding.etPeriodMinutes.isEnabled =!running
            binding.etCommentsPerPeriod.isEnabled = !running
            binding.etBurstSpacing.isEnabled = !running
            binding.spRegion.isEnabled = !running
            binding.btnStart.isEnabled = !running
            binding.btnStop.isEnabled = running
        }

        binding.btnStart.setOnClickListener {
            val regionId = simVm.selectedRegionId.value
            if (regionId.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Select a region (dropdown or map).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                simVm.getCandidates().distinct()
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
