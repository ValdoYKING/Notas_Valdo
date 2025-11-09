package com.valdo.notasinteligentesvaldo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore de preferencias de UI (filtro, categoría seleccionada y última ruta)
val Context.uiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ui_prefs")

object UiPrefs {
    private val KEY_FILTER = stringPreferencesKey("filter_type")
    private val KEY_CATEGORY_ID = intPreferencesKey("selected_category_id")
    private val KEY_LAST_ROUTE = stringPreferencesKey("last_route")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // valores: system, light, dark, dark_plus

    // Nuevas claves para perfil de usuario
    private val KEY_FIRST_NAME = stringPreferencesKey("first_name")
    private val KEY_LAST_NAME = stringPreferencesKey("last_name")
    private val KEY_BIRTH_DATE = stringPreferencesKey("birth_date") // formato ISO: yyyy-MM-dd
    private val KEY_PROFILE_URI = stringPreferencesKey("profile_uri") // uri de la imagen como string
    private val KEY_START_ACTION = stringPreferencesKey("start_action") // 'notes' | 'quick_note'

    fun filterTypeFlow(context: Context): Flow<String> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_FILTER] ?: "all" }

    fun selectedCategoryIdFlow(context: Context): Flow<Int?> =
        context.uiDataStore.data.map { prefs ->
            val v = prefs[KEY_CATEGORY_ID] ?: -1
            if (v >= 0) v else null
        }

    fun lastRouteFlow(context: Context): Flow<String?> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_LAST_ROUTE] }

    fun themeModeFlow(context: Context): Flow<String> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_THEME_MODE] ?: "system" }

    // Flows nuevos para perfil
    fun firstNameFlow(context: Context): Flow<String> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_FIRST_NAME] ?: "" }

    fun lastNameFlow(context: Context): Flow<String> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_LAST_NAME] ?: "" }

    fun birthDateFlow(context: Context): Flow<String> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_BIRTH_DATE] ?: "" }

    fun profileImageUriFlow(context: Context): Flow<String> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_PROFILE_URI] ?: "" }

    fun startActionFlow(context: Context): Flow<String> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_START_ACTION] ?: "notes" }

    suspend fun setFilterType(context: Context, filter: String) {
        context.uiDataStore.edit { it[KEY_FILTER] = filter }
    }

    suspend fun setSelectedCategoryId(context: Context, categoryId: Int?) {
        context.uiDataStore.edit { prefs ->
            if (categoryId == null) prefs.remove(KEY_CATEGORY_ID) else prefs[KEY_CATEGORY_ID] = categoryId
        }
    }

    suspend fun setLastRoute(context: Context, route: String) {
        // Limitar tamaño por seguridad
        val safe = route.take(200)
        context.uiDataStore.edit { it[KEY_LAST_ROUTE] = safe }
    }

    suspend fun setThemeMode(context: Context, mode: String) {
        // mode: "system" | "light" | "dark" | "dark_plus"
        val safe = when (mode) {
            "light", "dark", "dark_plus" -> mode
            else -> "system"
        }
        context.uiDataStore.edit { it[KEY_THEME_MODE] = safe }
    }

    // Setters para perfil
    suspend fun setFirstName(context: Context, name: String) {
        context.uiDataStore.edit { it[KEY_FIRST_NAME] = name }
    }

    suspend fun setLastName(context: Context, name: String) {
        context.uiDataStore.edit { it[KEY_LAST_NAME] = name }
    }

    suspend fun setBirthDate(context: Context, isoDate: String) {
        context.uiDataStore.edit { it[KEY_BIRTH_DATE] = isoDate }
    }

    suspend fun setProfileImageUri(context: Context, uriString: String) {
        context.uiDataStore.edit { it[KEY_PROFILE_URI] = uriString }
    }

    suspend fun setStartAction(context: Context, action: String) {
        val safe = if (action == "quick_note") "quick_note" else "notes"
        context.uiDataStore.edit { it[KEY_START_ACTION] = safe }
    }
}
