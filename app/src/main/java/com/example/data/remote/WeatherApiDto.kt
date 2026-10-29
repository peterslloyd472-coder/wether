package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherApiForecastResponse(
    val location: WeatherApiLocation? = null,
    val current: WeatherApiCurrent? = null,
    val forecast: WeatherApiForecastContainer? = null
)

@JsonClass(generateAdapter = true)
data class WeatherApiLocation(
    val name: String = "",
    val region: String = "",
    val country: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class WeatherApiCurrent(
    @Json(name = "temp_c") val tempC: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1,
    val condition: WeatherApiCondition? = null,
    @Json(name = "wind_kph") val windKph: Double = 0.0,
    @Json(name = "wind_degree") val windDegree: Int = 0,
    @Json(name = "pressure_mb") val pressureMb: Double = 1013.0,
    val humidity: Int = 0,
    @Json(name = "feelslike_c") val feelslikeC: Double = 0.0,
    val uv: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class WeatherApiCondition(
    val text: String = "",
    val icon: String = "",
    val code: Int = 1000
)

@JsonClass(generateAdapter = true)
data class WeatherApiForecastContainer(
    val forecastday: List<WeatherApiForecastDay> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WeatherApiForecastDay(
    val date: String = "",
    @Json(name = "date_epoch") val dateEpoch: Long = 0,
    val day: WeatherApiDayData? = null,
    val hour: List<WeatherApiHourData> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WeatherApiDayData(
    @Json(name = "maxtemp_c") val maxtempC: Double = 0.0,
    @Json(name = "mintemp_c") val mintempC: Double = 0.0,
    @Json(name = "daily_chance_of_rain") val dailyChanceOfRain: Int = 0,
    val condition: WeatherApiCondition? = null,
    val uv: Double = 0.0,
    @Json(name = "maxwind_kph") val maxwindKph: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class WeatherApiHourData(
    @Json(name = "time_epoch") val timeEpoch: Long = 0,
    val time: String = "",
    @Json(name = "temp_c") val tempC: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1,
    val condition: WeatherApiCondition? = null,
    @Json(name = "chance_of_rain") val chanceOfRain: Int = 0
)

@JsonClass(generateAdapter = true)
data class WeatherApiSearchItem(
    val id: Long = 0,
    val name: String = "",
    val region: String = "",
    val country: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)
