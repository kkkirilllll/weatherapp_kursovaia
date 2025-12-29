package com.ezhovkirill.myapplication.data

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("current") current: String = "temperature_2m,weather_code,apparent_temperature,relative_humidity_2m,surface_pressure,wind_speed_10m,wind_direction_10m",
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
        @Query("precipitation_unit") precipitationUnit: String = "mm"
    ): WeatherResponse
}
