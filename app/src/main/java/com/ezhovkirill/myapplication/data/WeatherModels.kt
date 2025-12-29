package com.ezhovkirill.myapplication.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current") val current: CurrentWeather,
    @SerializedName("hourly") val hourly: HourlyWeather,
    @SerializedName("daily") val daily: DailyWeather
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("apparent_temperature") val apparentTemperature: Double?,
    @SerializedName("relative_humidity_2m") val humidity: Int?,
    @SerializedName("surface_pressure") val pressure: Double?,
    @SerializedName("wind_speed_10m") val windSpeed: Double?,
    @SerializedName("wind_direction_10m") val windDirection: Int?
)

data class HourlyWeather(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temperatures: List<Double>,
    @SerializedName("weather_code") val weatherCodes: List<Int>
)

data class DailyWeather(
    @SerializedName("time") val time: List<String>,
    @SerializedName("weather_code") val weatherCodes: List<Int>,
    @SerializedName("temperature_2m_max") val maxTemps: List<Double>,
    @SerializedName("temperature_2m_min") val minTemps: List<Double>,
    @SerializedName("sunrise") val sunrise: List<String>?,
    @SerializedName("sunset") val sunset: List<String>?
)

data class GeocodingResponse(
    @SerializedName("results") val results: List<GeocodingResult>?
)

data class GeocodingResult(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("country") val country: String?,
    @SerializedName("admin1") val admin1: String?
)

