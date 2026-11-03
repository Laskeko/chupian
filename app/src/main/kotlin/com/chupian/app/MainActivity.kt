package com.chupian.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.chupian.app.ui.CameraScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 相机界面恒定暗色，与取景画面统一
            MaterialTheme(colorScheme = darkColorScheme()) {
                CameraScreen()
            }
        }
    }
}
