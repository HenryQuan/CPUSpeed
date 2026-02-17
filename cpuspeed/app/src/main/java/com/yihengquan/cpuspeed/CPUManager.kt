package com.yihengquan.cpuspeed

import android.util.Log
import java.io.*

/**
 * Manager class for CPU frequency operations
 */
class CPUManager {
    private val numberOfCores = Runtime.getRuntime().availableProcessors()
    
    private val places = arrayOf(
        "/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
        "/data/local/bin/", "/system/sd/xbin/",
        "/system/bin/failsafe/", "/data/local/"
    )
    
    private val binaries = arrayOf("su", "busybox")
    
    companion object {
        private const val TAG = "CPUManager"
        private const val maxPath = "/sys/module/msm_performance/parameters/cpu_max_freq"
        private const val minPath = "/sys/module/msm_performance/parameters/cpu_min_freq"
    }
    
    data class CPUInfo(
        val maxFreqInfo: Int,
        val minFreqInfo: Int,
        val currMaxFreq: Int,
        val currMinFreq: Int,
        val speedInfo: Map<String, Int>,
        val isSupported: Boolean = true,
        val errorMessage: String? = null
    )
    
    /**
     * Check if device is rooted
     */
    fun isDeviceRooted(): Boolean {
        for (binary in binaries) {
            if (findBinary(binary)) return true
        }
        return false
    }
    
    /**
     * Get CPU information
     */
    fun getCPUInfo(): CPUInfo {
        try {
            // Set CPU folder permission first
            setCPUFolderPermission()
            
            val output = getOutputFromShell("su -c cat /sys/devices/system/cpu/cpu*/cpufreq/*m*_freq")
            
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
            
            val shell = output.split("\n".toRegex()).toTypedArray()
            val speedInfo = mutableMapOf<String, Int>()
            
            var maxFreqInfo = 0
            var currMaxFreq = 0
            var currMinFreq = 0
            
            // Validate output format
            val expectedLines = numberOfCores * 4
            if (shell.size < expectedLines) {
                return CPUInfo(
                    maxFreqInfo = 0,
                    minFreqInfo = 0,
                    currMaxFreq = 0,
                    currMinFreq = 0,
                    speedInfo = emptyMap(),
                    isSupported = false,
                    errorMessage = "Unexpected CPU frequency data format. This device may not be supported."
                )
            }
            
            // Find max values
            for (i in 0 until numberOfCores) {
                try {
                    val maxInfoStr = shell[i * 4]
                    val maxInfo = maxInfoStr.toInt()
                    val maxCurr = shell[i * 4 + 2].toInt()
                    val minCurr = shell[i * 4 + 3].toInt()
                    
                    if (maxInfo > maxFreqInfo) maxFreqInfo = maxInfo
                    if (maxCurr > currMaxFreq) currMaxFreq = maxCurr
                    if (minCurr > currMinFreq) currMinFreq = minCurr
                    
                    // Store max info
                    val count = speedInfo[maxInfoStr] ?: 0
                    speedInfo[maxInfoStr] = count + 1
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing CPU info for core $i", e)
                }
            }
            
            val minFreqInfo = shell[1].toInt()
            
            return CPUInfo(
                maxFreqInfo = maxFreqInfo,
                minFreqInfo = minFreqInfo,
                currMaxFreq = currMaxFreq,
                currMinFreq = currMinFreq,
                speedInfo = speedInfo
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
     * Set CPU frequency
     */
    fun setCPUSpeed(maxSpeed: Int, minSpeed: Int): Result<Unit> {
        return try {
            val commands = mutableListOf<String>()
            
            for (core in 0 until numberOfCores) {
                commands.add(getScalingCommand(core, maxSpeed, max = true))
                commands.add(getScalingCommand(core, minSpeed, max = false))
            }
            
            commands.add(getPerformanceParameterCommand(maxSpeed, max = true))
            commands.add(getPerformanceParameterCommand(minSpeed, max = false))
            
            runWithSU(commands.toTypedArray())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting CPU speed", e)
            Result.failure(e)
        }
    }
    
    private fun getScalingCommand(core: Int, speed: Int, max: Boolean): String {
        val path = "/sys/devices/system/cpu/cpu$core/cpufreq/scaling_${if (max) "max" else "min"}_freq"
        return """
            chmod 644 $path
            echo "$speed" > $path
            chmod 444 $path
            
        """.trimIndent()
    }
    
    private fun getPerformanceParameterCommand(speed: Int, max: Boolean): String {
        val path = if (max) maxPath else minPath
        var command = ""
        for (core in 0 until numberOfCores) {
            command += """
                chmod 644 $path
                echo '$core:$speed' > $path
                chmod 444 $path
                
            """.trimIndent()
        }
        return command
    }
    
    private fun setCPUFolderPermission() {
        try {
            runWithSU(arrayOf("chmod 755 /sys/devices/system/cpu/cpu*"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update folders' permission", e)
        }
    }
    
    private fun runWithSU(commands: Array<String>) {
        val su = Runtime.getRuntime().exec("su")
        val terminal = DataOutputStream(su.outputStream)
        for (command in commands) {
            terminal.writeBytes(command)
            terminal.flush()
        }
        terminal.close()
    }
    
    private fun getOutputFromShell(command: String): String? {
        return try {
            val p = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            var read: Int
            val buffer = CharArray(4096)
            val output = StringBuffer()
            while (reader.read(buffer).also { read = it } > 0) {
                output.append(buffer, 0, read)
            }
            reader.close()
            p.waitFor()
            output.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting output from shell", e)
            null
        }
    }
    
    private fun findBinary(binaryName: String): Boolean {
        for (path in places) {
            if (File(path + binaryName).exists()) {
                return true
            }
        }
        return false
    }
}
