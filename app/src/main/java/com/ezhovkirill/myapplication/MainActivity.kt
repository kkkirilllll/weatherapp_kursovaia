package com.ezhovkirill.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezhovkirill.myapplication.ui.WeatherHome
import com.ezhovkirill.myapplication.ui.WeatherViewModel
import com.ezhovkirill.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: WeatherViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeatherHome(viewModel = viewModel, innerPadding = innerPadding)
                }
            }
        }
    }
}
