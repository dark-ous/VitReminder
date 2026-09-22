package com.vitreminder.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vitreminder.app.data.local.TimetableRepository
import com.vitreminder.app.data.local.UserSettings
import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.ui.MainActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

class TimetableGlanceWidget : GlanceAppWidget() {

    companion object {
        suspend fun updateAll(context: Context) {
            try {
                TimetableGlanceWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = TimetableRepository(context)

        // Resilient timetable and settings loading with timeout and memory fallback
        val timetable = try {
            withTimeoutOrNull(2500) {
                repository.currentTimetableFlow.firstOrNull()
            } ?: repository.preferencesManager.getFastTimetable()
        } catch (e: Exception) {
            repository.preferencesManager.getFastTimetable()
        }

        val settings = try {
            withTimeoutOrNull(1500) {
                repository.userSettingsFlow.firstOrNull()
            } ?: UserSettings()
        } catch (e: Exception) {
            UserSettings()
        }

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val currentMin = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val todaySessions = if (timetable != null) {
            timetable.getFilteredSessions(settings.selectedBatch)
                .filter { it.dayOfWeek == dayOfWeek }
                .sortedBy { it.startMinute }
        } else emptyList()

        val lastTodaySession = todaySessions.lastOrNull()
        val isAfterTwoHoursOfEnd = lastTodaySession != null && currentMin >= (lastTodaySession.endMinute + 120)
        val shouldAutoShowNextDay = isAfterTwoHoursOfEnd || (todaySessions.isEmpty() && currentMin >= 12 * 60)

        // Calculate tomorrow's sessions if today is finished or weekend
        val daysOrder = listOf(
            Calendar.MONDAY to "Monday",
            Calendar.TUESDAY to "Tuesday",
            Calendar.WEDNESDAY to "Wednesday",
            Calendar.THURSDAY to "Thursday",
            Calendar.FRIDAY to "Friday",
            Calendar.SATURDAY to "Saturday"
        )
        val currentDayIndex = daysOrder.indexOfFirst { it.first == dayOfWeek }
        var targetNextDay = daysOrder[((currentDayIndex + 1).coerceAtLeast(0)) % daysOrder.size]
        var nextDaySessions = timetable?.getFilteredSessions(settings.selectedBatch)
            ?.filter { it.dayOfWeek == targetNextDay.first }
            ?.sortedBy { it.startMinute } ?: emptyList()

        if (nextDaySessions.isEmpty() && timetable != null) {
            for (i in 2..5) {
                val nextIdx = (currentDayIndex + i) % daysOrder.size
                val cand = daysOrder[nextIdx]
                val candSessions = timetable.getFilteredSessions(settings.selectedBatch)
                    .filter { it.dayOfWeek == cand.first }
                    .sortedBy { it.startMinute }
                if (candSessions.isNotEmpty()) {
                    targetNextDay = cand
                    nextDaySessions = candSessions
                    break
                }
            }
        }

        val currentSession = todaySessions.find { currentMin in it.startMinute until it.endMinute }
        val nextTodaySession = todaySessions.find { it.startMinute > currentMin }

        val isShowingNextDay = shouldAutoShowNextDay && nextDaySessions.isNotEmpty() && currentSession == null

        provideContent {
            GlanceTheme {
                WidgetContent(
                    division = settings.divisionName,
                    batch = settings.selectedBatch,
                    currentSession = currentSession,
                    nextSession = if (isShowingNextDay) nextDaySessions.firstOrNull() else nextTodaySession,
                    isNextDayPreview = isShowingNextDay,
                    nextDayName = targetNextDay.second,
                    todaySessions = todaySessions,
                    currentMin = currentMin
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        division: String,
        batch: String,
        currentSession: ClassSession?,
        nextSession: ClassSession?,
        isNextDayPreview: Boolean,
        nextDayName: String,
        todaySessions: List<ClassSession>,
        currentMin: Int
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1E1E2E)))
                .padding(10.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Header Row: Division & Batch + Refresh & Brand
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$division • Batch $batch",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF89B4FA)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                // Interactive Refresh Button
                Text(
                    text = "🔄",
                    style = TextStyle(
                        fontSize = 12.sp
                    ),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshWidgetCallback>())
                        .padding(horizontal = 4.dp)
                )
                Text(
                    text = "VIT",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6ADC8)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Main Status Box
            val activeSession = currentSession ?: nextSession
            if (activeSession != null) {
                val isOngoing = currentSession != null
                val isFirstClassToday = !isNextDayPreview && todaySessions.firstOrNull()?.id == activeSession.id && currentMin < activeSession.startMinute
                val statusLabel = when {
                    isOngoing -> "🔴 ONGOING CLASS"
                    isNextDayPreview -> "🌅 TOMORROW (${nextDayName.take(3).uppercase()})"
                    isFirstClassToday -> "📅 TODAY'S 1ST CLASS"
                    else -> "⏱️ NEXT UP"
                }
                val statusColor = when {
                    isOngoing -> 0xFFA6E3A1
                    isNextDayPreview -> 0xFF89B4FA
                    isFirstClassToday -> 0xFFF9E2AF
                    else -> 0xFFF9E2AF
                }

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF313244)))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusLabel,
                            style = TextStyle(
                                color = ColorProvider(androidx.compose.ui.graphics.Color(statusColor)),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (activeSession.classroom.isNotBlank()) {
                            Text(
                                text = "📍 Room ${activeSession.classroom}",
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF38BA8)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(3.dp))

                    Text(
                        text = activeSession.displayTitle,
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFCDD6F4)),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    val timeDesc = if (isOngoing) {
                        val minsLeft = (activeSession.endMinute - currentMin).coerceAtLeast(0)
                        "${activeSession.startTime}-${activeSession.endTime} ($minsLeft mins left) • ${activeSession.facultyName.ifBlank { activeSession.facultyCode }}"
                    } else {
                        "${activeSession.startTime}-${activeSession.endTime} • ${activeSession.loadType} • ${activeSession.facultyName.ifBlank { activeSession.facultyCode }}"
                    }

                    Text(
                        text = timeDesc,
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFBAC2DE)),
                            fontSize = 10.5.sp
                        ),
                        maxLines = 1
                    )
                }
            } else {
                // No more classes today
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF313244)))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (todaySessions.isEmpty()) "No classes scheduled today 🌴" else "All done for today! 🎉",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6E3A1)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Tap to open timetable",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6ADC8)),
                            fontSize = 10.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Footer info / remaining count
            if (!isNextDayPreview) {
                val remaining = todaySessions.filter { it.startMinute > currentMin }
                if (remaining.size > 1) {
                    Text(
                        text = "+ ${remaining.size - 1} more class${if (remaining.size > 2) "es" else ""} scheduled today",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6ADC8)),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}
