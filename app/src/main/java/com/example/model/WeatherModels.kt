package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class TemperatureUnit(val symbol: String, val label: String) {
    CELSIUS("°C", "Celsius"),
    FAHRENHEIT("°F", "Fahrenheit")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class WeatherProvider(val displayName: String, val requiresKey: Boolean) {
    OPEN_METEO("Open-Meteo (No API Key Required)", false),
    OPEN_WEATHER_MAP("OpenWeatherMap (Requires Key)", true),
    WEATHER_API("WeatherAPI.com (Requires Key)", true)
}

data class GeoSearchResult(
    val name: String,
    val region: String = "",
    val country: String = "",
    val latitude: Double,
    val longitude: Double
) {
    val displaySubtitle: String
        get() = listOf(region, country).filter { it.isNotBlank() }.joinToString(", ")
}

data class CurrentWeather(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val weatherCode: Int,
    val conditionText: String,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int = 0,
    val surfacePressureHpa: Double = 1013.0,
    val uvIndex: Double = 0.0,
    val precipitationMm: Double = 0.0,
    val isDay: Boolean = true
) {
    val windDirectionText: String
        get() {
            val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
            val index = ((windDirectionDegrees + 11.25) / 22.5).toInt() % 16
            return directions[index]
        }
}

data class HourlyWeather(
    val timeFormatted: String,
    val epochMillis: Long,
    val temperatureC: Double,
    val weatherCode: Int,
    val conditionText: String,
    val precipitationProb: Int = 0
)

data class DailyForecast(
    val dateStr: String,
    val dayOfWeek: String,
    val weatherCode: Int,
    val conditionText: String,
    val maxTempC: Double,
    val minTempC: Double,
    val precipitationProb: Int = 0,
    val uvIndexMax: Double = 0.0,
    val windSpeedMaxKmh: Double = 0.0
)

data class WeatherData(
    val locationName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isGpsLocation: Boolean = false,
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyForecast>,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val provider: WeatherProvider = WeatherProvider.OPEN_METEO
)

object WeatherUtils {
    fun convertCelsiusToUnit(celsius: Double, unit: TemperatureUnit): Double {
        return when (unit) {
            TemperatureUnit.CELSIUS -> celsius
            TemperatureUnit.FAHRENHEIT -> (celsius * 9.0 / 5.0) + 32.0
        }
    }

    fun formatTemperature(celsius: Double, unit: TemperatureUnit): String {
        val converted = convertCelsiusToUnit(celsius, unit)
        return "${converted.roundToInt()}${unit.symbol}"
    }

    fun formatSpeed(speedKmh: Double, unit: TemperatureUnit): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> "${speedKmh.roundToInt()} km/h"
            TemperatureUnit.FAHRENHEIT -> "${(speedKmh * 0.621371).roundToInt()} mph"
        }
    }

    fun formatLastUpdated(millis: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    // WMO Weather code to text interpretation
    fun mapWmoCodeToText(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66, 67 -> "Freezing Rain"
            71, 73 -> "Snow Fall"
            75 -> "Heavy Snow"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Partly Cloudy"
        }
    }
}
