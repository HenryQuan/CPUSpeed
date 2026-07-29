package com.yihengquan.cpuspeed.flutter

import android.content.Context
import io.flutter.plugin.common.MethodChannel

abstract class BaseMethodChannel(context: Context) {
    companion object {
        private const val channel = "cpuspeed.flutter/"
    }

    abstract val name: String
    fun setupChannel(): MethodChannel {
        return MethodChannel(FlutterManager.engine.dartExecutor.binaryMessenger, "$channel$name")
    }
}
