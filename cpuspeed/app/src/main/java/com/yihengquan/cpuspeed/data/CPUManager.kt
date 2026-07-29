package com.yihengquan.cpuspeed.data

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

/**
 * Manages CPU frequency operations via sysfs with root (su) escalation.
 * Extracted and modernised from the original Flutter method channel.
 */
class CPUManager(private val context: Context) {

    companion object {
        private const val TAG = "CPUManager"
        private const val MAX_PATH = "/sys/module/msm_performance/parameters/cpu_max_freq"
        private const val MIN_PATH = "/sys/module/msm_performance/parameters/cpu_min_freq"
    }

    private val places = arrayOf(
        "/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
        "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"
    )

    private val runtime = Runtime.getRuntime()
    val coreCount: Int get() = runtime.availableProcessors()

    // region Root detection

    /** Check if device is rooted by looking for su binary. */
    fun isRooted(): Boolean {
        for (place in places) {
            if (File("${place}su").exists()) return true
        }
        return false
    }

    /** Check for specific root solution. */
    fun hasSuperSU(): Boolean {
        return File("/system/app/SuperSU/SuperSU.apk").exists()
                || File("/system/app/SuperSU.apk").exists()
    }

    fun hasMagisk(): Boolean {
        return File("/sbin/magisk").exists()
                || File("/data/adb/magisk").exists()
    }

    // endregion

    // region CPU info

    /** Gather current CPU frequency state across all cores. */
    fun getCPUInfo(): CPUInfo {
        val cores = coreCount
        var maxFreqInfo = 0
        var currMaxFreq = 0
        var currMinFreq = Int.MAX_VALUE
        val speedInfo = LinkedHashMap<String, Int>()

        for (i in 0 until cores) {
            val maxInfoStr = readSysfsFile(
                "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq"
            ) ?: continue
            val minInfoStr = readSysfsFile(
                "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq"
            ) ?: continue
            val curMaxStr = readSysfsFile(
                "/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq"
            )
            val curMinStr = readSysfsFile(
                "/sys/devices/system/cpu/cpu$i/cpufreq/scaling_min_freq"
            )

            try {
                val maxInfo = maxInfoStr.trim().toInt()
                val curMax = curMaxStr?.trim()?.toInt() ?: 0
                val curMin = curMinStr?.trim()?.toInt() ?: 0

                if (maxInfo > maxFreqInfo) maxFreqInfo = maxInfo
                if (curMax > currMaxFreq) currMaxFreq = curMax
                if (curMin in 1 until currMinFreq) currMinFreq = curMin

                speedInfo[maxInfoStr.trim()] = (speedInfo[maxInfoStr.trim()] ?: 0) + 1
            } catch (_: NumberFormatException) {
                Log.w(TAG, "Failed to parse freq for core $i")
            }
        }

        if (currMinFreq == Int.MAX_VALUE) currMinFreq = 0
        val infoStr = speedInfo.entries.joinToString(" | ") { (freq, count) ->
            "${count}x ${formatFrequency(freq.toInt())}"
        }

        return CPUInfo(
            maxFrequency = maxFreqInfo,
            minFrequency = if (currMinFreq > 0) currMinFreq else 0,
            currMaxFrequency = currMaxFreq,
            currMinFrequency = currMinFreq,
            cpuInfo = infoStr.ifEmpty { "Unknown" },
            coreCount = cores,
            hasData = maxFreqInfo > 0 && currMinFreq > 0,
            coreFrequencies = getCoreFrequencies()
        )
    }

    /** Get current governor for core 0 (usually same across all cores). */
    fun getCurrentGovernor(): String {
        return readSysfsFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            ?.trim() ?: "unknown"
    }

    /** Get list of available governors. */
    fun getAvailableGovernors(): List<String> {
        return readSysfsFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors")
            ?.trim()?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    }

    /** Get available frequencies for core 0. */
    fun getAvailableFrequencies(): List<Int> {
        return readSysfsFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies")
            ?.trim()?.split(" ")
            ?.mapNotNull { it.toIntOrNull() }
            ?.sorted() ?: emptyList()
    }

    // endregion

    // region Set speed

    /** Read per-core frequencies. */
    fun getCoreFrequencies(): List<CoreFrequency> {
        val list = mutableListOf<CoreFrequency>()
        for (i in 0 until coreCount) {
            val maxStr = readSysfsFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            val minStr = readSysfsFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
            val curStr = readSysfsFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            list.add(
                CoreFrequency(
                    core = i,
                    maxFreq = maxStr?.trim()?.toIntOrNull() ?: 0,
                    minFreq = minStr?.trim()?.toIntOrNull() ?: 0,
                    curFreq = curStr?.trim()?.toIntOrNull() ?: 0,
                )
            )
        }
        return list
    }

    /** Set speed for a single core via root. */
    fun setCoreSpeed(core: Int, maxSpeed: Int, minSpeed: Int, callback: (Boolean, String) -> Unit) {
        val commands = arrayOf(
            buildScalingCommand(core, maxSpeed, max = true),
            buildScalingCommand(core, minSpeed, max = false)
        )
        runWithSU(commands) { success, msg ->
            if (success) setPermissions(core)
            callback(success, msg)
        }
    }

    /** Set max and min CPU speed (kHz) across all cores via root. */
    fun setCPUSpeed(maxSpeed: Int, minSpeed: Int, callback: (Boolean, String) -> Unit) {
        val commands = ArrayList<String>()

        for (core in 0 until coreCount) {
            commands.add(buildScalingCommand(core, maxSpeed, max = true))
            commands.add(buildScalingCommand(core, minSpeed, max = false))
        }

        // Also try the MSM performance parameter path
        commands.add(buildPerformanceParameterCommand(maxSpeed, max = true))
        commands.add(buildPerformanceParameterCommand(minSpeed, max = false))

        runWithSU(commands.toTypedArray()) { success, msg ->
            if (success) {
                // Restore permissions on all CPU freq files
                for (core in 0 until coreCount) {
                    setPermissions(core)
                }
            }
            callback(success, msg)
        }
    }

    /** Set CPU governor via root. */
    fun setGovernor(governor: String, callback: (Boolean, String) -> Unit) {
        val commands = mutableListOf<String>()
        for (core in 0 until coreCount) {
            val path = "/sys/devices/system/cpu/cpu$core/cpufreq/scaling_governor"
            commands.add("chmod 644 $path")
            commands.add("echo '$governor' > $path")
            commands.add("chmod 444 $path")
        }
        runWithSU(commands.toTypedArray(), callback)
    }

    private fun buildScalingCommand(core: Int, speed: Int, max: Boolean): String {
        val path = "/sys/devices/system/cpu/cpu$core/cpufreq/scaling_${if (max) "max" else "min"}_freq"
        return "chmod 644 $path\necho \"$speed\" > $path\nchmod 444 $path"
    }

    private fun buildPerformanceParameterCommand(speed: Int, max: Boolean): String {
        val path = if (max) MAX_PATH else MIN_PATH
        val sb = StringBuilder()
        for (core in 0 until coreCount) {
            sb.append("chmod 644 $path\n")
            sb.append("echo '$core:$speed' > $path\n")
            sb.append("chmod 444 $path\n")
        }
        return sb.toString()
    }

    private fun setPermissions(core: Int) {
        runWithSU(
            arrayOf("chmod 444 /sys/devices/system/cpu/cpu$core/cpufreq/scaling_max_freq"),
            showSuccess = false
        )
    }

    /** Apply saved settings on boot. */
    fun applyOnBoot(maxSpeed: Int, minSpeed: Int, governor: String?) {
        setCPUFolderPermission()
        setCPUSpeed(maxSpeed, minSpeed) { _, _ -> }
        if (!governor.isNullOrBlank()) {
            setGovernor(governor) { _, _ -> }
        }
    }

    // endregion

    // region Root shell

    private fun setCPUFolderPermission() {
        runWithSU(
            arrayOf("chmod 755 /sys/devices/system/cpu/cpu*"),
            errorMessage = "Failed to update folders' permission",
            showSuccess = false
        )
    }

    private fun runWithSU(
        commands: Array<String>,
        showSuccess: Boolean = false,
        errorMessage: String? = null,
        onResult: ((Boolean, String) -> Unit)? = null,
    ) {
        try {
            val su = runtime.exec("su")
            val terminal = DataOutputStream(su.outputStream)
            for (command in commands) {
                terminal.writeBytes(command)
                terminal.flush()
            }
            terminal.close()

            if (showSuccess) {
                onResult?.invoke(true, "Success")
            } else {
                onResult?.invoke(true, "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "su command failed", e)
            onResult?.invoke(false, errorMessage ?: e.localizedMessage ?: "Root command failed")
        }
    }

    // endregion

    // region Shell helpers

    private fun readSysfsFile(path: String): String? {
        return try {
            File(path).takeIf { it.exists() }?.readText()?.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read $path", e)
            null
        }
    }

    /** Run a shell command and return output. */
    fun getOutputFromShell(command: String): String? {
        return try {
            val p = runtime.exec(command)
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val output = StringBuilder()
            val buffer = CharArray(4096)
            var read: Int
            while (reader.read(buffer).also { read = it } > 0) {
                output.append(buffer, 0, read)
            }
            reader.close()
            p.waitFor()
            output.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Shell command failed: $command", e)
            null
        }
    }

    // endregion

    companion object {
        fun formatFrequency(kHz: Int): String {
            return when {
                kHz >= 1_000_000 -> String.format("%.2f GHz", kHz / 1_000_000f)
                kHz >= 1_000 -> String.format("%.2f MHz", kHz / 1_000f)
                else -> "$kHz KHz"
            }
        }
    }
}
