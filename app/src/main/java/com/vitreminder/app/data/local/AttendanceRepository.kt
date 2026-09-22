package com.vitreminder.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vitreminder.app.data.model.CourseAttendance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.attendanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "vit_attendance_prefs")

class AttendanceRepository(private val context: Context) {
    private val gson = Gson()
    private val attendanceKey = stringPreferencesKey("attendance_records_json")

    val attendanceFlow: Flow<List<CourseAttendance>> = context.attendanceDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val json = preferences[attendanceKey]
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<CourseAttendance>>() {}.type
                    gson.fromJson<List<CourseAttendance>>(json, type) ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("AttendanceRepository", "Failed to deserialize attendance: ${e.message}", e)
                    emptyList()
                }
            }
        }

    suspend fun getAllAttendance(): List<CourseAttendance> {
        return try {
            attendanceFlow.first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun recordAttendance(courseCode: String, courseTitle: String, attended: Boolean) {
        val current = getAllAttendance().toMutableList()
        val index = current.indexOfFirst { it.courseCode.equals(courseCode, ignoreCase = true) }

        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(
                attendedClasses = existing.attendedClasses + if (attended) 1 else 0,
                totalClasses = existing.totalClasses + 1
            )
        } else {
            current.add(
                CourseAttendance(
                    courseCode = courseCode,
                    courseTitle = courseTitle,
                    attendedClasses = if (attended) 1 else 0,
                    totalClasses = 1
                )
            )
        }
        saveAttendance(current)
    }

    suspend fun autoSyncCourses(courses: List<Pair<String, String>>) {
        val current = getAllAttendance().toMutableList()
        var modified = false

        for ((code, title) in courses) {
            if (code.isBlank()) continue
            val exists = current.any { it.courseCode.equals(code, ignoreCase = true) }
            if (!exists) {
                current.add(
                    CourseAttendance(
                        courseCode = code,
                        courseTitle = title.ifBlank { code },
                        attendedClasses = 0,
                        totalClasses = 0
                    )
                )
                modified = true
            }
        }

        if (modified) {
            saveAttendance(current)
        }
    }

    suspend fun resetCourse(courseCode: String) {
        val current = getAllAttendance().map {
            if (it.courseCode.equals(courseCode, ignoreCase = true)) {
                it.copy(attendedClasses = 0, totalClasses = 0)
            } else it
        }
        saveAttendance(current)
    }

    suspend fun clearAll() {
        context.attendanceDataStore.edit { preferences ->
            preferences[attendanceKey] = "[]"
        }
    }

    private suspend fun saveAttendance(list: List<CourseAttendance>) {
        val json = gson.toJson(list)
        context.attendanceDataStore.edit { preferences ->
            preferences[attendanceKey] = json
        }
    }
}
