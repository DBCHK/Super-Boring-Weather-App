package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CitySelectionSheet
import com.example.ui.screens.DetailedForecastScreen
import com.example.ui.screens.MainWeatherScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalAppThemeMode
import com.example.ui.theme.LocalThemePalette
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePalette
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import com.example.util.LocationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NotBoringWeatherApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotBoringWeatherApp(
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val weatherUiState by viewModel.weatherUiState.collectAsState()
    val selectedHourIndex by viewModel.selectedHourIndex.collectAsState()
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    val savedCities by viewModel.savedCities.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val lastRefreshAtMs by viewModel.lastRefreshAtMs.collectAsState()

    // 0 Light (default white) · 1 Yellow · 2 Dark — shared across home, details, city sheet
    var themeMode by rememberSaveable { mutableIntStateOf(0) }
    val appTheme = when (themeMode) {
        1 -> AppThemeMode.YELLOW
        2 -> AppThemeMode.DARK
        else -> AppThemeMode.LIGHT
    }
    val palette = ThemePalette.forMode(appTheme)

    var showDetailsScreen by remember { mutableStateOf(false) }
    var showCitySheet by remember { mutableStateOf(false) }
    var showLocationServicesDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            // After permission, still check if device location is on
            if (!locationHelper.isLocationServicesEnabled()) {
                showLocationServicesDialog = true
            } else {
                scope.launch {
                    viewModel.detectUserLocationResult()
                }
            }
        }
    }

    val runDetect: () -> Unit = {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 1) Device location services must be on first
        if (!locationHelper.isLocationServicesEnabled()) {
            showLocationServicesDialog = true
        } else if (!(hasFine || hasCoarse)) {
            // 2) App permission
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // 3) Detect
            scope.launch {
                when (viewModel.detectUserLocationResult()) {
                    is WeatherViewModel.LocationDetectResult.ServicesDisabled -> {
                        showLocationServicesDialog = true
                    }
                    is WeatherViewModel.LocationDetectResult.PermissionRequired -> {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    // Auto-detect on first composition only if permission + services are available
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if ((hasFine || hasCoarse) && locationHelper.isLocationServicesEnabled()) {
            viewModel.detectUserLocationResult()
        } else {
            // No location path — still pull a fresh forecast for the selected city
            viewModel.refreshWeather(showLoading = true)
        }
    }

    // Every time the user opens / returns to the app, refresh live weather
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Silent refresh so the UI does not flash Loading over existing data
                viewModel.refreshWeather(showLoading = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showLocationServicesDialog) {
        AlertDialog(
            onDismissRequest = { showLocationServicesDialog = false },
            title = { Text("Turn on location") },
            text = {
                Text(
                    "Location is turned off on this device. Enable it in Settings so we can " +
                        "auto-detect your city and refresh local weather."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationServicesDialog = false
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationServicesDialog = false }) {
                    Text("Not now")
                }
            }
        )
    }

    CompositionLocalProvider(
        LocalThemePalette provides palette,
        LocalAppThemeMode provides appTheme
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MainWeatherScreen(
                    weatherUiState = weatherUiState,
                    selectedHourIndex = selectedHourIndex,
                    temperatureUnit = temperatureUnit,
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                    lastRefreshAtMs = lastRefreshAtMs,
                    onHourSelected = { viewModel.setSelectedHourIndex(it) },
                    onToggleUnit = { viewModel.toggleTemperatureUnit() },
                    onOpenCitySheet = { showCitySheet = true },
                    onOpenDetailsCard = { showDetailsScreen = true },
                    onDetectLocation = { runDetect() },
                    onRefresh = { viewModel.refreshWeather() }
                )

                AnimatedVisibility(
                    visible = showDetailsScreen,
                    enter = slideInVertically(
                        initialOffsetY = { full -> full },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(220)),
                    exit = slideOutVertically(
                        targetOffsetY = { full -> full },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(180)),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f)
                ) {
                    if (weatherUiState is WeatherUiState.Success) {
                        DetailedForecastScreen(
                            data = (weatherUiState as WeatherUiState.Success).data,
                            selectedHourIndex = selectedHourIndex,
                            temperatureUnit = temperatureUnit,
                            onHourSelected = { viewModel.setSelectedHourIndex(it) },
                            onClose = { showDetailsScreen = false }
                        )
                    }
                }

                if (showCitySheet) {
                    CitySelectionSheet(
                        sheetState = sheetState,
                        savedCities = savedCities,
                        selectedCity = selectedCity,
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSelectCity = { viewModel.selectCity(it) },
                        onSaveCity = { viewModel.saveCity(it) },
                        onDeleteCity = { viewModel.deleteCity(it) },
                        onDetectLocation = { runDetect() },
                        onDismiss = {
                            viewModel.updateSearchQuery("")
                            showCitySheet = false
                        }
                    )
                }
            }
        }
    }
}
