package com.yihengquan.cpuspeed.flutter;

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine

object FlutterManager {
    lateinit var engine: FlutterEngine

    private lateinit var uiChannel: UIMethodChannel

    /// This must be called in MainActivity
    fun setup(engine: FlutterEngine, context: Context) {
        this.engine = engine
        uiChannel = UIMethodChannel(context)
    }
}
