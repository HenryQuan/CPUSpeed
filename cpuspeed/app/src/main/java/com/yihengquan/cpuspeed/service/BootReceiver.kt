package com.yihengquan.cpuspeed.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yihengquan.cpuspeed.data.CPUManager
import com.yihengquan.cpuspeed.data.CPUPrefs

/**
 * Applies saved CPU settings after device boot.
 * Triggered by ACTION_BOOT_COMPLETED (manifest-registered).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = CPUPrefs(context)
        if (!prefs.applyOnBoot) return

        val cpuManager = CPUManager(context)
        cpuManager.applyOnBoot(
            maxSpeed = prefs.maxSpeed,
            minSpeed = prefs.minSpeed,
            governor = prefs.governor
        )
    }
}
