package com.ezhovkirill.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.ezhovkirill.myapplication.ui.WeatherHome
import com.ezhovkirill.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Pass innerPadding if WeatherHome supports it, or handle it inside WeatherHome
                    // For now, assuming WeatherHome handles its own layout or ignores padding for full screen
                    // But since WeatherHome has a background image, it likely wants to be full screen.
                    // However, Scaffold provides padding for system bars.
                    // Let's check WeatherHome again to see if it takes a modifier.
                    WeatherHome()
                }
            }
        }
    }
}
