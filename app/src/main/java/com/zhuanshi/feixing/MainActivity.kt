package com.zhuanshi.feixing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhuanshi.feixing.ui.FlightApp
import com.zhuanshi.feixing.ui.theme.FocusFlightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 全屏沉浸：状态栏 + 导航栏透明，内容铺到屏幕四边和刘海区
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FocusFlightTheme {
                FlightApp()
            }
        }
    }
}
