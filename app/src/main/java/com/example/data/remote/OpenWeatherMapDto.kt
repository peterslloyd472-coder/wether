package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OwmWeatherResponse(
    val id: Long? = null,
    val name: String? = null,
    val dt: Long = 0,
    val main: OwmMain? = null,
    val weather: List<OwmWeatherItem> = emptyList(),
    val wind: OwmWind? = null
)

@JsonClass(generateAdapter = true)
data class OwmMain(
    val temp: Double = 0.0,
    @Json(name = "feels_like") val feelsLike: Double = 0.0,
    @Json(name = "temp_min") val tempMin: Double = 0.0,
    @Json(name = "temp_max") val tempMax: Double = 0.0,
    val pressure: Double = 1013.0,
    val humidity: Int = 0
)

@JsonClass(generateAdapter = true)
data class OwmWeatherItem(
    val id: Int = 800,
    val main: String = "",
    val description: String = "",
    val icon: String = ""
)

@JsonClass(generateAdapter = true)
data class OwmWind(
    val speed: Double = 0.0,
    val deg: Int = 0
)

@JsonClass(generateAdapter = true)
data class OwmForecastResponse(
    val list: List<OwmForecastItem> = emptyList(),
    val city: OwmCity? = null
)

@JsonClass(generateAdapter = true)
data class OwmCity(
    val name: String = "",
    val country: String = ""
)

@JsonClass(generateAdapter = true)
data class OwmForecastItem(
    val dt: Long = 0,
    val main: OwmMain? = null,
    val weather: List<OwmWeatherItem> = emptyList(),
    val wind: OwmWind? = null,
    val pop: Double = 0.0,
    @Json(name = "dt_txt") val dtTxt: String = ""
)

@JsonClass(generateAdapter = true)
data class OwmGeoItem(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String? = null,
    val state: String? = null
)
