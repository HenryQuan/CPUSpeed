package com.yihengquan.cpuspeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yihengquan.cpuspeed.ui.screens.HomeScreen
import com.yihengquan.cpuspeed.ui.theme.CPUSpeedTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPUSpeedTheme {
                HomeScreen()
            }
        }
    }
}
