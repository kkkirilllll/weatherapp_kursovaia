package com.ezhovkirill.myapplication.ui

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ezhovkirill.myapplication.data.GeocodingApi
import com.ezhovkirill.myapplication.data.WeatherApi
import com.ezhovkirill.myapplication.data.WeatherResponse
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

data class WeatherUiState(
    val currentTemp: String = "--",
    val condition: String = "Загрузка...",
    val highLow: String = "H:-- L:--",
    val city: String = "Определение...",
    val hourly: List<HourlyUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSearching: Boolean = false,
    val searchResults: List<SearchResultItem> = emptyList(),
    val suggestedCities: List<SearchResultItem> = emptyList()
)

data class SearchResultItem(
    val name: String,
    val description: String,
    val lat: Double,
    val lon: Double
)

data class HourlyUiItem(
    val time: String,
    val temp: String,
    val iconRes: Int, // We will map this to our drawable resources later, for now using int or we can pass the URL string if we had one
    val isActive: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val weatherApi: WeatherApi
    private val geocodingApi: GeocodingApi

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val defaultCities = listOf(
        SearchResultItem("Москва", "Россия", 55.7558, 37.6173),
        SearchResultItem("Санкт-Петербург", "Россия", 59.9343, 30.3351),
        SearchResultItem("Новосибирск", "Россия", 55.0084, 82.9357),
        SearchResultItem("Екатеринбург", "Россия", 56.8389, 60.6057),
        SearchResultItem("Казань", "Россия", 55.7961, 49.1064),
        SearchResultItem("Нижний Новгород", "Россия", 56.3269, 44.0059),
        SearchResultItem("Челябинск", "Россия", 55.1644, 61.4368),
        SearchResultItem("Самара", "Россия", 53.2415, 50.2212),
        SearchResultItem("Омск", "Россия", 54.9885, 73.3242),
        SearchResultItem("Ростов-на-Дону", "Россия", 47.2357, 39.7015)
    )

    init {
        _uiState.value = _uiState.value.copy(suggestedCities = defaultCities)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        weatherApi = retrofit.create(WeatherApi::class.java)

        val geocodingRetrofit = Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        geocodingApi = geocodingRetrofit.create(GeocodingApi::class.java)
        
        fetchLocationAndWeather()
    }

    fun searchCity(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSearching = true)
                val response = geocodingApi.searchCity(query)
                
                val results = response.results?.map { 
                    SearchResultItem(
                        name = it.name,
                        description = listOfNotNull(it.admin1, it.country).joinToString(", "),
                        lat = it.latitude,
                        lon = it.longitude
                    )
                } ?: emptyList()
                
                _uiState.value = _uiState.value.copy(searchResults = results)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка поиска: ${e.message}")
            }
        }
    }

    fun selectCity(item: SearchResultItem) {
        _uiState.value = _uiState.value.copy(
            isSearching = false, 
            searchResults = emptyList(),
            city = item.name,
            condition = "Загрузка..."
        )
        viewModelScope.launch {
            fetchWeather(item.lat, item.lon, item.name)
        }
    }

    fun closeSearch() {
        _uiState.value = _uiState.value.copy(isSearching = false, searchResults = emptyList())
    }

    @SuppressLint("MissingPermission")
    fun fetchLocationAndWeather() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, condition = "Определение места...")
                
                // Check permissions before calling this in UI, but here we assume we might have them or fail
                // In a real app, we'd handle permission results more gracefully.
                // For this demo, we'll try to get the last location or current location.
                
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    
                    // Get City Name
                    val geocoder = Geocoder(getApplication(), Locale("ru"))
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val cityName = addresses?.firstOrNull()?.locality ?: "Неизвестно"

                    _uiState.value = _uiState.value.copy(city = cityName, condition = "Загрузка погоды...")

                    fetchWeather(lat, lon, cityName)
                } else {
                     _uiState.value = _uiState.value.copy(isLoading = false, error = "Не удалось получить местоположение", condition = "Ошибка")
                }
            } catch (e: Exception) {
                 _uiState.value = _uiState.value.copy(isLoading = false, error = e.message, condition = "Ошибка")
            }
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double, cityName: String) {
        try {
            val response = weatherApi.getWeather(lat, lon)
            
            val currentTemp = response.current.temperature.toInt().toString() + "°"
            val conditionText = getWeatherDescription(response.current.weatherCode)
            val maxTemp = response.daily.maxTemps.firstOrNull()?.toInt() ?: 0
            val minTemp = response.daily.minTemps.firstOrNull()?.toInt() ?: 0
            val highLow = "Макс:$maxTemp° Мин:$minTemp°"

            // Process hourly
            // The API returns 24 hours or more. We just want the next few hours.
            // We need to find the current hour index.
            // For simplicity, we'll just take the first 7 items from the list assuming the API returns from current time or 00:00
            // Open-Meteo returns hourly data starting from 00:00 of the current day usually.
            // We should find the current hour.
            
            val calendar = java.util.Calendar.getInstance()
            val currentHourIso = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            // This is a simplification. Ideally we parse the time strings.
            
            val hourlyItems = response.hourly.time.mapIndexed { index, timeStr ->
                // timeStr is "2023-10-27T00:00"
                val hour = timeStr.substringAfter("T").substringBefore(":")
                val temp = response.hourly.temperatures[index].toInt().toString() + "°"
                val code = response.hourly.weatherCodes[index]
                
                HourlyUiItem(
                    time = if (hour.toInt() == currentHourIso) "Сейчас" else "$hour:00",
                    temp = temp,
                    iconRes = code, // Placeholder
                    isActive = hour.toInt() == currentHourIso
                )
            }.filter { 
                // Filter to show only current and future hours (next 24h)
                // Simplified: just take a slice around current time
                true 
            }.dropWhile { 
                 !it.time.equals("Сейчас") && it.time.substringBefore(":").toInt() < currentHourIso
            }.take(7)

            _uiState.value = WeatherUiState(
                currentTemp = currentTemp,
                condition = conditionText,
                highLow = highLow,
                city = cityName,
                hourly = hourlyItems,
                isLoading = false
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Ошибка сети: ${e.message}", condition = "Ошибка")
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Ясно"
            1, 2, 3 -> "Преимущественно ясно"
            45, 48 -> "Туман"
            51, 53, 55 -> "Морось"
            56, 57 -> "Ледяная морось"
            61, 63, 65 -> "Дождь"
            66, 67 -> "Ледяной дождь"
            71, 73, 75 -> "Снегопад"
            77 -> "Снежные зерна"
            80, 81, 82 -> "Ливень"
            85, 86 -> "Снежный ливень"
            95 -> "Гроза"
            96, 99 -> "Гроза с градом"
            else -> "Неизвестно"
        }
    }
}
