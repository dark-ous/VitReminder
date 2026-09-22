package com.vitreminder.app.data.local

import android.content.Context
import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.data.model.Faculty
import com.vitreminder.app.data.model.Timetable
import com.vitreminder.app.parser.VitTimetableParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

class TimetableRepository(private val context: Context) {
    val preferencesManager = PreferencesManager(context)

    val userSettingsFlow: Flow<UserSettings> = preferencesManager.userSettingsFlow
    val currentTimetableFlow: Flow<Timetable?> = preferencesManager.cachedTimetableFlow

    suspend fun parseAndSavePdf(inputStream: InputStream, selectedBatch: String): Timetable =
        withContext(Dispatchers.IO) {
            val parser = VitTimetableParser()
            val timetable = parser.parse(inputStream)
            preferencesManager.saveTimetable(timetable, selectedBatch)
            timetable
        }

    suspend fun parseFromUrl(pdfUrl: String, selectedBatch: String): Timetable =
        withContext(Dispatchers.IO) {
            val connection = URL(pdfUrl).openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            val inputStream = connection.getInputStream()
            val parser = VitTimetableParser()
            val timetable = parser.parse(inputStream)
            preferencesManager.saveTimetable(timetable, selectedBatch)
            timetable
        }

    suspend fun parseOnly(inputStream: InputStream): Timetable =
        withContext(Dispatchers.IO) {
            val parser = VitTimetableParser()
            parser.parse(inputStream)
        }

    suspend fun getTodaySessions(): List<ClassSession> = withContext(Dispatchers.IO) {
        val timetable = currentTimetableFlow.firstOrNull() ?: return@withContext emptyList()
        val settings = userSettingsFlow.firstOrNull() ?: UserSettings()
        val calendar = java.util.Calendar.getInstance()
        val todayDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

        timetable.getFilteredSessions(settings.selectedBatch)
            .filter { it.dayOfWeek == todayDayOfWeek }
            .sortedBy { it.startMinute }
    }
}
