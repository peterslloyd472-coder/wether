package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.TemperatureUnit
import com.example.model.ThemeMode
import com.example.model.WeatherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("atmosphere_prefs", Context.MODE_PRIVATE)

    private val _unitFlow = MutableStateFlow(loadUnit())
    val unitFlow: StateFlow<TemperatureUnit> = _unitFlow.asStateFlow()

    private val _themeFlow = MutableStateFlow(loadTheme())
    val themeFlow: StateFlow<ThemeMode> = _themeFlow.asStateFlow()

    private val _providerFlow = MutableStateFlow(loadProvider())
    val providerFlow: StateFlow<WeatherProvider> = _providerFlow.asStateFlow()

    private fun loadUnit(): TemperatureUnit {
        val name = prefs.getString("temp_unit", TemperatureUnit.CELSIUS.name)
        return try {
            TemperatureUnit.valueOf(name ?: TemperatureUnit.CELSIUS.name)
        } catch (_: Exception) {
            TemperatureUnit.CELSIUS
        }
    }

    fun setUnit(unit: TemperatureUnit) {
        prefs.edit().putString("temp_unit", unit.name).apply()
        _unitFlow.value = unit
    }

    private fun loadTheme(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setTheme(theme: ThemeMode) {
        prefs.edit().putString("theme_mode", theme.name).apply()
        _themeFlow.value = theme
    }

    private fun loadProvider(): WeatherProvider {
        val name = prefs.getString("weather_provider", WeatherProvider.OPEN_METEO.name)
        return try {
            WeatherProvider.valueOf(name ?: WeatherProvider.OPEN_METEO.name)
        } catch (_: Exception) {
            WeatherProvider.OPEN_METEO
        }
    }

    fun setProvider(provider: WeatherProvider) {
        prefs.edit().putString("weather_provider", provider.name).apply()
        _providerFlow.value = provider
    }

    fun getOpenWeatherKey(): String {
        return prefs.getString("owm_key", "").orEmpty()
    }

    fun setOpenWeatherKey(key: String) {
        prefs.edit().putString("owm_key", key.trim()).apply()
    }

    fun getWeatherApiKey(): String {
        return prefs.getString("weather_api_key", "").orEmpty()
    }

    fun setWeatherApiKey(key: String) {
        prefs.edit().putString("weather_api_key", key.trim()).apply()
    }

    fun saveLastLocation(lat: Double, lon: Double, name: String, country: String) {
        prefs.edit()
            .putFloat("last_lat", lat.toFloat())
            .putFloat("last_lon", lon.toFloat())
            .putString("last_name", name)
            .putString("last_country", country)
            .apply()
    }

    fun getLastLocation(): Pair<Double, Double>? {
        if (!prefs.contains("last_lat") || !prefs.contains("last_lon")) return null
        val lat = prefs.getFloat("last_lat", 0f).toDouble()
        val lon = prefs.getFloat("last_lon", 0f).toDouble()
        return Pair(lat, lon)
    }

    fun getLastLocationName(): Pair<String, String> {
        val name = prefs.getString("last_name", "London") ?: "London"
        val country = prefs.getString("last_country", "United Kingdom") ?: "United Kingdom"
        return Pair(name, country)
    }
}
