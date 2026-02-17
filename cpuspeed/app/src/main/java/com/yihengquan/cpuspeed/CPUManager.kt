package com.yihengquan.cpuspeed

import android.util.Log

/**
 * Manager class for CPU frequency and power operations
 * Prepared for future governor and power management features
 */
class CPUManager(private val rootManager: RootManager = RootManager()) {
    private val numberOfCores = Runtime.getRuntime().availableProcessors()
    
    companion object {
        private const val TAG = "CPUManager"
        
        // CPU frequency paths
        private const val CPU_BASE_PATH = "/sys/devices/system/cpu"
        private const val SCALING_MAX_FREQ = "cpufreq/scaling_max_freq"
        private const val SCALING_MIN_FREQ = "cpufreq/scaling_min_freq"
        private const val SCALING_GOVERNOR = "cpufreq/scaling_governor"
        private const val SCALING_AVAILABLE_GOVERNORS = "cpufreq/scaling_available_governors"
        private const val CPUINFO_MAX_FREQ = "cpufreq/cpuinfo_max_freq"
        private const val CPUINFO_MIN_FREQ = "cpufreq/cpuinfo_min_freq"
        private const val CPUINFO_CUR_FREQ = "cpufreq/cpuinfo_cur_freq"
        
        // Performance parameters (Qualcomm-specific)
        private const val MSM_PERFORMANCE_PATH = "/sys/module/msm_performance/parameters"
        private const val MSM_MAX_FREQ = "$MSM_PERFORMANCE_PATH/cpu_max_freq"
        private const val MSM_MIN_FREQ = "$MSM_PERFORMANCE_PATH/cpu_min_freq"
    }
    
    data class CPUInfo(
        val maxFreqInfo: Int,
        val minFreqInfo: Int,
        val currMaxFreq: Int,
        val currMinFreq: Int,
        val speedInfo: Map<String, Int>,
        val currentGovernor: String? = null,
        val availableGovernors: List<String>? = null,
        val isSupported: Boolean = true,
        val errorMessage: String? = null
    )
    
    /**
     * Check if device is rooted
     */
    fun isDeviceRooted(): Boolean {
        return rootManager.isRootAvailable()
    }
    
    /**
     * Get comprehensive CPU information
     */
    fun getCPUInfo(): CPUInfo {
        try {
            // Set CPU folder permissions first
            setCPUFolderPermissions()
            
            val output = rootManager.executeCommand(
                "cat $CPU_BASE_PATH/cpu*/cpufreq/*m*_freq"
            ).getOrNull()
            
            if (output.isNullOrEmpty()) {
                return CPUInfo(
                    maxFreqInfo = 0,
                    minFreqInfo = 0,
                    currMaxFreq = 0,
                    currMinFreq = 0,
                    speedInfo = emptyMap(),
                    isSupported = false,
                    errorMessage = "Unable to read CPU frequency information. This device may not be supported."
                )
            }
            
            val lines = output.split("\n").filter { it.isNotBlank() }
            val speedInfo = mutableMapOf<String, Int>()
            
            var maxFreqInfo = 0
            var currMaxFreq = 0
            var currMinFreq = 0
            
            // Validate output format (expecting 4 lines per core: max_freq, min_freq, scaling_max_freq, scaling_min_freq)
            val expectedLines = numberOfCores * 4
            if (lines.size < expectedLines) {
                return CPUInfo(
                    maxFreqInfo = 0,
                    minFreqInfo = 0,
                    currMaxFreq = 0,
                    currMinFreq = 0,
                    speedInfo = emptyMap(),
                    isSupported = false,
                    errorMessage = "Unexpected CPU frequency data format. Expected $expectedLines lines, got ${lines.size}. This device may not be supported."
                )
            }
            
            // Parse CPU frequency info
            for (i in 0 until numberOfCores) {
                try {
                    val maxInfoStr = lines[i * 4]
                    val maxInfo = maxInfoStr.toInt()
                    val maxCurr = lines[i * 4 + 2].toInt()
                    val minCurr = lines[i * 4 + 3].toInt()
                    
                    if (maxInfo > maxFreqInfo) maxFreqInfo = maxInfo
                    if (maxCurr > currMaxFreq) currMaxFreq = maxCurr
                    if (minCurr > currMinFreq) currMinFreq = minCurr
                    
                    // Store frequency distribution
                    val count = speedInfo[maxInfoStr] ?: 0
                    speedInfo[maxInfoStr] = count + 1
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing CPU info for core $i", e)
                }
            }
            
            val minFreqInfo = lines[1].toInt()
            
            // Get governor information (for future use)
            val currentGovernor = getCurrentGovernor()
            val availableGovernors = getAvailableGovernors()
            
            return CPUInfo(
                maxFreqInfo = maxFreqInfo,
                minFreqInfo = minFreqInfo,
                currMaxFreq = currMaxFreq,
                currMinFreq = currMinFreq,
                speedInfo = speedInfo,
                currentGovernor = currentGovernor,
                availableGovernors = availableGovernors
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting CPU info", e)
            return CPUInfo(
                maxFreqInfo = 0,
                minFreqInfo = 0,
                currMaxFreq = 0,
                currMinFreq = 0,
                speedInfo = emptyMap(),
                isSupported = false,
                errorMessage = "Error reading CPU information: ${e.message}"
            )
        }
    }
    
    /**
     * Set CPU frequency limits
     */
    fun setCPUSpeed(maxSpeed: Int, minSpeed: Int): Result<Unit> {
        return try {
            val commands = mutableListOf<String>()
            
            // Generate commands for each core
            for (core in 0 until numberOfCores) {
                commands.addAll(getScalingCommands(core, maxSpeed, minSpeed))
            }
            
            // Add performance parameter commands (Qualcomm-specific)
            if (rootManager.fileExists(MSM_MAX_FREQ)) {
                commands.addAll(getPerformanceParameterCommands(maxSpeed, minSpeed))
            }
            
            rootManager.executeCommands(commands)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting CPU speed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current CPU governor (prepared for future governor control)
     */
    private fun getCurrentGovernor(): String? {
        return rootManager.readFile("$CPU_BASE_PATH/cpu0/$SCALING_GOVERNOR")
            .getOrNull()?.trim()
    }
    
    /**
     * Get available CPU governors (prepared for future governor control)
     */
    private fun getAvailableGovernors(): List<String>? {
        return rootManager.readFile("$CPU_BASE_PATH/cpu0/$SCALING_AVAILABLE_GOVERNORS")
            .getOrNull()?.trim()?.split("\\s+".toRegex())
    }
    
    /**
     * Set CPU governor (prepared for future implementation)
     */
    @Suppress("unused")
    fun setCPUGovernor(governor: String): Result<Unit> {
        val commands = mutableListOf<String>()
        for (core in 0 until numberOfCores) {
            val path = "$CPU_BASE_PATH/cpu$core/$SCALING_GOVERNOR"
            commands.add("chmod 644 $path")
            commands.add("echo '$governor' > $path")
            commands.add("chmod 444 $path")
        }
        return rootManager.executeCommands(commands)
    }
    
    private fun getScalingCommands(core: Int, maxSpeed: Int, minSpeed: Int): List<String> {
        val commands = mutableListOf<String>()
        val basePath = "$CPU_BASE_PATH/cpu$core"
        
        // Max frequency
        val maxPath = "$basePath/$SCALING_MAX_FREQ"
        commands.add("chmod 644 $maxPath")
        commands.add("echo '$maxSpeed' > $maxPath")
        commands.add("chmod 444 $maxPath")
        
        // Min frequency
        val minPath = "$basePath/$SCALING_MIN_FREQ"
        commands.add("chmod 644 $minPath")
        commands.add("echo '$minSpeed' > $minPath")
        commands.add("chmod 444 $minPath")
        
        return commands
    }
    
    private fun getPerformanceParameterCommands(maxSpeed: Int, minSpeed: Int): List<String> {
        val commands = mutableListOf<String>()
        
        for (core in 0 until numberOfCores) {
            // Max frequency
            commands.add("chmod 644 $MSM_MAX_FREQ")
            commands.add("echo '$core:$maxSpeed' > $MSM_MAX_FREQ")
            commands.add("chmod 444 $MSM_MAX_FREQ")
            
            // Min frequency
            commands.add("chmod 644 $MSM_MIN_FREQ")
            commands.add("echo '$core:$minSpeed' > $MSM_MIN_FREQ")
            commands.add("chmod 444 $MSM_MIN_FREQ")
        }
        
        return commands
    }
    
    private fun setCPUFolderPermissions() {
        try {
            rootManager.setPermissions("$CPU_BASE_PATH/cpu*", "755")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update CPU folder permissions", e)
        }
    }
}
