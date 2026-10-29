package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteCityEntity
import com.example.data.local.UserAuthManager
import com.example.data.local.UserPreferences
import com.example.data.repository.WeatherRepository
import com.example.model.AppEdition
import com.example.model.GeoSearchResult
import com.example.model.TemperatureUnit
import com.example.model.ThemeMode
import com.example.model.UserSession
import com.example.model.WeatherData
import com.example.model.WeatherProvider
import com.example.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(
        val data: WeatherData,
        val isRefreshing: Boolean = false,
        val notice: String? = null
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WeatherViewModel(
    private val repository: WeatherRepository,
    private val userPreferences: UserPreferences,
    private val userAuthManager: UserAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    val unitPreference: StateFlow<TemperatureUnit> = userPreferences.unitFlow
    val themeMode: StateFlow<ThemeMode> = userPreferences.themeFlow
    val weatherProvider: StateFlow<WeatherProvider> = userPreferences.providerFlow
    val appEdition: StateFlow<AppEdition> = userPreferences.editionFlow

    val userSession: StateFlow<UserSession?> = userAuthManager.currentUser
    val globalAdBypass: StateFlow<Boolean> = userAuthManager.globalAdBypass

    // Ad is completely removed if running FOSS Edition OR user is signed in OR admin bypass enabled
    val isAdFree: StateFlow<Boolean> = combine(userSession, globalAdBypass, appEdition) { session, bypass, edition ->
        edition.isFoss || session != null || bypass
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        userPreferences.editionFlow.value.isFoss || userAuthManager.currentUser.value != null || userAuthManager.globalAdBypass.value
    )

    val isAdmin: StateFlow<Boolean> = userSession.map { it?.isAdmin == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), userAuthManager.currentUser.value?.isAdmin == true)

    val favorites: StateFlow<List<FavoriteCityEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentCoords = MutableStateFlow<Pair<Double, Double>?>(null)

    val isCurrentLocationFavorite: StateFlow<Boolean> = _currentCoords
        .flatMapLatest { coords ->
            if (coords != null) {
                repository.isFavorite(coords.first, coords.second)
            } else {
                flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _searchResults = MutableStateFlow<List<GeoSearchResult>>(emptyList())
    val searchResults: StateFlow<List<GeoSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null
    private var activeLat: Double = 51.5074
    private var activeLon: Double = -0.1278
    private var activeName: String = "London"
    private var activeCountry: String = "United Kingdom"
    private var isGpsActive: Boolean = false

    init {
        val lastLoc = userPreferences.getLastLocation()
        if (lastLoc != null) {
            val (name, country) = userPreferences.getLastLocationName()
            activeLat = lastLoc.first
            activeLon = lastLoc.second
            activeName = name
            activeCountry = country
        }
        loadWeather(activeLat, activeLon, activeName, activeCountry, isGps = false)
    }

    fun loadWeather(
        lat: Double,
        lon: Double,
        name: String,
        country: String,
        isGps: Boolean = false
    ) {
        activeLat = lat
        activeLon = lon
        activeName = name
        activeCountry = country
        isGpsActive = isGps
        _currentCoords.value = Pair(lat, lon)
        userPreferences.saveLastLocation(lat, lon, name, country)

        viewModelScope.launch {
            val currentData = (_uiState.value as? WeatherUiState.Success)?.data
            if (currentData == null) {
                _uiState.value = WeatherUiState.Loading
            } else {
                _uiState.value = WeatherUiState.Success(currentData, isRefreshing = true)
            }

            val result = repository.getWeatherData(lat, lon, name, country, isGps)
            result.fold(
                onSuccess = { data ->
                    _uiState.value = WeatherUiState.Success(data, isRefreshing = false)
                },
                onFailure = { error ->
                    _uiState.value = WeatherUiState.Error(
                        error.localizedMessage ?: "Unable to fetch weather data. Check your internet connection."
                    )
                }
            )
        }
    }

    fun refresh() {
        loadWeather(activeLat, activeLon, activeName, activeCountry, isGpsActive)
    }

    fun loadCurrentLocation(context: Context) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val isFoss = appEdition.value.isFoss
            val deviceLocation = LocationHelper.getCurrentLocation(context, isFossEdition = isFoss)
            if (deviceLocation != null) {
                loadWeather(
                    lat = deviceLocation.latitude,
                    lon = deviceLocation.longitude,
                    name = deviceLocation.cityName,
                    country = deviceLocation.country,
                    isGps = true
                )
            } else {
                _uiState.value = WeatherUiState.Error("Location permission required or GPS unavailable. Please enable GPS in device settings.")
            }
        }
    }

    fun searchCities(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(350)
            val results = repository.searchLocations(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun toggleFavorite() {
        val coords = _currentCoords.value ?: return
        val currentState = _uiState.value as? WeatherUiState.Success ?: return
        viewModelScope.launch {
            val isFav = isCurrentLocationFavorite.value
            if (isFav) {
                repository.removeFavorite(coords.first, coords.second)
            } else {
                repository.addFavorite(
                    name = currentState.data.locationName,
                    country = currentState.data.country,
                    lat = coords.first,
                    lon = coords.second
                )
            }
        }
    }

    fun deleteFavorite(city: FavoriteCityEntity) {
        viewModelScope.launch {
            repository.removeFavoriteById(city.id)
        }
    }

    fun setAppEdition(edition: AppEdition) {
        userPreferences.setEdition(edition)
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        userPreferences.setUnit(unit)
    }

    fun setThemeMode(mode: ThemeMode) {
        userPreferences.setTheme(mode)
    }

    fun setWeatherProvider(provider: WeatherProvider, openWeatherKey: String, weatherApiKey: String) {
        userPreferences.setProvider(provider)
        if (openWeatherKey.isNotBlank()) {
            userPreferences.setOpenWeatherKey(openWeatherKey)
        }
        if (weatherApiKey.isNotBlank()) {
            userPreferences.setWeatherApiKey(weatherApiKey)
        }
        refresh()
    }

    fun getOpenWeatherKey(): String = userPreferences.getOpenWeatherKey()
    fun getWeatherApiKey(): String = userPreferences.getWeatherApiKey()

    // Authentication methods
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            userAuthManager.signInWithGoogle(context)
        }
    }

    fun signInWithEmail(email: String, pass: String): String? {
        val result = userAuthManager.signInWithEmail(email, pass)
        return result.exceptionOrNull()?.message
    }

    fun registerWithEmail(name: String, email: String, pass: String): String? {
        val result = userAuthManager.registerWithEmail(name, email, pass)
        return result.exceptionOrNull()?.message
    }

    fun signInAsAdmin() {
        userAuthManager.signInAsAdmin()
    }

    fun signOut() {
        userAuthManager.signOut()
    }

    // Admin Suite Actions
    fun toggleGlobalAdBypass(enabled: Boolean) {
        userAuthManager.setGlobalAdBypass(enabled)
    }

    fun switchWeatherProvider(provider: WeatherProvider) {
        userPreferences.setProvider(provider)
        refresh()
    }

    fun seedWorldCities() {
        viewModelScope.launch {
            repository.seedWorldCities()
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            repository.clearAllFavorites()
        }
    }

    fun clearCache() {
        refresh()
    }
}

class WeatherViewModelFactory(
    private val repository: WeatherRepository,
    private val userPreferences: UserPreferences,
    private val userAuthManager: UserAuthManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(repository, userPreferences, userAuthManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
