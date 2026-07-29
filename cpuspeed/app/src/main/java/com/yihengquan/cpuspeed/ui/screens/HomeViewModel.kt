package com.yihengquan.cpuspeed.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yihengquan.cpuspeed.data.CPUInfo
import com.yihengquan.cpuspeed.data.CPUManager
import com.yihengquan.cpuspeed.data.CPUPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val cpuInfo: CPUInfo = CPUInfo(),
    val isRooted: Boolean = false,
    val rootChecked: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null,
    val currentGovernor: String = "unknown",
    val availableGovernors: List<String> = emptyList(),
    val applyOnBoot: Boolean = false,
    val maxSlider: Float = 0f,
    val minSlider: Float = 0f,
    val perCoreMode: Boolean = false,
    val coreMaxSliders: List<Float> = emptyList(),
    val coreMinSliders: List<Float> = emptyList(),
    val showSettings: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val cpuManager = CPUManager(application)
    private val prefs = CPUPrefs(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCPUInfo()
        checkRoot()
        loadPrefs()
    }

    private fun loadPrefs() {
        _uiState.value = _uiState.value.copy(
            applyOnBoot = prefs.applyOnBoot
        )
    }

    fun checkRoot() {
        viewModelScope.launch {
            val rooted = withContext(Dispatchers.IO) { cpuManager.isRooted() }
            _uiState.value = _uiState.value.copy(
                isRooted = rooted,
                rootChecked = true
            )
        }
    }

    fun loadCPUInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val info = withContext(Dispatchers.IO) { cpuManager.getCPUInfo() }
            val governor = withContext(Dispatchers.IO) { cpuManager.getCurrentGovernor() }
            val governors = withContext(Dispatchers.IO) { cpuManager.getAvailableGovernors() }
            val coreMax = info.coreFrequencies.map { cf ->
                val diff = maxOf(cf.maxFreq - cf.minFreq, 1)
                if (cf.curFreq > 0) (cf.curFreq - cf.minFreq).toFloat() / diff else 0f
            }
            val coreMin = info.coreFrequencies.map { cf ->
                val diff = maxOf(cf.maxFreq - cf.minFreq, 1)
                if (cf.curFreq > 0) (cf.curFreq - cf.minFreq).toFloat() / diff else 0f
            }
            _uiState.value = _uiState.value.copy(
                cpuInfo = info,
                currentGovernor = governor,
                availableGovernors = governors,
                isBusy = false,
                maxSlider = info.maxPercent,
                minSlider = info.minPercent,
                coreMaxSliders = coreMax,
                coreMinSliders = coreMin,
            )
        }
    }

    fun setMaxSpeed(percent: Float) {
        _uiState.value = _uiState.value.copy(maxSlider = percent)
    }

    fun setMinSpeed(percent: Float) {
        _uiState.value = _uiState.value.copy(minSlider = percent)
    }

    fun applySpeed() {
        val state = _uiState.value
        if (!state.isRooted) {
            _uiState.value = state.copy(message = "Device is not rooted")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            withContext(Dispatchers.IO) {
                if (state.perCoreMode) {
                    // Apply per-core
                    for (i in state.coreMaxSliders.indices) {
                        val cf = state.cpuInfo.coreFrequencies.getOrNull(i) ?: continue
                        val max = state.cpuInfo.calcCoreFrequency(
                            state.coreMaxSliders.getOrElse(i) { 0f }, cf.minFreq, cf.maxFreq
                        )
                        val min = state.cpuInfo.calcCoreFrequency(
                            state.coreMinSliders.getOrElse(i) { 0f }, cf.minFreq, cf.maxFreq
                        )
                        cpuManager.setCoreSpeed(i, max, min) { _, _ -> }
                    }
                    _uiState.value = _uiState.value.copy(isBusy = false, message = "Per-core speed applied")
                } else {
                    val max = state.cpuInfo.calcFrequency(state.maxSlider)
                    val min = state.cpuInfo.calcFrequency(state.minSlider)
                    cpuManager.setCPUSpeed(max, min) { success, msg ->
                        _uiState.value = _uiState.value.copy(
                            isBusy = false,
                            message = if (success) "Speed applied" else msg
                        )
                        if (success) {
                            prefs.maxSpeed = max
                            prefs.minSpeed = min
                        }
                    }
                }
            }
        }
    }

    fun setGovernor(governor: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            withContext(Dispatchers.IO) {
                cpuManager.setGovernor(governor) { success, msg ->
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        currentGovernor = if (success) governor else _uiState.value.currentGovernor,
                        message = if (success) "Governor set: $governor" else msg
                    )
                    if (success) prefs.governor = governor
                }
            }
        }
    }

    fun toggleApplyOnBoot(enabled: Boolean) {
        prefs.applyOnBoot = enabled
        _uiState.value = _uiState.value.copy(applyOnBoot = enabled)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun togglePerCoreMode() {
        _uiState.value = _uiState.value.copy(perCoreMode = !_uiState.value.perCoreMode)
    }

    fun setCoreMaxSlider(core: Int, percent: Float) {
        val list = _uiState.value.coreMaxSliders.toMutableList()
        if (core in list.indices) {
            list[core] = percent
            _uiState.value = _uiState.value.copy(coreMaxSliders = list)
        }
    }

    fun setCoreMinSlider(core: Int, percent: Float) {
        val list = _uiState.value.coreMinSliders.toMutableList()
        if (core in list.indices) {
            list[core] = percent
            _uiState.value = _uiState.value.copy(coreMinSliders = list)
        }
    }

    fun toggleSettings() {
        _uiState.value = _uiState.value.copy(showSettings = !_uiState.value.showSettings)
    }

    fun dismissSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }
}
