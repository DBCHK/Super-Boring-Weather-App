package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CitySelectionSheet
import com.example.ui.screens.DetailedForecastScreen
import com.example.ui.screens.MainWeatherScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel

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
    val pinnedWidgets by viewModel.pinnedWidgets.collectAsState()

    var showDetailsScreen by remember { mutableStateOf(false) }
    var showCitySheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.detectUserLocation()
        }
    }

    val requestLocationAndDetect = {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.detectUserLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Auto-detect user location on launch if permission granted
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.detectUserLocation()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Weather Screen
            MainWeatherScreen(
                weatherUiState = weatherUiState,
                selectedHourIndex = selectedHourIndex,
                temperatureUnit = temperatureUnit,
                onHourSelected = { viewModel.setSelectedHourIndex(it) },
                onToggleUnit = { viewModel.toggleTemperatureUnit() },
                onOpenCitySheet = { showCitySheet = true },
                onOpenDetailsCard = { showDetailsScreen = true },
                onDetectLocation = { requestLocationAndDetect() },
                onRefresh = { viewModel.refreshWeather() }
            )

            // Detailed Forecast Screen Overlay
            AnimatedVisibility(
                visible = showDetailsScreen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
            ) {
                if (weatherUiState is WeatherUiState.Success) {
                    DetailedForecastScreen(
                        data = (weatherUiState as WeatherUiState.Success).data,
                        selectedHourIndex = selectedHourIndex,
                        temperatureUnit = temperatureUnit,
                        pinnedWidgets = pinnedWidgets,
                        onToggleWidgetPin = { viewModel.toggleWidgetPin(it) },
                        onHourSelected = { viewModel.setSelectedHourIndex(it) },
                        onClose = { showDetailsScreen = false }
                    )
                }
            }

            // City Selection Bottom Sheet
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
                    onDetectLocation = { requestLocationAndDetect() },
                    onDismiss = {
                        viewModel.updateSearchQuery("")
                        showCitySheet = false
                    }
                )
            }
        }
    }
}
