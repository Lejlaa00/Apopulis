package com.example.apopulis.simulation

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apopulis.R
import org.json.JSONObject

data class RegionOption(val id: String, val name: String)

data class SimulationConfig(
    val periodMinutes: Int,
    val commentsPerPeriod: Int,
    val burstSpacingMs: Long
)

sealed class SimulationCommand {
    data class Start(val config: SimulationConfig) : SimulationCommand()
    data object Stop : SimulationCommand()
}

class Event<out T>(private val content: T) {
    private var handled = false
    fun getContentIfNotHandled(): T? {
        if (handled) return null
        handled = true
        return content
    }
    fun peek(): T = content
}

class SimulationViewModel : ViewModel() {
     private val simManager = CommentSimulationManager()
    // Region list
    private val _regions = MutableLiveData<List<RegionOption>>(emptyList())
    val regions: LiveData<List<RegionOption>> = _regions

    // Selected region
    private val _selectedRegionId = MutableLiveData<String?>(null)
    val selectedRegionId: LiveData<String?> = _selectedRegionId

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    fun ensureRegionsLoaded(context: Context) {
        if (!_regions.value.isNullOrEmpty()) return
        _regions.value = loadRegionsFromRaw(context)
    }

    private fun loadRegionsFromRaw(context: Context): List<RegionOption> {
        val jsonText = context.resources.openRawResource(R.raw.sr_regions)
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(jsonText)
        val features = root.getJSONArray("features")

        val list = ArrayList<RegionOption>(features.length())

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val props = feature.optJSONObject("properties") ?: continue

            val regionId = props.opt("SR_ID")?.toString() ?: continue
            val regionName = props.optString("SR_UIME", "Unknown")

            list.add(RegionOption(id = regionId, name = regionName))
        }

        return list.sortedBy { it.name }
    }

    fun setRegions(list: List<RegionOption>) {
        _regions.value = list
    }

    fun setSelectedRegion(id: String?) {
        _selectedRegionId.value = id
    }

    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }

    fun requestStart(
        config: SimulationConfig,
        getCandidateNewsIds: () -> List<String>
    ) {
        if (simManager.isRunning()) return

        _isRunning.value = true

        simManager.startRegionRandom(
            periodMinutes = config.periodMinutes,
            commentsPerPeriod = config.commentsPerPeriod,
            burstSpacingMs = config.burstSpacingMs,
            getCandidateNewsIds = getCandidateNewsIds,
            onEvent = { ev ->
                android.util.Log.d("SIM", "event=$ev")
            }
        )
    }

    fun requestStop() {
        simManager.stop()
        _isRunning.value = false
    }

    override fun onCleared() {
        simManager.stop()
        super.onCleared()
    }

}
