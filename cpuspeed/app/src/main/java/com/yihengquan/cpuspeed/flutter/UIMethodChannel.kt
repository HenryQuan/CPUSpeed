package com.yihengquan.cpuspeed.flutter

import android.content.Context
import android.widget.Toast
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class UIMethodChannel(context: Context) : BaseMethodChannel(context) {
    override val name: String = "ui"
    private val channel: MethodChannel by lazy(this::setupChannel)

    init {
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "toast" -> toast(context, call)
                else -> result.notImplemented()
            }
        }
    }

    private fun toast(context: Context, call: MethodCall) {
        val msg = call.argument<String>("message")
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}