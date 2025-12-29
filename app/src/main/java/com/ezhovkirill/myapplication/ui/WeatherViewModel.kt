package com.ezhovkirill.myapplication.ui

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ezhovkirill.myapplication.data.GeocodingApi
import com.ezhovkirill.myapplication.data.GeocodingResult
import com.ezhovkirill.myapplication.data.UserPreferencesRepository
import com.ezhovkirill.myapplication.data.WeatherApi
import com.ezhovkirill.myapplication.data.WeatherResponse
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

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
    val suggestedCities: List<SearchResultItem> = emptyList(),
    
    // New fields
    val apparentTemp: String = "--",
    val humidity: String = "--",
    val pressure: String = "--",
    val windSpeed: String = "--",
    val windDirection: String = "--",
    val sunrise: String = "--",
    val sunset: String = "--",
    
    // Settings & Favorites
    val isDarkTheme: Boolean = false,
    val isImperialUnits: Boolean = false,
    val favoriteCities: List<GeocodingResult> = emptyList(),
    val currentCityId: Int? = null,
    val dailyForecast: List<DailyUiItem> = emptyList()
)

data class DailyUiItem(
    val day: String,
    val maxTemp: String,
    val minTemp: String,
    val iconRes: Int
)

data class SearchResultItem(
    val name: String,
    val description: String,
    val lat: Double,
    val lon: Double,
    val id: Int = 0,
    val country: String? = null,
    val admin1: String? = null
)

data class HourlyUiItem(
    val time: String,
    val temp: String,
    val iconRes: Int, 
    val isActive: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val weatherApi: WeatherApi
    private val geocodingApi: GeocodingApi
    private val userPreferencesRepository: UserPreferencesRepository

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var currentCityName: String = ""

    private val defaultCities = listOf(
        SearchResultItem("Москва", "Россия", 55.7558, 37.6173, 524901),
        SearchResultItem("Санкт-Петербург", "Россия", 59.9343, 30.3351, 498817),
        SearchResultItem("Новосибирск", "Россия", 55.0084, 82.9357, 1496747),
        SearchResultItem("Екатеринбург", "Россия", 56.8389, 60.6057, 1486209),
        SearchResultItem("Казань", "Россия", 55.7961, 49.1064, 551487),
        SearchResultItem("Нижний Новгород", "Россия", 56.3269, 44.0059, 520555),
        SearchResultItem("Челябинск", "Россия", 55.1644, 61.4368, 1508291),
        SearchResultItem("Самара", "Россия", 53.2415, 50.2212, 499099),
        SearchResultItem("Омск", "Россия", 54.9885, 73.3242, 1496153),
        SearchResultItem("Ростов-на-Дону", "Россия", 47.2357, 39.7015, 501175)
    )

    init {
        _uiState.value = _uiState.value.copy(suggestedCities = defaultCities)
        userPreferencesRepository = UserPreferencesRepository(application)

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
        
        // Observe preferences
        viewModelScope.launch {
            userPreferencesRepository.isDarkTheme.collectLatest { isDark ->
                _uiState.value = _uiState.value.copy(isDarkTheme = isDark)
            }
        }
        
        viewModelScope.launch {
            userPreferencesRepository.isImperialUnits.collectLatest { isImperial ->
                _uiState.value = _uiState.value.copy(isImperialUnits = isImperial)
                if (currentLat != 0.0 && currentLon != 0.0) {
                    fetchWeather(currentLat, currentLon, currentCityName)
                }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.favoriteCities.collectLatest { favorites ->
                _uiState.value = _uiState.value.copy(favoriteCities = favorites)
            }
        }

        fetchLocationAndWeather()
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkTheme(isDark)
        }
    }

    fun toggleUnits(isImperial: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setImperialUnits(isImperial)
        }
    }

    fun toggleFavorite() {
        val currentCityName = _uiState.value.city
        // We need the full GeocodingResult to save. 
        // Ideally, we should store the current GeocodingResult in the state.
        // For now, let's try to find it in favorites or construct a basic one if we are at current location
        // This is a simplification. Better approach: Store currentGeocodingResult in state.
        
        // If we are viewing a searched city, we might have its details.
        // If we are at GPS location, we might not have a stable ID.
        // Let's assume we can only favorite searched cities or we need to reverse geocode current location to get ID.
        // For this task, let's implement adding from search results or if we have an ID.
    }

    fun addToFavorites(city: SearchResultItem) {
        viewModelScope.launch {
            val geoResult = GeocodingResult(
                id = city.id,
                name = city.name,
                latitude = city.lat,
                longitude = city.lon,
                country = city.country,
                admin1 = city.admin1
            )
            userPreferencesRepository.addFavoriteCity(geoResult)
        }
    }

    fun removeFromFavorites(cityId: Int) {
        viewModelScope.launch {
            userPreferencesRepository.removeFavoriteCity(cityId)
        }
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
                        lon = it.longitude,
                        id = it.id,
                        country = it.country,
                        admin1 = it.admin1
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
            condition = "Загрузка...",
            currentCityId = item.id
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
        currentLat = lat
        currentLon = lon
        currentCityName = cityName
        try {
            val tempUnit = if (_uiState.value.isImperialUnits) "fahrenheit" else "celsius"
            val windUnit = if (_uiState.value.isImperialUnits) "mph" else "kmh"

            val response = weatherApi.getWeather(
                lat = lat, 
                long = lon,
                temperatureUnit = tempUnit,
                windSpeedUnit = windUnit
            )
            
            val currentTemp = response.current.temperature.roundToInt().toString() + "°"
            val conditionText = getWeatherDescription(response.current.weatherCode)
            val maxTemp = response.daily.maxTemps.firstOrNull()?.roundToInt() ?: 0
            val minTemp = response.daily.minTemps.firstOrNull()?.roundToInt() ?: 0
            val highLow = "Макс:$maxTemp° Мин:$minTemp°"

            // New fields
            val apparentTemp = "${response.current.apparentTemperature?.roundToInt() ?: "--"}°"
            val humidity = "${response.current.humidity ?: "--"}%"
            val pressure = "${response.current.pressure?.roundToInt() ?: "--"} гПа"
            val windSpeed = "${response.current.windSpeed?.roundToInt() ?: "--"} ${if (_uiState.value.isImperialUnits) "mph" else "км/ч"}"
            val windDirection = getWindDirection(response.current.windDirection)
            val sunrise = response.daily.sunrise?.firstOrNull()?.substringAfter("T") ?: "--:--"
            val sunset = response.daily.sunset?.firstOrNull()?.substringAfter("T") ?: "--:--"

            // Process hourly
            val calendar = java.util.Calendar.getInstance()
            val currentHourIso = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            val hourlyItems = response.hourly.time.mapIndexed { index, timeStr ->
                val hour = timeStr.substringAfter("T").substringBefore(":")
                val temp = response.hourly.temperatures[index].roundToInt().toString() + "°"
                val code = response.hourly.weatherCodes[index]
                
                HourlyUiItem(
                    time = if (hour.toInt() == currentHourIso) "Сейчас" else "$hour:00",
                    temp = temp,
                    iconRes = code,
                    isActive = hour.toInt() == currentHourIso
                )
            }.dropWhile { 
                 !it.time.equals("Сейчас") && it.time.substringBefore(":").toInt() < currentHourIso
            }.take(24)

            // Process daily
            val dailyItems = response.daily.time.mapIndexed { index, timeStr ->
                val date = LocalDate.parse(timeStr)
                val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru"))
                val max = response.daily.maxTemps[index].roundToInt().toString() + "°"
                val min = response.daily.minTemps[index].roundToInt().toString() + "°"
                val code = response.daily.weatherCodes[index]
                
                DailyUiItem(
                    day = dayOfWeek.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    maxTemp = max,
                    minTemp = min,
                    iconRes = code
                )
            }

            _uiState.value = _uiState.value.copy(
                currentTemp = currentTemp,
                condition = conditionText,
                highLow = highLow,
                city = cityName,
                hourly = hourlyItems,
                dailyForecast = dailyItems,
                isLoading = false,
                apparentTemp = apparentTemp,
                humidity = humidity,
                pressure = pressure,
                windSpeed = windSpeed,
                windDirection = windDirection,
                sunrise = sunrise,
                sunset = sunset
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Ошибка сети: ${e.message}", condition = "Ошибка")
        }
    }

    private fun getWindDirection(degrees: Int?): String {
        if (degrees == null) return "--"
        val directions = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
        return directions[((degrees + 22.5) / 45.0).toInt() % 8]
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
