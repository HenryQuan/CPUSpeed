package com.yihengquan.cpuspeed.flutter

import android.content.Context
import android.widget.Toast
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

class CPUMethodChannel(context: Context) : BaseMethodChannel(context) {
    override val name: String = "cpu"
    private val channel: MethodChannel by lazy(this::setupChannel)

    private val places = arrayOf(
        "/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
        "/data/local/bin/", "/system/sd/xbin/",
        "/system/bin/failsafe/", "/data/local/"
    )

    private val binaries = arrayOf(
        "su", "busybox"
    )

    private val runtime = Runtime.getRuntime()
    private val numberOfCores = runtime.availableProcessors()

    companion object {
        private const val maxPath = "/sys/module/msm_performance/parameters/cpu_max_freq"
        private const val minPath = "/sys/module/msm_performance/parameters/cpu_min_freq"
    }

    init {
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "setup" -> setup(context)
                "info" -> getCPUInfo(context) { result.success(it) }
                "setSpeed" -> setCPUSpeed(context, call)
                else -> throw Error("Method not found, ${call.method}")
            }
        }
    }

    /// Check if device is rooted and update CPU folder permission
    private fun setup(context: Context) {
        checkIfRooted(context)
        setCPUFolderPermission(context)
    }

    private fun getCPUInfo(context: Context, callback: (HashMap<String, Any>?) -> Unit) {
        // This will print all cpu max, min and curr max and curr min speed
        getOutputFromShell(
            "su -c cat /sys/devices/system/cpu/cpu*/cpufreq/*m*_freq"
        )?.let {
            if (it.isNotEmpty()) {
                val list = it.split("\n".toRegex()).toTypedArray()
                // System.out.println(speedList);
                val info = HashMap<String, Any>()
                val speedInfo = HashMap<String, Int>()

                var maxFreqInfo = 0
                var currMaxFreq = 0
                var currMinFreq = 0

                // Find max values
                for (i in 0 until numberOfCores) {
                    // First and third values
                    val maxInfoStr = list[i * 4]
                    val maxInfo = maxInfoStr.toInt()
                    val maxCurr = list[i * 4 + 2].toInt()
                    val minCurr = list[i * 4 + 3].toInt()
                    if (maxInfo > maxFreqInfo) maxFreqInfo = maxInfo
                    if (maxCurr > currMaxFreq) currMaxFreq = maxCurr
                    if (minCurr > currMinFreq) currMinFreq = minCurr

                    // Store max info
                    var count = speedInfo[maxInfoStr]
                    if (count == null) count = 0
                    speedInfo[maxInfoStr] = count + 1
                }

                // Min and curr min don't change, only max values could differ
                // TODO: it is possible to have different min speed
                val minFreqInfo = list[1].toInt()
                Toast.makeText(
                    context,
                    String.format("%d MHz - %d MHz", minFreqInfo, maxFreqInfo),
                    Toast.LENGTH_SHORT
                ).show()

                // Set cpuInfo text
                var infoStr = ""
                for (key in speedInfo.keys) {
                    val count: Int? = speedInfo[key]
                    val speed = key.toFloat() / 1000000
                    infoStr += String.format("%d x %.2f GHz\n", count, speed)
                }
                println(infoStr)

                // Pass data back to flutter
                info["info"] = infoStr
                info["cpu"] = speedInfo
                info["max"] = maxFreqInfo
                info["min"] = minFreqInfo
                info["max_curr"] = currMaxFreq
                info["min_curr"] = currMinFreq
                callback(info)
            } else {
                Toast.makeText(
                    context,
                    "Please contact the developer for more info",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setCPUSpeed(context: Context, call: MethodCall) {
        setCPUSpeed(
            context,
            call.argument<Int>("max")!!,
            call.argument<Int>("min")!!
        )
    }

    private fun setCPUSpeed(context: Context, maxSpeed: Int, minSpeed: Int) {
        // Get a list for commands
        val commands = ArrayList<String>()

        for (core in 0 until numberOfCores) {
            commands.add(getScalingCommand(core, maxSpeed, max = true))
            commands.add(getScalingCommand(core, minSpeed, max = false))
        }

        commands.add(getPerformanceParameterCommand(maxSpeed, max = true))
        commands.add(getPerformanceParameterCommand(minSpeed, max = false))

        runWithSU(
            context,
            commands.toTypedArray(),
            true,
            "Something went wrong"
        )
    }

    // region Command
    /// For example, "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"
    private fun getScalingCommand(core: Int, speed: Int, max: Boolean): String {
        // Decide the path based on core and max/min
        val path = String.format(
            "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_%s_freq",
            core, if (max) "max" else "min"
        )

        // Change to 644 for changing value and then change it back (based on Kernel Adiutor)
        return arrayOf(
            "chmod 644 $path",
            "echo \"$speed\" > $path",
            "chmod 444 $path",
            ""
        ).joinToString(separator = "\n")
    }

    /// For example, "/sys/module/msm_performance/parameters/cpu_min_freq"
    private fun getPerformanceParameterCommand(speed: Int, max: Boolean): String {
        val path = if (max) maxPath else minPath
        var command = ""
        for (core in 0 until numberOfCores) {
            command += arrayOf(
                "chmod 644 $path",
                "echo '$core:$speed' > $path",
                "chmod 444 $path",
                ""
            ).joinToString(separator = "\n")
        }

        return command
    }
    // endregion

    // region Helpers
    private fun checkIfRooted(context: Context) {
        for (binary in binaries) {
            if (findBinary(binary)) return
        }

        // Show a toast only if none of the binaries was found
        Toast.makeText(context, "Device might not be rooted", Toast.LENGTH_LONG).show()
    }

    private fun setCPUFolderPermission(context: Context) {
        runWithSU(
            context,
            arrayOf("\"chmod 755 /sys/devices/system/cpu/cpu*\""),
            false,
            "Failed to update folders' permission"
        )
    }

    private fun runWithSU(
        context: Context,
        commands: Array<String>,
        showSuccessMessage: Boolean = false,
        errorMessage: String = "Failed to run commands"
    ) {
        val su = runtime.exec("su")
        val terminal = DataOutputStream(su.outputStream)
        try {
            for (command in commands) {
                terminal.writeBytes(command)
                terminal.flush()
            }
            terminal.close()

            if (showSuccessMessage) {
                Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                errorMessage,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /// From https://stackoverflow.com/a/23608939, maybe
    private fun getOutputFromShell(command: String): String? {
        try {
            val p = runtime.exec(command)
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            var read: Int
            val buffer = CharArray(4096)
            val output = StringBuffer()
            while (reader.read(buffer).also { read = it } > 0) {
                output.append(buffer, 0, read)
            }
            reader.close()
            p.waitFor()
            return output.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /// Find binary from https://stackoverflow.com/a/19289543
    private fun findBinary(binaryName: String): Boolean {
        for (path in places) {
            if (File(path + binaryName).exists()) {
                return true
            }
        }
        return false
    }
    // endregion
}