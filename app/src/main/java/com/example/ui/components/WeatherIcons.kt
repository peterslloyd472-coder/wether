package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.WeatherCloudy
import com.example.ui.theme.WeatherRain
import com.example.ui.theme.WeatherSnow
import com.example.ui.theme.WeatherSunny
import com.example.ui.theme.WeatherThunder

data class WeatherIconInfo(
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun getWeatherIcon(weatherCode: Int, isDay: Boolean = true): WeatherIconInfo {
    return when (weatherCode) {
        0 -> if (isDay) {
            WeatherIconInfo(Icons.Filled.WbSunny, WeatherSunny)
        } else {
            WeatherIconInfo(Icons.Filled.NightsStay, Color(0xFF94A3B8))
        }
        1 -> if (isDay) {
            WeatherIconInfo(Icons.Filled.WbSunny, WeatherSunny)
        } else {
            WeatherIconInfo(Icons.Filled.NightsStay, Color(0xFF94A3B8))
        }
        2 -> WeatherIconInfo(Icons.Filled.CloudQueue, WeatherCloudy)
        3 -> WeatherIconInfo(Icons.Filled.Cloud, WeatherCloudy)
        45, 48 -> WeatherIconInfo(Icons.Filled.WbCloudy, WeatherCloudy)
        51, 53, 55 -> WeatherIconInfo(Icons.Filled.Grain, WeatherRain)
        56, 57 -> WeatherIconInfo(Icons.Filled.AcUnit, WeatherSnow)
        61, 63, 65 -> WeatherIconInfo(Icons.Filled.WaterDrop, WeatherRain)
        66, 67 -> WeatherIconInfo(Icons.Filled.AcUnit, WeatherSnow)
        71, 73, 75, 77 -> WeatherIconInfo(Icons.Filled.AcUnit, WeatherSnow)
        80, 81, 82 -> WeatherIconInfo(Icons.Filled.WaterDrop, WeatherRain)
        85, 86 -> WeatherIconInfo(Icons.Filled.AcUnit, WeatherSnow)
        95, 96, 99 -> WeatherIconInfo(Icons.Filled.Thunderstorm, WeatherThunder)
        else -> WeatherIconInfo(Icons.Filled.WbCloudy, WeatherCloudy)
    }
}
