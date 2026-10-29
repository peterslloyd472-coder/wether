package com.example.data.repository

import com.example.data.local.FavoriteCityDao
import com.example.data.local.FavoriteCityEntity
import com.example.data.local.UserPreferences
import com.example.data.remote.NetworkClient
import com.example.data.remote.OpenMeteoDailyData
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.data.remote.OpenMeteoHourlyData
import com.example.data.remote.OwmForecastResponse
import com.example.data.remote.OwmWeatherResponse
import com.example.data.remote.WeatherApiForecastResponse
import com.example.model.CurrentWeather
import com.example.model.DailyForecast
import com.example.model.GeoSearchResult
import com.example.model.HourlyWeather
import com.example.model.WeatherData
import com.example.model.WeatherProvider
import com.example.model.WeatherUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

class WeatherRepository(
    private val favoriteCityDao: FavoriteCityDao,
    private val userPreferences: UserPreferences
) {
    val favorites: Flow<List<FavoriteCityEntity>> = favoriteCityDao.getAllFavorites()

    fun isFavorite(lat: Double, lon: Double): Flow<Boolean> = favoriteCityDao.isFavorite(lat, lon)

    suspend fun addFavorite(name: String, country: String, lat: Double, lon: Double) {
        favoriteCityDao.insertFavorite(
            FavoriteCityEntity(
                name = name,
                country = country,
                latitude = lat,
                longitude = lon
            )
        )
    }

    suspend fun removeFavorite(lat: Double, lon: Double) {
        favoriteCityDao.removeByCoordinates(lat, lon)
    }

    suspend fun removeFavoriteById(id: Long) {
        favoriteCityDao.deleteFavoriteById(id)
    }

    suspend fun clearAllFavorites() {
        favoriteCityDao.clearAllFavorites()
    }

    suspend fun seedWorldCities() {
        val sampleCities = listOf(
            FavoriteCityEntity(name = "Tokyo", country = "Japan", latitude = 35.6762, longitude = 139.6503),
            FavoriteCityEntity(name = "Paris", country = "France", latitude = 48.8566, longitude = 2.3522),
            FavoriteCityEntity(name = "New York", country = "United States", latitude = 40.7128, longitude = -74.0060),
            FavoriteCityEntity(name = "Reykjavik", country = "Iceland", latitude = 64.1466, longitude = -21.9426)
        )
        sampleCities.forEach { favoriteCityDao.insertFavorite(it) }
    }

    suspend fun searchLocations(query: String): List<GeoSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val response = NetworkClient.openMeteoGeocodingService.searchLocations(trimmed, count = 8)
            response.results?.map { item ->
                GeoSearchResult(
                    name = item.name,
                    region = item.admin1.orEmpty(),
                    country = item.country.orEmpty(),
                    latitude = item.latitude,
                    longitude = item.longitude
                )
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getWeatherData(
        latitude: Double,
        longitude: Double,
        locationName: String?,
        country: String?,
        isGps: Boolean = false
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        val provider = userPreferences.providerFlow.value

        try {
            when (provider) {
                WeatherProvider.OPEN_METEO -> {
                    fetchFromOpenMeteo(latitude, longitude, locationName, country, isGps)
                }
                WeatherProvider.OPEN_WEATHER_MAP -> {
                    val key = userPreferences.getOpenWeatherKey()
                    if (key.isBlank()) {
                        // Informative fallback
                        fetchFromOpenMeteo(latitude, longitude, locationName, country, isGps)
                    } else {
                        fetchFromOpenWeatherMap(latitude, longitude, locationName, country, key, isGps)
                    }
                }
                WeatherProvider.WEATHER_API -> {
                    val key = userPreferences.getWeatherApiKey()
                    if (key.isBlank()) {
                        // Informative fallback
                        fetchFromOpenMeteo(latitude, longitude, locationName, country, isGps)
                    } else {
                        fetchFromWeatherApi(latitude, longitude, locationName, country, key, isGps)
                    }
                }
            }
        } catch (e: Exception) {
            // If secondary provider failed, attempt Open-Meteo fallback
            if (provider != WeatherProvider.OPEN_METEO) {
                try {
                    return@withContext fetchFromOpenMeteo(latitude, longitude, locationName, country, isGps)
                } catch (_: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchFromOpenMeteo(
        latitude: Double,
        longitude: Double,
        locationName: String?,
        country: String?,
        isGps: Boolean
    ): Result<WeatherData> {
        val response = NetworkClient.openMeteoService.getForecast(latitude, longitude)
        val currentData = response.current ?: throw IllegalStateException("Current weather unavailable")

        val current = CurrentWeather(
            temperatureC = currentData.temperature2m,
            apparentTemperatureC = currentData.apparentTemperature,
            weatherCode = currentData.weatherCode,
            conditionText = WeatherUtils.mapWmoCodeToText(currentData.weatherCode),
            humidityPercent = currentData.relativeHumidity2m,
            windSpeedKmh = currentData.windSpeed10m,
            windDirectionDegrees = currentData.windDirection10m,
            surfacePressureHpa = currentData.surfacePressure,
            precipitationMm = currentData.precipitation,
            isDay = currentData.isDay == 1
        )

        val hourlyList = parseOpenMeteoHourly(response.hourly)
        val dailyList = parseOpenMeteoDaily(response.daily)

        val finalName = locationName?.ifBlank { "Selected Location" } ?: "Selected Location"
        val finalCountry = country.orEmpty()

        return Result.success(
            WeatherData(
                locationName = finalName,
                country = finalCountry,
                latitude = latitude,
                longitude = longitude,
                isGpsLocation = isGps,
                current = current,
                hourly = hourlyList,
                daily = dailyList,
                provider = WeatherProvider.OPEN_METEO
            )
        )
    }

    private fun parseOpenMeteoHourly(hourly: OpenMeteoHourlyData?): List<HourlyWeather> {
        if (hourly == null || hourly.time.isEmpty()) return emptyList()

        val list = mutableListOf<HourlyWeather>()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("ha", Locale.getDefault())
        val now = System.currentTimeMillis() - (3600 * 1000) // include current hour

        val maxHours = min(48, hourly.time.size)
        for (i in 0 until maxHours) {
            val timeStr = hourly.time[i]
            val parsedDate = try { inputFormat.parse(timeStr) } catch (_: Exception) { null }
            val epoch = parsedDate?.time ?: 0L
            if (epoch < now) continue

            val formattedTime = if (parsedDate != null) outputFormat.format(parsedDate).lowercase() else timeStr
            val temp = hourly.temperature2m.getOrNull(i) ?: 0.0
            val code = hourly.weatherCode.getOrNull(i) ?: 0
            val pop = hourly.precipitationProbability.getOrNull(i) ?: 0

            list.add(
                HourlyWeather(
                    timeFormatted = formattedTime,
                    epochMillis = epoch,
                    temperatureC = temp,
                    weatherCode = code,
                    conditionText = WeatherUtils.mapWmoCodeToText(code),
                    precipitationProb = pop
                )
            )

            if (list.size >= 24) break
        }
        return list
    }

    private fun parseOpenMeteoDaily(daily: OpenMeteoDailyData?): List<DailyForecast> {
        if (daily == null || daily.time.isEmpty()) return emptyList()

        val list = mutableListOf<DailyForecast>()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val calendarToday = Calendar.getInstance()
        val todayStr = inputFormat.format(calendarToday.time)

        for (i in daily.time.indices) {
            val dateStr = daily.time[i]
            val parsedDate = try { inputFormat.parse(dateStr) } catch (_: Exception) { null }
            val dayName = if (dateStr == todayStr) {
                "Today"
            } else if (parsedDate != null) {
                dayNameFormat.format(parsedDate)
            } else {
                dateStr
            }

            val code = daily.weatherCode.getOrNull(i) ?: 0
            val maxT = daily.temperature2mMax.getOrNull(i) ?: 0.0
            val minT = daily.temperature2mMin.getOrNull(i) ?: 0.0
            val pop = daily.precipitationProbabilityMax.getOrNull(i) ?: 0
            val uv = daily.uvIndexMax.getOrNull(i) ?: 0.0
            val wind = daily.windSpeed10mMax.getOrNull(i) ?: 0.0

            list.add(
                DailyForecast(
                    dateStr = dateStr,
                    dayOfWeek = dayName,
                    weatherCode = code,
                    conditionText = WeatherUtils.mapWmoCodeToText(code),
                    maxTempC = maxT,
                    minTempC = minT,
                    precipitationProb = pop,
                    uvIndexMax = uv,
                    windSpeedMaxKmh = wind
                )
            )

            if (list.size >= 7) break
        }
        return list
    }

    private suspend fun fetchFromOpenWeatherMap(
        latitude: Double,
        longitude: Double,
        locationName: String?,
        country: String?,
        apiKey: String,
        isGps: Boolean
    ): Result<WeatherData> {
        val currentRes = NetworkClient.openWeatherMapService.getCurrentWeather(latitude, longitude, apiKey)
        val forecastRes = NetworkClient.openWeatherMapService.get5DayForecast(latitude, longitude, apiKey)

        val weatherItem = currentRes.weather.firstOrNull()
        val current = CurrentWeather(
            temperatureC = currentRes.main?.temp ?: 0.0,
            apparentTemperatureC = currentRes.main?.feelsLike ?: 0.0,
            weatherCode = mapOwmIdToWmo(weatherItem?.id ?: 800),
            conditionText = weatherItem?.main ?: "Clear",
            humidityPercent = currentRes.main?.humidity ?: 0,
            windSpeedKmh = (currentRes.wind?.speed ?: 0.0) * 3.6, // m/s to km/h
            windDirectionDegrees = currentRes.wind?.deg ?: 0,
            surfacePressureHpa = currentRes.main?.pressure ?: 1013.0,
            isDay = true
        )

        val hourlyList = forecastRes.list.take(24).map { item ->
            val w = item.weather.firstOrNull()
            val timeFmt = SimpleDateFormat("ha", Locale.getDefault()).format(Date(item.dt * 1000)).lowercase()
            HourlyWeather(
                timeFormatted = timeFmt,
                epochMillis = item.dt * 1000,
                temperatureC = item.main?.temp ?: 0.0,
                weatherCode = mapOwmIdToWmo(w?.id ?: 800),
                conditionText = w?.main ?: "Clear",
                precipitationProb = ((item.pop) * 100).toInt()
            )
        }

        // Aggregate 5-day forecast by date
        val dailyMap = mutableMapOf<String, MutableList<com.example.data.remote.OwmForecastItem>>()
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        forecastRes.list.forEach { item ->
            val day = dayFmt.format(Date(item.dt * 1000))
            dailyMap.getOrPut(day) { mutableListOf() }.add(item)
        }

        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val todayStr = dayFmt.format(Date())
        val dailyList = dailyMap.map { (dateStr, items) ->
            val maxTemp = items.maxOfOrNull { it.main?.tempMax ?: it.main?.temp ?: 0.0 } ?: 0.0
            val minTemp = items.minOfOrNull { it.main?.tempMin ?: it.main?.temp ?: 0.0 } ?: 0.0
            val maxPop = (items.maxOfOrNull { it.pop } ?: 0.0) * 100
            val midItem = items[items.size / 2]
            val w = midItem.weather.firstOrNull()
            val parsedDate = try { dayFmt.parse(dateStr) } catch (_: Exception) { null }
            val dayName = if (dateStr == todayStr) "Today" else if (parsedDate != null) dayNameFormat.format(parsedDate) else dateStr

            DailyForecast(
                dateStr = dateStr,
                dayOfWeek = dayName,
                weatherCode = mapOwmIdToWmo(w?.id ?: 800),
                conditionText = w?.main ?: "Clear",
                maxTempC = maxTemp,
                minTempC = minTemp,
                precipitationProb = maxPop.toInt()
            )
        }.take(5)

        val finalName = locationName ?: currentRes.name ?: "Selected Location"
        val finalCountry = country ?: forecastRes.city?.country.orEmpty()

        return Result.success(
            WeatherData(
                locationName = finalName,
                country = finalCountry,
                latitude = latitude,
                longitude = longitude,
                isGpsLocation = isGps,
                current = current,
                hourly = hourlyList,
                daily = dailyList,
                provider = WeatherProvider.OPEN_WEATHER_MAP
            )
        )
    }

    private suspend fun fetchFromWeatherApi(
        latitude: Double,
        longitude: Double,
        locationName: String?,
        country: String?,
        apiKey: String,
        isGps: Boolean
    ): Result<WeatherData> {
        val query = "$latitude,$longitude"
        val response = NetworkClient.weatherApiService.getForecast(apiKey, query, days = 6)
        val cur = response.current ?: throw IllegalStateException("Current data empty")

        val current = CurrentWeather(
            temperatureC = cur.tempC,
            apparentTemperatureC = cur.feelslikeC,
            weatherCode = mapWeatherApiCodeToWmo(cur.condition?.code ?: 1000),
            conditionText = cur.condition?.text ?: "Clear",
            humidityPercent = cur.humidity,
            windSpeedKmh = cur.windKph,
            windDirectionDegrees = cur.windDegree,
            surfacePressureHpa = cur.pressureMb,
            uvIndex = cur.uv,
            isDay = cur.isDay == 1
        )

        val hourlyList = mutableListOf<HourlyWeather>()
        val timeFmt = SimpleDateFormat("ha", Locale.getDefault())
        response.forecast?.forecastday?.forEach { day ->
            day.hour.forEach { h ->
                val epoch = h.timeEpoch * 1000
                if (epoch >= System.currentTimeMillis() - (3600 * 1000)) {
                    val formatted = timeFmt.format(Date(epoch)).lowercase()
                    hourlyList.add(
                        HourlyWeather(
                            timeFormatted = formatted,
                            epochMillis = epoch,
                            temperatureC = h.tempC,
                            weatherCode = mapWeatherApiCodeToWmo(h.condition?.code ?: 1000),
                            conditionText = h.condition?.text ?: "Clear",
                            precipitationProb = h.chanceOfRain
                        )
                    )
                }
            }
        }

        val dayNameFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFmt.format(Date())

        val dailyList = response.forecast?.forecastday?.map { fd ->
            val parsedDate = try { dateFmt.parse(fd.date) } catch (_: Exception) { null }
            val dayName = if (fd.date == todayStr) "Today" else if (parsedDate != null) dayNameFmt.format(parsedDate) else fd.date

            DailyForecast(
                dateStr = fd.date,
                dayOfWeek = dayName,
                weatherCode = mapWeatherApiCodeToWmo(fd.day?.condition?.code ?: 1000),
                conditionText = fd.day?.condition?.text ?: "Clear",
                maxTempC = fd.day?.maxtempC ?: 0.0,
                minTempC = fd.day?.mintempC ?: 0.0,
                precipitationProb = fd.day?.dailyChanceOfRain ?: 0,
                uvIndexMax = fd.day?.uv ?: 0.0,
                windSpeedMaxKmh = fd.day?.maxwindKph ?: 0.0
            )
        } ?: emptyList()

        val finalName = locationName ?: response.location?.name ?: "Selected Location"
        val finalCountry = country ?: response.location?.country.orEmpty()

        return Result.success(
            WeatherData(
                locationName = finalName,
                country = finalCountry,
                latitude = latitude,
                longitude = longitude,
                isGpsLocation = isGps,
                current = current,
                hourly = hourlyList.take(24),
                daily = dailyList,
                provider = WeatherProvider.WEATHER_API
            )
        )
    }

    private fun mapOwmIdToWmo(id: Int): Int {
        return when (id) {
            800 -> 0 // Clear sky
            801 -> 1 // Few clouds
            802 -> 2 // Scattered clouds
            803, 804 -> 3 // Broken / overcast
            in 200..232 -> 95 // Thunderstorm
            in 300..321 -> 51 // Drizzle
            in 500..504 -> 61 // Rain
            511 -> 66 // Freezing rain
            in 520..531 -> 80 // Rain showers
            in 600..622 -> 71 // Snow
            in 701..781 -> 45 // Atmosphere/Fog
            else -> 2
        }
    }

    private fun mapWeatherApiCodeToWmo(code: Int): Int {
        return when (code) {
            1000 -> 0 // Sunny / Clear
            1003 -> 2 // Partly cloudy
            1006, 1009 -> 3 // Cloudy / Overcast
            1030, 1135, 1147 -> 45 // Fog / Mist
            1063, 1180, 1183, 1186, 1189 -> 61 // Patchy / Moderate rain
            1192, 1195, 1243, 1246 -> 65 // Heavy rain
            1066, 1210, 1213, 1216, 1219, 1222, 1225 -> 71 // Snow
            1087, 1273, 1276, 1279, 1282 -> 95 // Thunderstorm
            else -> 2
        }
    }
}
