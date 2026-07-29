package com.yihengquan.cpuspeed.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists CPU speed and governor settings for apply-on-boot.
 */
class CPUPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cpuspeed_prefs", Context.MODE_PRIVATE)

    var maxSpeed: Int
        get() = prefs.getInt(KEY_MAX_SPEED, 0)
        set(value) = prefs.edit { putInt(KEY_MAX_SPEED, value) }

    var minSpeed: Int
        get() = prefs.getInt(KEY_MIN_SPEED, 0)
        set(value) = prefs.edit { putInt(KEY_MIN_SPEED, value) }

    var governor: String?
        get() = prefs.getString(KEY_GOVERNOR, null)
        set(value) = prefs.edit { putString(KEY_GOVERNOR, value) }

    var applyOnBoot: Boolean
        get() = prefs.getBoolean(KEY_APPLY_ON_BOOT, false)
        set(value) = prefs.edit { putBoolean(KEY_APPLY_ON_BOOT, value) }

    companion object {
        private const val KEY_MAX_SPEED = "max_speed"
        private const val KEY_MIN_SPEED = "min_speed"
        private const val KEY_GOVERNOR = "governor"
        private const val KEY_APPLY_ON_BOOT = "apply_on_boot"
    }
}
