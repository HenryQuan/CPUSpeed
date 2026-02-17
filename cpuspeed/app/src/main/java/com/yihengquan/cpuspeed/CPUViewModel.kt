package com.yihengquan.cpuspeed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing CPU speed state
 */
class CPUViewModel(application: Application) : AndroidViewModel(application) {
    private val cpuManager = CPUManager()
    
    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()
    
    private val _maxSpeed = MutableStateFlow(0f)
    val maxSpeed: StateFlow<Float> = _maxSpeed.asStateFlow()
    
    private val _minSpeed = MutableStateFlow(0f)
    val minSpeed: StateFlow<Float> = _minSpeed.asStateFlow()
    
    init {
        checkDeviceCompatibility()
    }
    
    private fun checkDeviceCompatibility() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (!cpuManager.isDeviceRooted()) {
                    UIState.Error("Device is not rooted. Root access is required to control CPU speed.")
                } else {
                    val cpuInfo = cpuManager.getCPUInfo()
                    if (!cpuInfo.isSupported) {
                        UIState.Error(cpuInfo.errorMessage ?: "This device is not supported")
                    } else {
                        _maxSpeed.value = cpuInfo.currMaxFreq.toFloat()
                        _minSpeed.value = cpuInfo.currMinFreq.toFloat()
                        UIState.Success(cpuInfo)
                    }
                }
            }
            _uiState.value = result
        }
    }
    
    fun updateMaxSpeed(value: Float) {
        val currentState = _uiState.value
        if (currentState is UIState.Success) {
            // Ensure max speed is not less than min speed
            if (value < _minSpeed.value) {
                _minSpeed.value = value
            }
            _maxSpeed.value = value
        }
    }
    
    fun updateMinSpeed(value: Float) {
        val currentState = _uiState.value
        if (currentState is UIState.Success) {
            // Ensure min speed is not greater than max speed
            if (value > _maxSpeed.value) {
                _maxSpeed.value = value
            }
            _minSpeed.value = value
        }
    }
    
    fun applyCPUSpeed(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                cpuManager.setCPUSpeed(
                    maxSpeed = _maxSpeed.value.toInt(),
                    minSpeed = _minSpeed.value.toInt()
                )
            }
            
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { onError(it.message ?: "Failed to set CPU speed") }
            )
        }
    }
    
    fun refreshCPUInfo() {
        checkDeviceCompatibility()
    }
    
    sealed class UIState {
        object Loading : UIState()
        data class Success(val cpuInfo: CPUManager.CPUInfo) : UIState()
        data class Error(val message: String) : UIState()
    }
}
