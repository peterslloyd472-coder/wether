package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserAuthManager
import com.example.data.local.UserPreferences
import com.example.data.local.WeatherDatabase
import com.example.data.repository.WeatherRepository
import com.example.ui.screens.WeatherDashboardScreen
import com.example.ui.theme.AtmosphereTheme
import com.example.ui.viewmodel.WeatherViewModel
import com.example.ui.viewmodel.WeatherViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels {
        val database = WeatherDatabase.getInstance(applicationContext)
        val userPreferences = UserPreferences(applicationContext)
        val userAuthManager = UserAuthManager(applicationContext)
        val repository = WeatherRepository(database.favoriteCityDao(), userPreferences)
        WeatherViewModelFactory(repository, userPreferences, userAuthManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            AtmosphereTheme(themeMode = themeMode) {
                WeatherDashboardScreen(viewModel = viewModel)
            }
        }
    }
}
