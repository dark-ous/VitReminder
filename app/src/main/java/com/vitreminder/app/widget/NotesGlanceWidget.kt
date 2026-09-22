package com.vitreminder.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionParametersOf
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
import com.vitreminder.app.data.local.NotesRepository
import com.vitreminder.app.data.local.TimetableRepository
import com.vitreminder.app.data.local.UserSettings
import com.vitreminder.app.data.model.ClassNote
import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.ui.MainActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

class NotesGlanceWidget : GlanceAppWidget() {

    companion object {
        suspend fun updateAll(context: Context) {
            try {
                NotesGlanceWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notesRepo = NotesRepository(context)
        val timetableRepo = TimetableRepository(context)

        val notes = try {
            notesRepo.getAllNotes()
        } catch (e: Exception) {
            emptyList()
        }

        val timetable = try {
            withTimeoutOrNull(2000) {
                timetableRepo.currentTimetableFlow.firstOrNull()
            } ?: timetableRepo.preferencesManager.getFastTimetable()
        } catch (e: Exception) {
            timetableRepo.preferencesManager.getFastTimetable()
        }

        val settings = try {
            withTimeoutOrNull(1500) {
                timetableRepo.userSettingsFlow.firstOrNull()
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
        } else emptyList()

        val activeSession = todaySessions.find { currentMin in it.startMinute until it.endMinute }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    notes = notes,
                    activeSession = activeSession
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        notes: List<ClassNote>,
        activeSession: ClassSession?
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1E1E2E)))
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Header: Title + Active Class + Refresh
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 Tasks & Reminders",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFCBA6F7)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                if (activeSession != null) {
                    Text(
                        text = "🔴 ${activeSession.displayTitle.take(12)}",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6E3A1)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.padding(end = 6.dp)
                    )
                }

                Text(
                    text = "🔄",
                    style = TextStyle(fontSize = 12.sp),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshNotesCallback>())
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (notes.isEmpty()) {
                // Empty state
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF313244)))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No pending tasks or class notes! ✨",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6E3A1)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = if (activeSession != null) "Tap to jot down a note for ${activeSession.displayTitle}." else "Tap to add a reminder or checklist item.",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6ADC8)),
                            fontSize = 10.5.sp
                        )
                    )
                }
            } else {
                // Checklist items (up to 4 items)
                val displayList = notes.take(4)
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    displayList.forEach { note ->
                        val checkSymbol = if (note.isCompleted) "☑️" else "⬜"
                        val textColor = if (note.isCompleted) 0xFF6C7086 else 0xFFCDD6F4

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF313244)))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = checkSymbol,
                                style = TextStyle(fontSize = 14.sp),
                                modifier = GlanceModifier
                                    .clickable(
                                        actionRunCallback<ToggleNoteCallback>(
                                            actionParametersOf(ToggleNoteCallback.NoteIdKey to note.id)
                                        )
                                    )
                                    .padding(end = 8.dp)
                            )

                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = if (note.isCompleted) "✓ ${note.text}" else note.text,
                                    style = TextStyle(
                                        color = ColorProvider(androidx.compose.ui.graphics.Color(textColor)),
                                        fontSize = 11.5.sp,
                                        fontWeight = if (note.isCompleted) FontWeight.Normal else FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                                if (note.isClassBound || note.dayName.isNotBlank()) {
                                    Text(
                                        text = note.displayTag,
                                        style = TextStyle(
                                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF89B4FA)),
                                            fontSize = 9.5.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }

                val remaining = notes.size - displayList.size
                if (remaining > 0) {
                    Text(
                        text = "+ $remaining more task${if (remaining > 1) "s" else ""} • Tap to view all",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFA6ADC8)),
                            fontSize = 10.sp
                        ),
                        modifier = GlanceModifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
