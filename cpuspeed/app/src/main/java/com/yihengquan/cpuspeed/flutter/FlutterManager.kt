package com.yihengquan.cpuspeed.flutter;

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine

object FlutterManager {
    lateinit var engine: FlutterEngine

    private lateinit var simpleChannel: SimpleMethodChannel

    /// This must be called in MainActivity
    fun setup(engine: FlutterEngine, context: Context) {
        this.engine = engine
        simpleChannel = SimpleMethodChannel(context)
    }
}
