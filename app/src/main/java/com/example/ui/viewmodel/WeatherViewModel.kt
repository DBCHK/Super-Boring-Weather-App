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

    private val _selectedHourIndex = MutableStateFlow(0)
    val selectedHourIndex: StateFlow<Int> = _selectedHourIndex.asStateFlow()

    private val _temperatureUnit = MutableStateFlow(TemperatureUnit.FAHRENHEIT)
    val temperatureUnit: StateFlow<TemperatureUnit> = _temperatureUnit.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CityEntity>>(emptyList())
    val searchResults: StateFlow<List<CityEntity>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

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
    }

    fun selectCity(city: CityEntity) {
        _selectedCity.value = city
        _selectedHourIndex.value = 0
        loadWeatherForSelectedCity()
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

    fun refreshWeather() {
        loadWeatherForSelectedCity()
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

    private fun loadWeatherForSelectedCity() {
        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState.Loading
            try {
                val data = repository.fetchWeather(_selectedCity.value)
                _weatherUiState.value = WeatherUiState.Success(data)
            } catch (e: Exception) {
                _weatherUiState.value = WeatherUiState.Error(e.localizedMessage ?: "Failed to load weather")
            }
        }
    }
}
