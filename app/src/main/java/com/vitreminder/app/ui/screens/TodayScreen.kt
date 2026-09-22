package com.vitreminder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitreminder.app.data.local.UserSettings
import com.vitreminder.app.data.model.BreakPeriod
import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.data.model.Timetable
import com.vitreminder.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TodayScreen(
    timetable: Timetable,
    settings: UserSettings,
    diagnostics: com.vitreminder.app.alarm.AlarmDiagnostics? = null,
    onNavigateToWeek: () -> Unit = {},
    onTestNotification: () -> Unit = {},
    onAddNote: ((String, ClassSession?) -> Unit)? = null
) {
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

    val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
    val formattedDate = dateFormat.format(calendar.time)

    val todaySessions = timetable.getFilteredSessions(settings.selectedBatch)
        .filter { it.dayOfWeek == dayOfWeek }
        .sortedBy { it.startMinute }

    val lastTodaySession = todaySessions.lastOrNull()
    val isAfterTwoHoursOfEnd = lastTodaySession != null && currentMinute >= (lastTodaySession.endMinute + 120)
    val shouldAutoShowNextDay = isAfterTwoHoursOfEnd || (todaySessions.isEmpty() && currentMinute >= 12 * 60)

    var forceTodayView by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val isShowingNextDay = shouldAutoShowNextDay && !forceTodayView

    var showAddNoteModal by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var noteModalSession by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ClassSession?>(null) }

    val daysOrder = listOf(
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday"
    )
    val currentDayIndex = daysOrder.indexOfFirst { it.first == dayOfWeek }
    var targetNextDay = daysOrder[(currentDayIndex + 1) % daysOrder.size]
    var nextDaySessions = timetable.getFilteredSessions(settings.selectedBatch)
        .filter { it.dayOfWeek == targetNextDay.first }
        .sortedBy { it.startMinute }

    if (nextDaySessions.isEmpty()) {
        for (i in 2..6) {
            val candidate = daysOrder[(currentDayIndex + i) % daysOrder.size]
            val candidateSessions = timetable.getFilteredSessions(settings.selectedBatch)
                .filter { it.dayOfWeek == candidate.first }
                .sortedBy { it.startMinute }
            if (candidateSessions.isNotEmpty()) {
                targetNextDay = candidate
                nextDaySessions = candidateSessions
                break
            }
        }
    }

    val displayedSessions = if (isShowingNextDay) nextDaySessions else todaySessions
    val activeDayOfWeek = if (isShowingNextDay) targetNextDay.first else dayOfWeek
    val displayedBreaks = timetable.getBreaksForDay(activeDayOfWeek, displayedSessions)

    // Current & next session calculation
    val currentSession = if (!isShowingNextDay) {
        todaySessions.find { currentMinute in it.startMinute until it.endMinute }
    } else null
    val nextSession = if (!isShowingNextDay) {
        todaySessions.find { it.startMinute > currentMinute }
    } else nextDaySessions.firstOrNull()
    val currentBreak = if (!isShowingNextDay) {
        displayedBreaks.find { currentMinute in it.startMinute until it.endMinute }
    } else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Date & Batch Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (isShowingNextDay) {
                        Text(
                            text = "${targetNextDay.second}'s Schedule (Tomorrow)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Classes ended >2h ago • ${displayedSessions.size} classes tomorrow",
                            fontSize = 12.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = formattedDate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${todaySessions.size} classes scheduled today",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentBlue.copy(alpha = 0.15f))
                        .border(1.dp, AccentBlue, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${timetable.division} • ${settings.selectedBatch}",
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick toggle button if eligible for next day preview
            if (shouldAutoShowNextDay) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { forceTodayView = !forceTodayView },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isShowingNextDay) "Show Today's Finished Classes" else "Show Tomorrow's Preview (+2h)",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Reminders & Alarm Status Card
        if (diagnostics != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardDarkHover),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(if (diagnostics.isActive) AccentGreen else AccentYellow)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (diagnostics.isActive) "REMINDERS ACTIVE" else "CHECK PERMISSIONS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diagnostics.isActive) AccentGreen else AccentYellow
                                )
                            }
                            Text(
                                text = "${diagnostics.totalAlarms} weekly alarms set",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Next: ${diagnostics.nextAlarmFormatted}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        if (diagnostics.nextAlarmSubject.isNotBlank()) {
                            Text(
                                text = "${diagnostics.nextAlarmSubject}${if (diagnostics.nextAlarmRoom.isNotBlank()) " @ Room ${diagnostics.nextAlarmRoom}" else ""}",
                                fontSize = 12.sp,
                                color = AccentBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onNavigateToWeek,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.GridOn, contentDescription = null, tint = SurfaceDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Weekly Grid", color = SurfaceDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onTestNotification,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Hero Card
        item {
            HeroCard(
                currentSession = currentSession,
                nextSession = nextSession,
                currentBreak = currentBreak,
                todaySessions = displayedSessions,
                currentMinute = currentMinute,
                isNextDayPreview = isShowingNextDay,
                onAddNoteForSession = { sess ->
                    noteModalSession = sess
                    showAddNoteModal = true
                }
            )
        }

        // Section Title
        item {
            Text(
                text = if (isShowingNextDay) "${targetNextDay.second}'s Schedule (Tomorrow)" else "Today's Schedule",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Sessions & Breaks List
        if (displayedSessions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Weekend,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isShowingNextDay) "No Classes Tomorrow! 🌴" else "No Classes Today! 🌴",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Enjoy your free time or inspect your week timetable.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToWeek,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SurfaceDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Weekly Timetable Grid", color = SurfaceDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(displayedSessions) { session ->
                SessionCard(session = session, currentMinute = if (isShowingNextDay) -1 else currentMinute)

                // Check if a break follows this session
                val followingBreak = displayedBreaks.find { it.startTime == session.endTime }
                if (followingBreak != null) {
                    BreakCard(breakPeriod = followingBreak)
                }
            }
        }
    }

    if (showAddNoteModal && onAddNote != null) {
        com.vitreminder.app.ui.components.AddNoteBottomSheet(
            activeSession = noteModalSession,
            availableSessions = todaySessions,
            preselectedSession = noteModalSession,
            onDismiss = {
                showAddNoteModal = false
                noteModalSession = null
            },
            onSaveNote = onAddNote
        )
    }
}

@Composable
private fun HeroCard(
    currentSession: ClassSession?,
    nextSession: ClassSession?,
    currentBreak: BreakPeriod?,
    todaySessions: List<ClassSession>,
    currentMinute: Int,
    isNextDayPreview: Boolean = false,
    onAddNoteForSession: (ClassSession) -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            when {
                isNextDayPreview && nextSession != null -> {
                    // Previewing Tomorrow's First Class
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TOMORROW'S FIRST CLASS",
                                color = AccentPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (nextSession.classroom.isNotBlank()) {
                            Text(
                                text = "📍 Room ${nextSession.classroom}",
                                color = AccentRed,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = nextSession.displayTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Starts at ${nextSession.startTime} - ${nextSession.endTime} • ${nextSession.loadType} • ${nextSession.facultyName}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                currentSession != null -> {
                    // Ongoing Class
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ONGOING NOW",
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (currentSession.classroom.isNotBlank()) {
                            Text(
                                text = "📍 Room ${currentSession.classroom}",
                                color = AccentRed,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentSession.displayTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${currentSession.startTime} - ${currentSession.endTime} • ${currentSession.loadType} • ${currentSession.facultyName}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    val remaining = currentSession.endMinute - currentMinute
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentMinute - currentSession.startMinute).toFloat() / currentSession.durationMinutes },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AccentGreen,
                        trackColor = CardDarkHover,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$remaining mins left",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        FilledTonalButton(
                            onClick = { onAddNoteForSession(currentSession) },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = AccentGreen.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Class Note", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                currentBreak != null -> {
                    // Ongoing Break
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Coffee, contentDescription = null, tint = BreakColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BREAK TIME (${currentBreak.startTime} - ${currentBreak.endTime})",
                            color = BreakColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${currentBreak.formattedDuration} free time to relax or grab lunch",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    if (nextSession != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Next: ${nextSession.displayTitle} @ Room ${nextSession.classroom} at ${nextSession.startTime}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                nextSession != null -> {
                    // Upcoming Class
                    val lead = nextSession.startMinute - currentMinute
                    val isFirstClass = todaySessions.firstOrNull()?.id == nextSession.id && currentMinute < nextSession.startMinute
                    val leadHours = lead / 60
                    val leadMins = lead % 60
                    val leadFormatted = when {
                        leadHours > 0 && leadMins > 0 -> "${leadHours}h ${leadMins}m"
                        leadHours > 0 -> "${leadHours}h"
                        else -> "${leadMins}m"
                    }
                    val badgeTitle = if (isFirstClass) "TODAY'S 1ST CLASS IN $leadFormatted" else "NEXT CLASS IN $leadFormatted"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = badgeTitle,
                            color = AccentYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        if (nextSession.classroom.isNotBlank()) {
                            Text(
                                text = "📍 Room ${nextSession.classroom}",
                                color = AccentRed,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = nextSession.displayTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Starts at ${nextSession.startTime} • ${nextSession.loadType} • ${nextSession.facultyName}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                todaySessions.isEmpty() -> {
                    // Free day
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Weekend, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No Classes Today! 🌴",
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have no classes scheduled today. Check the Weekly Grid for other days!",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                else -> {
                    // Done for the day
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All Done For Today! 🎉",
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have completed all classes for today. Relax and recharge!",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: ClassSession, currentMinute: Int) {
    val isCurrent = currentMinute in session.startMinute until session.endMinute
    val isPast = currentMinute >= session.endMinute

    val typeColor = when (session.loadType.lowercase()) {
        "lab" -> LabColor
        "tutorial" -> TutorialColor
        else -> TheoryColor
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) CardDarkHover else CardDark
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, AccentGreen) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Badge
                Text(
                    text = "${session.startTime} - ${session.endTime}",
                    color = if (isPast) TextMuted else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Load Type Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = session.loadType.uppercase(),
                        color = typeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subject Title
            Text(
                text = session.displayTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isPast) TextMuted else TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Room and Faculty row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (session.facultyName.isNotBlank()) "👨‍🏫 ${session.facultyName}" else "",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (session.classroom.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "📍 Room ${session.classroom}",
                            color = AccentRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakCard(breakPeriod: BreakPeriod) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BreakColor.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Coffee,
            contentDescription = null,
            tint = BreakColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Break (${breakPeriod.startTime} - ${breakPeriod.endTime}) • ${breakPeriod.formattedDuration}",
            color = BreakColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
