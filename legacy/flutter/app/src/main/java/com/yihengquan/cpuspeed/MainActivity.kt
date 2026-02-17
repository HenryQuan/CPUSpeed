package com.yihengquan.cpuspeed

import android.graphics.Color
import android.os.Bundle
import com.yihengquan.cpuspeed.flutter.FlutterManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        FlutterManager.setup(flutterEngine, context)
    }
}