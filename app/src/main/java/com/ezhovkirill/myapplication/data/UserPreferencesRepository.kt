package com.ezhovkirill.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    private val gson = Gson()

    companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val IS_IMPERIAL_UNITS = booleanPreferencesKey("is_imperial_units")
        val FAVORITE_CITIES = stringPreferencesKey("favorite_cities")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_THEME] ?: false // Default to system (handled in UI logic usually, but here false means light/system default)
        }

    val isImperialUnits: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_IMPERIAL_UNITS] ?: false
        }

    val favoriteCities: Flow<List<GeocodingResult>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[FAVORITE_CITIES] ?: "[]"
            val type = object : TypeToken<List<GeocodingResult>>() {}.type
            gson.fromJson(json, type)
        }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_THEME] = isDark
        }
    }

    suspend fun setImperialUnits(isImperial: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_IMPERIAL_UNITS] = isImperial
        }
    }

    suspend fun addFavoriteCity(city: GeocodingResult) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[FAVORITE_CITIES] ?: "[]"
            val type = object : TypeToken<MutableList<GeocodingResult>>() {}.type
            val currentList: MutableList<GeocodingResult> = gson.fromJson(currentJson, type)
            
            if (currentList.none { it.id == city.id }) {
                currentList.add(city)
                preferences[FAVORITE_CITIES] = gson.toJson(currentList)
            }
        }
    }

    suspend fun removeFavoriteCity(cityId: Int) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[FAVORITE_CITIES] ?: "[]"
            val type = object : TypeToken<MutableList<GeocodingResult>>() {}.type
            val currentList: MutableList<GeocodingResult> = gson.fromJson(currentJson, type)
            
            currentList.removeAll { it.id == cityId }
            preferences[FAVORITE_CITIES] = gson.toJson(currentList)
        }
    }
}
