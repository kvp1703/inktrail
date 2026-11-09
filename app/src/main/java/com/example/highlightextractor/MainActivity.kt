package com.example.highlightextractor

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.highlightextractor.ui.MainScreen
import com.example.highlightextractor.ui.theme.InktrailTheme
import com.example.highlightextractor.ui.theme.LocalInkPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContent {
            InktrailTheme {
                val ink = LocalInkPalette.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ink.bg,
                    contentColor = ink.fg,
                ) {
                    MainScreen()
                }
            }
        }
    }
}
