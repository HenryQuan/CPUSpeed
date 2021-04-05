package com.yihengquan.cpuspeed.flutter

import android.content.Context
import android.widget.Toast
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

    init {
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "setup" -> setup(context)
                "info" -> getCPUInfo(context) { result.success(it) }
                else -> throw Error("Method not found, ${call.method}")
            }
        }
    }

    private fun setCPUSpeed(context: Context, maxSpeed: Int, minSpeed: Int) {
        // Get a list for commands
        val commands = ArrayList<String>()

        for (i in 0..numberOfCores) {
            // Max scaling
            var path = String.format(
                "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq",
                i
            )
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(
                String.format(
                    "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s",
                    path,
                    maxSpeed,
                    path,
                    path
                )
            )

            // Min scaling
            path = String.format(
                "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_min_freq",
                i
            )
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(
                String.format(
                    "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s",
                    path,
                    minSpeed,
                    path,
                    path
                )
            )

            // Max
            path = "/sys/module/msm_performance/parameters/cpu_max_freq"
            commands.add(
                String.format(
                    "chmod 644 %s\necho '%d:%d' > %s\nchmod 444 %s",
                    path,
                    i,
                    3000000,
                    path,
                    path
                )
            )
            // Min
            path = "/sys/module/msm_performance/parameters/cpu_min_freq"
            commands.add(
                String.format(
                    "chmod 644 %s\necho '%d:%d' > %s\nchmod 444 %s",
                    path,
                    i,
                    minSpeed,
                    path,
                    path
                )
            )
        }

        // System.out.println(commands.toString());
        runWithSU(
            context,
            commands.toTypedArray(),
            true,
            "Something went wrong"
        )
    }

    /// Check if device is rooted and update CPU folder permission
    private fun setup(context: Context) {
        checkIfRooted(context)
        setCPUFolderPermission(context)
    }

    private fun getCPUInfo(context: Context, callback: (HashMap<String, Any>) -> Unit) {
        val output = getOutputFromShell("su -c cat /sys/devices/system/cpu/cpu*/cpufreq/*m*_freq")
        output?.let {
            if (it.isNotEmpty()) {
                val shell: Array<String> = it.split("\n".toRegex()).toTypedArray()
                // System.out.println(output);
                val info = HashMap<String, Any>()

                // Find max values
//                for (i in 0 until numberOfCores) {
//                    // First and third values
//                    val maxInfoStr = shell[i * 4]
//                    val maxInfo = maxInfoStr.toInt()
//                    val maxCurr = shell[i * 4 + 2].toInt()
//                    val minCurr = shell[i * 4 + 3].toInt()
//                    if (maxInfo > maxFreqInfo) maxFreqInfo = maxInfo
//                    if (maxCurr > currMaxFreq) currMaxFreq = maxCurr
//                    if (minCurr > currMinFreq) currMinFreq = minCurr
//
//                    // Store max info
//                    var count = speedInfo[maxInfoStr]
//                    if (count == null) count = 0
//                    speedInfo[maxInfoStr] = count + 1
//                }
//
//                // Min and curr min are current, only max values could differ
//                minFreqInfo = shell[1].toInt()
//                freqDiff = maxFreqInfo - minFreqInfo
//                Toast.makeText(
//                    context,
//                    String.format("%d MHz - %d MHz", minFreqInfo, maxFreqInfo),
//                    Toast.LENGTH_SHORT
//                ).show()
//                minValue.text = String.format(Locale.ENGLISH, "%d MHz", currMinFreq)
//                maxValue.text = String.format(Locale.ENGLISH, "%d MHz", currMaxFreq)
//
//                // Set cpuInfo text
//                var infoStr = ""
//                for (key in speedInfo.keys) {
//                    val count: Int? = speedInfo[key]
//                    val speed = key.toFloat() / 1000000
//                    infoStr += String.format("%d x %.2f GHz\n", count, speed)
//                }
//                println(infoStr)

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