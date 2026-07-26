package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CityEntity
import com.example.data.model.WeatherForecastData
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TemperatureUnit {
    FAHRENHEIT, CELSIUS
}

enum class WidgetType(val title: String, val description: String) {
    RED_SUN_ORB("Hero Red Sun Orb", "3D animated sun sphere with live temperature"),
    THREE_DAY_FORECAST("3-Day Forecast", "Compact 3-day weather mini table"),
    WIDE_RAIN_BANNER("Wide Rain & Temp Banner", "Full width banner with live condition and temperature bar"),
    AIR_QUALITY("Air Quality Index", "Live AQI monitor widget"),
    MOON_PHASE("3D Moon Phase", "Live lunar illumination and phase graphic"),
    WIND_COMPASS("Wind & Direction", "Interactive wind compass widget"),
    UV_METER("UV Index Meter", "Realtime ultraviolet risk indicator")
}

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val data: WeatherForecastData) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = WeatherRepository(db.cityDao())

    val savedCities: StateFlow<List<CityEntity>> = repository.savedCities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.defaultCities
        )

    private val _selectedCity = MutableStateFlow(repository.defaultCities.first())
    val selectedCity: StateFlow<CityEntity> = _selectedCity.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    /** Epoch millis of last successful weather fetch (0 = never). */
    private val _lastRefreshAtMs = MutableStateFlow(0L)
    val lastRefreshAtMs: StateFlow<Long> = _lastRefreshAtMs.asStateFlow()

    private val _selectedHourIndex = MutableStateFlow(0)
    val selectedHourIndex: StateFlow<Int> = _selectedHourIndex.asStateFlow()

    private val _temperatureUnit = MutableStateFlow(TemperatureUnit.CELSIUS)
    val temperatureUnit: StateFlow<TemperatureUnit> = _temperatureUnit.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CityEntity>>(emptyList())
    val searchResults: StateFlow<List<CityEntity>> = _searchResults.asStateFlow()
    val searchQueryResults: StateFlow<List<CityEntity>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Pinned Widgets State for user customization
    private val _pinnedWidgets = MutableStateFlow(
        listOf(
            WidgetType.RED_SUN_ORB,
            WidgetType.THREE_DAY_FORECAST,
            WidgetType.WIDE_RAIN_BANNER,
            WidgetType.AIR_QUALITY,
            WidgetType.MOON_PHASE,
            WidgetType.WIND_COMPASS,
            WidgetType.UV_METER
        )
    )
    val pinnedWidgets: StateFlow<List<WidgetType>> = _pinnedWidgets.asStateFlow()

    private var searchJob: Job? = null
    private var weatherJob: Job? = null

    init {
        // Pre-populate default cities into Room DB if empty
        viewModelScope.launch {
            savedCities.collect { list ->
                if (list.isEmpty()) {
                    repository.defaultCities.forEach { repository.saveCity(it) }
                }
            }
        }
        loadWeatherForSelectedCity()

        // 39-Minute Dynamic Auto-Refresh Loop
        viewModelScope.launch {
            while (true) {
                delay(39 * 60 * 1000L) // Refresh weather data dynamically every 39 minutes
                loadWeatherForSelectedCity()
            }
        }
    }

    fun toggleWidgetPin(widget: WidgetType) {
        val current = _pinnedWidgets.value.toMutableList()
        if (current.contains(widget)) {
            current.remove(widget)
        } else {
            current.add(widget)
        }
        _pinnedWidgets.value = current
    }

    /**
     * Result of an auto-detect attempt so the UI can prompt for services/permission.
     */
    sealed class LocationDetectResult {
        data object Success : LocationDetectResult()
        data object ServicesDisabled : LocationDetectResult()
        data object PermissionRequired : LocationDetectResult()
        data class Failed(val message: String) : LocationDetectResult()
    }

    suspend fun detectUserLocationResult(): LocationDetectResult {
        _weatherUiState.value = WeatherUiState.Loading
        val locationHelper = com.example.util.LocationHelper(getApplication())
        return when (val result = locationHelper.resolveLocation()) {
            is com.example.util.LocationResult.Success -> {
                _selectedCity.value = result.city
                _selectedHourIndex.value = 0
                repository.saveCity(result.city)
                loadWeatherForCity(result.city)
                LocationDetectResult.Success
            }
            is com.example.util.LocationResult.ServicesDisabled -> {
                // Stay on previous city weather, not SF fallback
                loadWeatherForSelectedCity()
                LocationDetectResult.ServicesDisabled
            }
            is com.example.util.LocationResult.PermissionRequired -> {
                loadWeatherForSelectedCity()
                LocationDetectResult.PermissionRequired
            }
            is com.example.util.LocationResult.Failed -> {
                loadWeatherForSelectedCity()
                LocationDetectResult.Failed(result.message)
            }
        }
    }

    fun detectUserLocation() {
        viewModelScope.launch {
            detectUserLocationResult()
        }
    }

    fun selectCity(city: CityEntity) {
        _selectedCity.value = city
        _selectedHourIndex.value = 0
        loadWeatherForCity(city)
    }

    fun toggleTemperatureUnit() {
        _temperatureUnit.value = if (_temperatureUnit.value == TemperatureUnit.FAHRENHEIT) {
            TemperatureUnit.CELSIUS
        } else {
            TemperatureUnit.FAHRENHEIT
        }
    }

    fun setSelectedHourIndex(index: Int) {
        _selectedHourIndex.value = index
    }

    /**
     * @param showLoading when false, keep showing the last successful forecast while refreshing
     * (used on app resume so the UI does not flash a full loading state).
     */
    fun refreshWeather(showLoading: Boolean = true) {
        loadWeatherForSelectedCity(showLoading = showLoading)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        _isSearching.value = true
        searchJob = viewModelScope.launch {
            delay(300) // Debounce search
            val results = repository.searchCities(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun saveCity(city: CityEntity) {
        viewModelScope.launch {
            repository.saveCity(city)
            selectCity(city)
        }
    }

    fun deleteCity(city: CityEntity) {
        viewModelScope.launch {
            repository.deleteCity(city)
        }
    }

    private fun loadWeatherForSelectedCity(showLoading: Boolean = true) {
        loadWeatherForCity(_selectedCity.value, showLoading = showLoading)
    }

    private fun loadWeatherForCity(city: CityEntity, showLoading: Boolean = true) {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            // Only blank the screen when we have nothing useful to show yet
            if (showLoading || _weatherUiState.value !is WeatherUiState.Success) {
                _weatherUiState.value = WeatherUiState.Loading
            }
            try {
                // Capture city at call time so a slow response cannot overwrite a newer selection
                val data = repository.fetchWeather(city)
                if (_selectedCity.value.id == city.id) {
                    _weatherUiState.value = WeatherUiState.Success(data)
                    _lastRefreshAtMs.value = System.currentTimeMillis()
                    _selectedHourIndex.value = 0
                    // Keep home-screen widgets in sync
                    com.example.widget.WidgetUpdateHelper.publishFromApp(
                        getApplication(),
                        com.example.widget.WidgetSnapshot.from(data)
                    )
                }
            } catch (e: Exception) {
                if (_selectedCity.value.id == city.id) {
                    // Keep prior Success if a background refresh fails
                    if (_weatherUiState.value !is WeatherUiState.Success) {
                        _weatherUiState.value = WeatherUiState.Error(
                            e.localizedMessage ?: "Failed to load weather for ${city.name}"
                        )
                    }
                }
            }
        }
    }
}
