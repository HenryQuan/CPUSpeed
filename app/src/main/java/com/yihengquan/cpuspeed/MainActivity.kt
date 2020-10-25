package com.yihengquan.cpuspeed

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.junit.runner.RunWith
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private var maxFreqInfo = 0
    private var minFreqInfo = 0
    private var freqDiff = 0
    private val core = Runtime.getRuntime().availableProcessors()
    private var currMaxFreq = 0
    private var currMinFreq = 0
    private val appVersion: String? = "1.0.7"
    private var menu: Menu? = null
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val m = menuInflater
        m.inflate(R.menu.menu_main, menu)
        this.menu = menu
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Show toolbar
        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (!showWelcomeDialog()) {
            showWhatsNewDialog()
        }
        if (!findBinary("su") && !findBinary("busybox")) {
            Toast.makeText(this, "Device is not rooted", Toast.LENGTH_LONG).show()
        } else {
            // Make sure cpu folders have the right permission
            try {
                val p = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(p.outputStream)
                os.writeBytes("chmod 755 /sys/devices/system/cpu/cpu*")
                os.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val output = getOutputFromShell("su -c cat /sys/devices/system/cpu/cpu*/cpufreq/*m*_freq")
            if (output != null && !output.isEmpty()) {
                // Update freq when seek bar changed
                val maxFreq = findViewById<SeekBar?>(R.id.maxFreq)
                val minFreq = findViewById<SeekBar?>(R.id.minFreq)

                // Catch errors...
                try {
                    val shell: Array<String?> = output.split("\n".toRegex()).toTypedArray()
                    //System.out.println(output);
                    // Store cpu info
                    val speedInfo = HashMap<String?, Int?>()

                    // Find max values
                    for (i in 0 until core) {
                        // First and third values
                        val maxInfoStr = shell[i * 4]
                        val maxInfo = maxInfoStr.toInt()
                        val maxCurr = shell[i * 4 + 2].toInt()
                        val minCurr = shell[i * 4 + 3].toInt()
                        if (maxInfo > maxFreqInfo) maxFreqInfo = maxInfo
                        if (maxCurr > currMaxFreq) currMaxFreq = maxCurr
                        if (minCurr > currMinFreq) currMinFreq = minCurr

                        // Store max info
                        var count = speedInfo[maxInfoStr]
                        if (count == null) count = 0
                        speedInfo[maxInfoStr] = count + 1
                    }

                    // Min and curr min are current, only max values could differ
                    minFreqInfo = shell[1].toInt()
                    freqDiff = maxFreqInfo - minFreqInfo
                    Toast.makeText(this, String.format(Locale.ENGLISH, "%d MHz - %d MHz", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show()
                    val minValue = findViewById<TextView?>(R.id.minFreqValue)
                    minValue.text = String.format(Locale.ENGLISH, "%d MHz", currMinFreq)
                    val maxValue = findViewById<TextView?>(R.id.maxFreqValue)
                    maxValue.text = String.format(Locale.ENGLISH, "%d MHz", currMaxFreq)

                    // Set cpuInfo text
                    var infoStr = ""
                    for (key in speedInfo.keys) {
                        val count: Int = speedInfo[key]
                        val speed = key.toFloat() / 1000000
                        infoStr += String.format("%d x %.2f GHz\n", count, speed)
                    }
                    println(infoStr)
                    val info = findViewById<TextView?>(R.id.cpuInfo)
                    info.text = infoStr

                    // Update progress (remember to x100 first)
                    maxFreq.progress = (currMaxFreq - minFreqInfo) * 100 / freqDiff
                    minFreq.progress = (currMinFreq - minFreqInfo) * 100 / freqDiff
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val maxValue = findViewById<TextView?>(R.id.maxFreqValue)
                val minValue = findViewById<TextView?>(R.id.minFreqValue)
                maxFreq.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        currMaxFreq = minFreqInfo + freqDiff * progress / 100
                        // Max has to be greater than or equal to min
                        if (currMaxFreq < currMinFreq) {
                            // Also move minFreq
                            minFreq.progress = progress
                            minValue.text = String.format(Locale.ENGLISH, "%d MHz", currMaxFreq)
                        }
                        maxValue.text = String.format(Locale.ENGLISH, "%d MHz", currMaxFreq)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
                minFreq.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        currMinFreq = minFreqInfo + freqDiff * progress / 100
                        // Min has to be less than or equal to max
                        if (currMinFreq > currMaxFreq) {
                            // Move maxFreq
                            maxFreq.progress = progress
                            maxValue.text = String.format(Locale.ENGLISH, "%d MHz", currMaxFreq)
                        }
                        minValue.text = String.format(Locale.ENGLISH, "%d MHz", currMinFreq)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            } else {
                Toast.makeText(this, "Please contact me for more info", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Show github page when pressed
     * @param item
     */
    fun showAbout(item: MenuItem?) {
        AlertDialog.Builder(this)
                .setTitle("CPUSpeed")
                .setMessage("It aims to help you set CPUSpeed easily for rooted android devices. Please visit my Github repository for more info.") // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton("Github") { dialog, which -> openLink("https://github.com/HenryQuan/CPUSpeed") }
                .setNeutralButton("Privacy Policy") { dialog, which -> openLink("https://github.com/HenryQuan/CPUSpeed/blob/master/Privacy%20Policy.md") }
                .setCancelable(true)
                .show()
    }

    /**
     * Share app with a popup
     * @param item
     */
    fun shareApp(item: MenuItem?) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "text/plain"
        share.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=com.yihengquan.cpuspeed")
        startActivity(Intent.createChooser(share, "Share CPUSpeed"))
    }

    /**
     * Show or hide ads
     * @param item
     */
    fun downloadKernelEditor(item: MenuItem?) {
        openLink("https://github.com/HenryQuan/CPUSpeed/blob/master/Privacy%20Policy.md")
    }

    /**
     * Email me with feed back
     * @param item
     */
    fun emailMe(item: MenuItem?) {
        openLink(String.format("mailto:development.henryquan@gmail.com?subject=[CPUSpeed %s] ", appVersion))
    }

    /**
     * Open certain links
     * @param url
     */
    private fun openLink(url: String?) {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show a simple dialog to say hello and some warnings
     */
    private fun showWelcomeDialog(): Boolean {
        val pref = getSharedPreferences("CPUSpeed", MODE_PRIVATE)
        val editor = pref.edit()
        val first = pref.getBoolean("Welcome", true)
        if (first) {
            AlertDialog.Builder(this)
                    .setTitle("CPUSpeed")
                    .setMessage("Thank you for downloading this app.\n\nPlease note that if you under clock your device, it might freeze or even shutdown in the worst case. If you overclock your device, it might become warm and battery will run out quickly.\n\nThis app might not work on your device. In this case, you can use other apps.") // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false)
                    .show()
            // Set to false
            editor.putBoolean("Welcome", false)
            editor.apply()
        }
        return first
    }

    /**
     * Show what's new in this version
     */
    private fun showWhatsNewDialog() {
        val pref = getSharedPreferences("CPUSpeed", MODE_PRIVATE)
        val editor = pref.edit()
        val whatsnew = pref.getBoolean(appVersion, true)
        if (whatsnew) {
            AlertDialog.Builder(this)
                    .setTitle(String.format("Version %s", appVersion))
                    .setMessage("Thank you for 25k downloads. This app was written because other apps weren't working for me and paid.\nNow, I do not have any rooted devices and this app will not be developed anymore unless I need it again. Thank you for all your support and all your suggestions.\n- Updated to androidx\n- Removed ads") // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false)
                    .show()
            // Set to false
            editor.putBoolean(appVersion, false)
            editor.apply()
        }
    }

    /**
     * Button clicked
     * @param view
     */
    fun setSpeed(view: View?) {
        if (maxFreqInfo == 0 || minFreqInfo == 0) {
            Toast.makeText(this, String.format(Locale.ENGLISH, "Error: unknown clock speed", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show()
        } else {
            setCPUSpeed(currMaxFreq, currMinFreq, core)
        }
    }

    /**
     * Set CPU frequency
     * @param maxSpeed
     * @param minSpeed
     * @param core
     */
    private fun setCPUSpeed(maxSpeed: Int, minSpeed: Int, core: Int) {
        // Get a list for commands
        var maxSpeed = maxSpeed
        val commands = ArrayList<String?>()
        var i: Int
        i = 0
        while (i < core) {

            // Max
            var path = String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq", i)
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s", path, maxSpeed, path, path))

            // Min
            path = String.format(Locale.ENGLISH, "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_min_freq", i)
            // Change to 644 for changing value and then change it back (from Kernel Adiutor)
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho \"%d\" > %s\nchmod 444 %s", path, minSpeed, path, path))
            i++
        }
        i = 0
        while (i < core) {
            val path = "/sys/module/msm_performance/parameters/cpu_max_freq"
            // Max
            maxSpeed = 3000000
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho '%d:%d' > %s\nchmod 444 %s", path, i, maxSpeed, path, path))
            i++
        }
        i = 0
        while (i < core) {
            val path = "/sys/module/msm_performance/parameters/cpu_min_freq"
            // Min
            commands.add(String.format(Locale.ENGLISH, "chmod 644 %s\necho '%d:%d' > %s\nchmod 444 %s", path, i, minSpeed, path, path))
            i++
        }

        // System.out.println(commands.toString());
        try {
            // Try to get root and run the script
            val c = commands.toTypedArray()
            val p = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(p.outputStream)
            for (cmd in c) {
                os.writeBytes("""
    $cmd

    """.trimIndent())
                os.flush()
            }
            Toast.makeText(this, String.format(Locale.ENGLISH, "Success", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, String.format(Locale.ENGLISH, "Something went wrong", minFreqInfo, maxFreqInfo), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        /**
         * Get output by running command
         * @param command
         * @return
         */
        private fun getOutputFromShell(command: String?): String? {
            try {
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
                return output.toString()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            return ""
        }

        /**
         * Find binary from https://stackoverflow.com/questions/19288463/how-to-check-if-android-phone-is-rooted#19289543
         * @param binaryName
         * @return
         */
        private fun findBinary(binaryName: String?): Boolean {
            var found = false
            val places = arrayOf<String?>("/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                    "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/")
            for (where in places) {
                if (File(where + binaryName).exists()) {
                    found = true
                    break
                }
            }
            return found
        }
    }
}