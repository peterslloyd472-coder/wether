package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TemperatureUnit
import com.example.model.WeatherData
import com.example.ui.components.AdminPanelDialog
import com.example.ui.components.AuthDialog
import com.example.ui.components.BottomBannerAd
import com.example.ui.components.CurrentWeatherCard
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.FavoritesDrawerContent
import com.example.ui.components.HourlyForecastRow
import com.example.ui.components.MetricCardsGrid
import com.example.ui.components.SearchLocationDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentLocationFavorite.collectAsStateWithLifecycle()
    val unit by viewModel.unitPreference.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val provider by viewModel.weatherProvider.collectAsStateWithLifecycle()
    val appEdition by viewModel.appEdition.collectAsStateWithLifecycle()

    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val isAdFree by viewModel.isAdFree.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val globalAdBypass by viewModel.globalAdBypass.collectAsStateWithLifecycle()

    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.loadCurrentLocation(context)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission denied. You can search any city.")
            }
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                FavoritesDrawerContent(
                    favorites = favorites,
                    activeProvider = provider,
                    appEdition = appEdition,
                    userSession = userSession,
                    onOpenAuthOrProfile = {
                        scope.launch {
                            drawerState.close()
                            if (userSession == null) {
                                showAuthDialog = true
                            } else {
                                showProfileDialog = true
                            }
                        }
                    },
                    onOpenAdminPanel = {
                        scope.launch {
                            drawerState.close()
                            showAdminDialog = true
                        }
                    },
                    onSelectGps = {
                        scope.launch {
                            drawerState.close()
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onSelectCity = { lat, lon, name, country ->
                        scope.launch {
                            drawerState.close()
                            viewModel.loadWeather(lat, lon, name, country, isGps = false)
                        }
                    },
                    onDeleteCity = { city ->
                        viewModel.deleteFavorite(city)
                    },
                    onOpenSettings = {
                        scope.launch {
                            drawerState.close()
                            showSettingsDialog = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .testTag("weather_dashboard_screen"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lloyds Weather",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (appEdition.isFoss) Color(0xFFD1FAE5) else Color(0xFFE0F2FE))
                                    .clickable { showSettingsDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (appEdition.isFoss) "FOSS" else "GOOGLE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (appEdition.isFoss) Color(0xFF059669) else Color(0xFF0284C7)
                                )
                            }
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .clickable { showAdminDialog = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Open Favorites Drawer"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.testTag("top_gps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "Current GPS Location"
                            )
                        }

                        IconButton(
                            onClick = { showSearchDialog = true },
                            modifier = Modifier.testTag("top_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Location"
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.testTag("top_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh Forecast"
                            )
                        }

                        // Auth / Profile Avatar Button
                        if (userSession != null) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isAdmin) Color(0xFFD97706) else MaterialTheme.colorScheme.primary)
                                    .clickable { showProfileDialog = true }
                                    .testTag("profile_avatar_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userSession?.displayInitials ?: "U",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { showAuthDialog = true },
                                modifier = Modifier.testTag("sign_in_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Sign In for No Ads",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Preferences and Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                // If user is NOT signed in, display bottom banner ad.
                // Once signed in, isAdFree is true -> NOTHING rendered (NO ADS!)
                if (!isAdFree) {
                    BottomBannerAd(onSignInClick = { showAuthDialog = true })
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (val state = uiState) {
                    is WeatherUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Loading weather conditions...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    is WeatherUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Unable to Load Weather",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = { viewModel.refresh() }) {
                                        Text("Try Again")
                                    }
                                    Button(onClick = { showSearchDialog = true }) {
                                        Text("Search City")
                                    }
                                }
                            }
                        }
                    }

                    is WeatherUiState.Success -> {
                        WeatherContent(
                            weatherData = state.data,
                            unit = unit,
                            isFavorite = isFavorite,
                            isRefreshing = state.isRefreshing,
                            onToggleFavorite = { viewModel.toggleFavorite() },
                            onToggleUnit = {
                                val nextUnit = if (unit == TemperatureUnit.CELSIUS) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
                                viewModel.setTemperatureUnit(nextUnit)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSearchDialog) {
        SearchLocationDialog(
            searchResults = searchResults,
            isSearching = isSearching,
            onQueryChange = { viewModel.searchCities(it) },
            onSelectCity = { lat, lon, name, country ->
                viewModel.loadWeather(lat, lon, name, country, isGps = false)
            },
            onUseCurrentLocation = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = {
                showSearchDialog = false
                viewModel.clearSearch()
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentEdition = appEdition,
            currentUnit = unit,
            currentTheme = themeMode,
            currentProvider = provider,
            initialOpenWeatherKey = viewModel.getOpenWeatherKey(),
            initialWeatherApiKey = viewModel.getWeatherApiKey(),
            onEditionChange = { viewModel.setAppEdition(it) },
            onSaveSettings = { newUnit, newTheme, newProvider, owmKey, weatherApiKey ->
                viewModel.setTemperatureUnit(newUnit)
                viewModel.setThemeMode(newTheme)
                viewModel.setWeatherProvider(newProvider, owmKey, weatherApiKey)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            onSignInWithGoogle = {
                viewModel.signInWithGoogle(context)
                scope.launch { snackbarHostState.showSnackbar("Signed in! All ads removed.") }
            },
            onSignInWithEmail = { email, pass ->
                val error = viewModel.signInWithEmail(email, pass)
                if (error == null) {
                    scope.launch { snackbarHostState.showSnackbar("Signed in! All ads removed.") }
                }
                error
            },
            onRegisterWithEmail = { name, email, pass ->
                val error = viewModel.registerWithEmail(name, email, pass)
                if (error == null) {
                    scope.launch { snackbarHostState.showSnackbar("Account created! Enjoy ad-free weather.") }
                }
                error
            },
            onSignInAsAdmin = {
                viewModel.signInAsAdmin()
                scope.launch { snackbarHostState.showSnackbar("Signed in as Administrator (Lloyd)! Ad-free enabled.") }
            },
            onDismiss = { showAuthDialog = false }
        )
    }

    if (showProfileDialog && userSession != null) {
        UserProfileDialog(
            userSession = userSession!!,
            onOpenAdminPanel = { showAdminDialog = true },
            onSignOut = {
                viewModel.signOut()
                scope.launch { snackbarHostState.showSnackbar("Signed out. Guest mode with ads restored.") }
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showAdminDialog && userSession != null) {
        AdminPanelDialog(
            userSession = userSession!!,
            currentProvider = provider,
            globalAdBypass = globalAdBypass,
            onToggleGlobalAdBypass = { viewModel.toggleGlobalAdBypass(it) },
            onSwitchProvider = { viewModel.switchWeatherProvider(it) },
            onSeedWorldCities = {
                viewModel.seedWorldCities()
                scope.launch { snackbarHostState.showSnackbar("World weather stations added to favorites.") }
            },
            onClearAllFavorites = {
                viewModel.clearAllFavorites()
                scope.launch { snackbarHostState.showSnackbar("All saved favorites cleared.") }
            },
            onForceRefresh = {
                viewModel.refresh()
                scope.launch { snackbarHostState.showSnackbar("Forecast force refreshed from server.") }
            },
            onClearCache = {
                viewModel.clearCache()
                scope.launch { snackbarHostState.showSnackbar("Local weather cache cleared.") }
            },
            onDismiss = { showAdminDialog = false }
        )
    }
}

@Composable
private fun WeatherContent(
    weatherData: WeatherData,
    unit: TemperatureUnit,
    isFavorite: Boolean,
    isRefreshing: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleUnit: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isRefreshing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Updating forecast...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (isTablet) {
                // Adaptive Tablet Mode: Dual Pane
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CurrentWeatherCard(
                            weatherData = weatherData,
                            unit = unit,
                            isFavorite = isFavorite,
                            onToggleFavorite = onToggleFavorite,
                            onToggleUnit = onToggleUnit
                        )

                        MetricCardsGrid(
                            current = weatherData.current,
                            unit = unit
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HourlyForecastRow(
                            hourlyList = weatherData.hourly,
                            unit = unit
                        )

                        DailyForecastCard(
                            dailyList = weatherData.daily,
                            unit = unit
                        )
                    }
                }
            } else {
                // Compact Phone Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CurrentWeatherCard(
                        weatherData = weatherData,
                        unit = unit,
                        isFavorite = isFavorite,
                        onToggleFavorite = onToggleFavorite,
                        onToggleUnit = onToggleUnit
                    )

                    HourlyForecastRow(
                        hourlyList = weatherData.hourly,
                        unit = unit
                    )

                    DailyForecastCard(
                        dailyList = weatherData.daily,
                        unit = unit
                    )

                    MetricCardsGrid(
                        current = weatherData.current,
                        unit = unit
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
