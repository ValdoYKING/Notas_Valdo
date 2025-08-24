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

    fun filterTypeFlow(context: Context): Flow<String> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_FILTER] ?: "all" }

    fun selectedCategoryIdFlow(context: Context): Flow<Int?> =
        context.uiDataStore.data.map { prefs ->
            val v = prefs[KEY_CATEGORY_ID] ?: -1
            if (v >= 0) v else null
        }

    fun lastRouteFlow(context: Context): Flow<String?> =
        context.uiDataStore.data.map { prefs -> prefs[KEY_LAST_ROUTE] }

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
}
