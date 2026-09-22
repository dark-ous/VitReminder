package com.vitreminder.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.vitreminder.app.data.model.Timetable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vit_reminder_prefs")

data class UserSettings(
    val selectedBatch: String = "B3",
    val divisionName: String = "FY CS-H",
    val leadTimeMinutes: Int = 10,
    val preClassEnabled: Boolean = true,
    val breakAlertsEnabled: Boolean = true,
    val dayEndAlertsEnabled: Boolean = true,
    val morningOverviewEnabled: Boolean = true,
    val isOnboarded: Boolean = true
)

class PreferencesManager(private val context: Context) {
    private val gson = Gson()

    private object PreferencesKeys {
        val SELECTED_BATCH = stringPreferencesKey("selected_batch")
        val DIVISION_NAME = stringPreferencesKey("division_name")
        val LEAD_TIME_MINUTES = intPreferencesKey("lead_time_minutes")
        val PRE_CLASS_ENABLED = booleanPreferencesKey("pre_class_enabled")
        val BREAK_ALERTS_ENABLED = booleanPreferencesKey("break_alerts_enabled")
        val DAY_END_ALERTS_ENABLED = booleanPreferencesKey("day_end_alerts_enabled")
        val MORNING_OVERVIEW_ENABLED = booleanPreferencesKey("morning_overview_enabled")
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val HAS_CLEARED = booleanPreferencesKey("has_cleared")
        val TIMETABLE_JSON = stringPreferencesKey("timetable_json")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hasExplicitlyCleared = preferences[PreferencesKeys.HAS_CLEARED] ?: false
            val isOnboarded = if (hasExplicitlyCleared) {
                preferences[PreferencesKeys.IS_ONBOARDED] ?: false
            } else {
                preferences[PreferencesKeys.IS_ONBOARDED] ?: true
            }

            UserSettings(
                selectedBatch = preferences[PreferencesKeys.SELECTED_BATCH] ?: "B3",
                divisionName = preferences[PreferencesKeys.DIVISION_NAME] ?: "FY CS-H",
                leadTimeMinutes = preferences[PreferencesKeys.LEAD_TIME_MINUTES] ?: 10,
                preClassEnabled = preferences[PreferencesKeys.PRE_CLASS_ENABLED] ?: true,
                breakAlertsEnabled = preferences[PreferencesKeys.BREAK_ALERTS_ENABLED] ?: true,
                dayEndAlertsEnabled = preferences[PreferencesKeys.DAY_END_ALERTS_ENABLED] ?: true,
                morningOverviewEnabled = preferences[PreferencesKeys.MORNING_OVERVIEW_ENABLED] ?: true,
                isOnboarded = isOnboarded
            )
        }

    val cachedTimetableFlow: Flow<Timetable?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hasExplicitlyCleared = preferences[PreferencesKeys.HAS_CLEARED] ?: false
            val json = preferences[PreferencesKeys.TIMETABLE_JSON]
            val timetable = if (!json.isNullOrBlank()) {
                try {
                    gson.fromJson(json, Timetable::class.java)
                } catch (e: Exception) {
                    android.util.Log.e("PreferencesManager", "Failed to deserialize cached timetable JSON: ${e.message}", e)
                    null
                }
            } else if (!hasExplicitlyCleared) {
                // First run default: load bundled FY CS-H timetable from assets
                loadBundledDefault()
            } else {
                null
            }
            memoryCachedTimetable = timetable
            timetable
        }

    @Volatile
    private var memoryCachedTimetable: Timetable? = null

    fun getFastTimetable(): Timetable? {
        return memoryCachedTimetable ?: loadBundledDefault()
    }

    fun loadBundledDefault(): Timetable? {
        return try {
            val json = context.assets.open("default_timetable.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, Timetable::class.java).also {
                memoryCachedTimetable = it
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveTimetable(timetable: Timetable, batch: String) {
        memoryCachedTimetable = timetable
        val json = gson.toJson(timetable)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMETABLE_JSON] = json
            preferences[PreferencesKeys.DIVISION_NAME] = timetable.division
            preferences[PreferencesKeys.SELECTED_BATCH] = batch
            preferences[PreferencesKeys.IS_ONBOARDED] = true
            preferences[PreferencesKeys.HAS_CLEARED] = false
        }
    }

    suspend fun updateBatch(batch: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_BATCH] = batch
        }
    }

    suspend fun updateLeadTime(leadMinutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEAD_TIME_MINUTES] = leadMinutes
        }
    }

    suspend fun updateNotificationToggles(
        preClass: Boolean,
        breaks: Boolean,
        dayEnd: Boolean,
        morning: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRE_CLASS_ENABLED] = preClass
            preferences[PreferencesKeys.BREAK_ALERTS_ENABLED] = breaks
            preferences[PreferencesKeys.DAY_END_ALERTS_ENABLED] = dayEnd
            preferences[PreferencesKeys.MORNING_OVERVIEW_ENABLED] = morning
        }
    }

    suspend fun clearAll() {
        memoryCachedTimetable = null
        context.dataStore.edit { preferences ->
            preferences.clear()
            preferences[PreferencesKeys.HAS_CLEARED] = true
            preferences[PreferencesKeys.IS_ONBOARDED] = false
        }
    }
}
