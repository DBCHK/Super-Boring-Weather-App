package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    val weatherUiState by viewModel.weatherUiState.collectAsState()
    val selectedHourIndex by viewModel.selectedHourIndex.collectAsState()
    val temperatureUnit by viewModel.temperatureUnit.collectAsState()
    val savedCities by viewModel.savedCities.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var showDetailsScreen by remember { mutableStateOf(false) }
    var showCitySheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                onRefresh = { viewModel.refreshWeather() }
            )

            // Detailed Forecast Screen Overlay
            AnimatedVisibility(
                visible = showDetailsScreen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
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
                    onDismiss = {
                        viewModel.updateSearchQuery("")
                        showCitySheet = false
                    }
                )
            }
        }
    }
}
