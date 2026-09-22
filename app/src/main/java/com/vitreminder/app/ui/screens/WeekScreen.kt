package com.vitreminder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitreminder.app.data.local.UserSettings
import com.vitreminder.app.data.model.BreakPeriod
import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.data.model.Timetable
import com.vitreminder.app.parser.VitTimetableParser
import com.vitreminder.app.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    timetable: Timetable,
    settings: UserSettings,
    onBatchChange: (String) -> Unit
) {
    var isGridView by remember { mutableStateOf(true) }
    var detailSession by remember { mutableStateOf<ClassSession?>(null) }

    val days = listOf(
        "Monday" to Calendar.MONDAY,
        "Tuesday" to Calendar.TUESDAY,
        "Wednesday" to Calendar.WEDNESDAY,
        "Thursday" to Calendar.THURSDAY,
        "Friday" to Calendar.FRIDAY,
        "Saturday" to Calendar.SATURDAY
    )

    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    var selectedDayIndex by remember {
        mutableStateOf(days.indexOfFirst { it.second == currentDay }.coerceAtLeast(0))
    }

    val (selectedDayName, selectedDayOfWeek) = days[selectedDayIndex]
    val filteredSessions = timetable.getFilteredSessions(settings.selectedBatch)

    val daySessions = filteredSessions
        .filter { it.dayOfWeek == selectedDayOfWeek }
        .sortedBy { it.startMinute }

    val dayBreaks = timetable.getBreaksForDay(selectedDayOfWeek, daySessions)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(top = 12.dp)
    ) {
        // Top Controls: View Mode Toggle & Batch Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // View Mode Pill Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isGridView) AccentPurple else CardDark)
                        .clickable { isGridView = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = null,
                            tint = if (isGridView) SurfaceDark else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Weekly Grid",
                            color = if (isGridView) SurfaceDark else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (!isGridView) AccentBlue else CardDark)
                        .clickable { isGridView = false }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ViewAgenda,
                            contentDescription = null,
                            tint = if (!isGridView) SurfaceDark else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Day List",
                            color = if (!isGridView) SurfaceDark else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Batch Switcher Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val batches = timetable.availableBatches.ifEmpty { listOf("B1", "B2", "B3") }
                batches.forEach { batch ->
                    val isCurrent = settings.selectedBatch == batch
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) AccentBlue else CardDark)
                            .clickable { onBatchChange(batch) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = batch,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) SurfaceDark else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isGridView) {
            // Full Grid Table View
            FullTimetableGridView(
                timetable = timetable,
                filteredSessions = filteredSessions,
                onSessionClick = { detailSession = it }
            )
        } else {
            // Day Selector Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(days.indices.toList()) { idx ->
                    val (name, _) = days[idx]
                    val isSelected = idx == selectedDayIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentBlue else CardDark)
                            .clickable { selectedDayIndex = idx }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = name.substring(0, 3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) SurfaceDark else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subheader: Day Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedDayName Schedule",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${daySessions.size} classes • Batch ${settings.selectedBatch}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Schedule List
            if (daySessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Weekend,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Classes on $selectedDayName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Enjoy your day off!",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(daySessions) { session ->
                        WeekSessionCard(session = session, onClick = { detailSession = session })

                        val followingBreak = dayBreaks.find { it.startTime == session.endTime }
                        if (followingBreak != null) {
                            WeekBreakCard(breakPeriod = followingBreak)
                        }
                    }
                }
            }
        }
    }

    // Session Detail Dialog
    detailSession?.let { session ->
        AlertDialog(
            onDismissRequest = { detailSession = null },
            confirmButton = {
                TextButton(onClick = { detailSession = null }) {
                    Text("Close", color = AccentBlue)
                }
            },
            title = {
                Text(session.displayTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Time: ${session.dayName}, ${session.startTime} - ${session.endTime}")
                    Text("Type: ${session.loadType} ${if (!session.isCombined && session.batch != null) "(${session.batch})" else "(Combined)"}")
                    Text("Classroom: Room ${session.classroom.ifEmpty { "TBA" }}", color = AccentRed, fontWeight = FontWeight.Bold)
                    Text("Faculty: ${session.facultyName.ifEmpty { "N/A" }}")
                    Text("Subject Code: ${session.subjectCode}")
                }
            },
            containerColor = CardDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun FullTimetableGridView(
    timetable: Timetable,
    filteredSessions: List<ClassSession>,
    onSessionClick: (ClassSession) -> Unit
) {
    val hasSaturday = filteredSessions.any { it.dayOfWeek == Calendar.SATURDAY }
    val weekDays = if (hasSaturday) {
        listOf(
            "Mon" to Calendar.MONDAY,
            "Tue" to Calendar.TUESDAY,
            "Wed" to Calendar.WEDNESDAY,
            "Thu" to Calendar.THURSDAY,
            "Fri" to Calendar.FRIDAY,
            "Sat" to Calendar.SATURDAY
        )
    } else {
        listOf(
            "Mon" to Calendar.MONDAY,
            "Tue" to Calendar.TUESDAY,
            "Wed" to Calendar.WEDNESDAY,
            "Thu" to Calendar.THURSDAY,
            "Fri" to Calendar.FRIDAY
        )
    }

    val now = Calendar.getInstance()
    val todayDow = now.get(Calendar.DAY_OF_WEEK)

    // Calculate active slots: dynamically limit columns so user doesn't scroll through 4 empty evening periods
    val maxClassEndMin = filteredSessions.maxOfOrNull { it.endMinute } ?: (16 * 60)
    val activeSlots = VitTimetableParser.SLOT_TIMES.filter { (_, timePair) ->
        timePair.first < maxClassEndMin.coerceAtLeast(16 * 60)
    }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val dayHeaderWidth = 64.dp
    val timeHeaderHeight = 42.dp
    val slotCellWidth = 116.dp
    val rowCellHeight = 78.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Legend & Hint Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "👉 Sticky days & times • Tap class details",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(TheoryColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Theory", fontSize = 10.sp, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(LabColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lab", fontSize = 10.sp, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(TutorialColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tutorial", fontSize = 10.sp, color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sticky 2D Grid Layout:
        // Left column is frozen horizontally (only scrolls vertically)
        // Top header row is frozen vertically (only scrolls horizontally)
        // Center table body scrolls in both directions seamlessly!
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // LEFT COLUMN: Frozen Days Header Column
            Column(modifier = Modifier.width(dayHeaderWidth)) {
                // Top-Left Corner Cell (Fixed)
                Box(
                    modifier = Modifier
                        .width(dayHeaderWidth)
                        .height(timeHeaderHeight)
                        .background(SurfaceDark)
                        .border(0.5.dp, CardDarkHover)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Day", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = AccentBlue)
                }

                // Days Column (Synchronized Vertical Scroll)
                Column(
                    modifier = Modifier
                        .verticalScroll(verticalScrollState)
                ) {
                    weekDays.forEach { (dayLabel, dayOfWeek) ->
                        val isToday = dayOfWeek == todayDow
                        Box(
                            modifier = Modifier
                                .width(dayHeaderWidth)
                                .height(rowCellHeight)
                                .background(if (isToday) AccentBlue.copy(alpha = 0.22f) else CardDark)
                                .border(
                                    width = if (isToday) 1.5.dp else 0.5.dp,
                                    color = if (isToday) AccentBlue else CardDarkHover
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isToday) AccentBlue else AccentPurple
                                )
                                if (isToday) {
                                    Text(
                                        text = "TODAY",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 7.5.sp,
                                        color = AccentBlue
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // RIGHT AREA: Horizontally scrollable slots
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Top Header Row (Slot Times) - Synchronized Horizontal Scroll
                Row(modifier = Modifier.background(SurfaceDark)) {
                    activeSlots.forEach { (slotLabel, _) ->
                        Box(
                            modifier = Modifier
                                .width(slotCellWidth)
                                .height(timeHeaderHeight)
                                .border(0.5.dp, CardDarkHover)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = slotLabel,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Grid Body Rows (Synchronized Vertical Scroll)
                Column(
                    modifier = Modifier
                        .verticalScroll(verticalScrollState)
                ) {
                    weekDays.forEach { (_, dayOfWeek) ->
                        val daySessions = filteredSessions.filter { it.dayOfWeek == dayOfWeek }
                        Row {
                            var currentSlotIdx = 0
                            while (currentSlotIdx < activeSlots.size) {
                                val (slotLabel, timePair) = activeSlots[currentSlotIdx]
                                val slotStartMin = timePair.first

                                // Find session occurring at this slot using minute calculation
                                val session = daySessions.find {
                                    slotStartMin >= it.startMinute && slotStartMin < it.endMinute
                                }

                                if (session != null) {
                                    // Multi-hour calculation: span columns if lab is 2 hours!
                                    val sessionDurationMin = (session.endMinute - session.startMinute).coerceAtLeast(60)
                                    val spanSlots = (sessionDurationMin / 60).coerceIn(1, activeSlots.size - currentSlotIdx)

                                    val cellBg = when (session.loadType.lowercase()) {
                                        "lab" -> LabColor.copy(alpha = 0.28f)
                                        "tutorial" -> TutorialColor.copy(alpha = 0.28f)
                                        else -> TheoryColor.copy(alpha = 0.28f)
                                    }
                                    val borderColor = when (session.loadType.lowercase()) {
                                        "lab" -> LabColor
                                        "tutorial" -> TutorialColor
                                        else -> TheoryColor
                                    }

                                    val totalWidth = slotCellWidth * spanSlots

                                    Box(
                                        modifier = Modifier
                                            .width(totalWidth)
                                            .height(rowCellHeight)
                                            .background(cellBg)
                                            .border(0.5.dp, borderColor)
                                            .clickable { onSessionClick(session) }
                                            .padding(5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            if (spanSlots > 1) {
                                                Text(
                                                    text = "⚡ 2-HOUR ${session.loadType.uppercase()}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 7.5.sp,
                                                    color = LabColor
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                            }
                                            Text(
                                                text = session.displayTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.5.sp,
                                                color = TextPrimary,
                                                maxLines = 2,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (session.classroom.isNotBlank()) {
                                                    Text(
                                                        text = "📍 ${session.classroom}",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 8.5.sp,
                                                        color = AccentRed
                                                    )
                                                }
                                                val facultyCode: String = session.facultyCode.ifBlank {
                                                    session.facultyName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(3).joinToString("")
                                                }
                                                if (facultyCode.isNotBlank()) {
                                                    Text(
                                                        text = facultyCode,
                                                        fontSize = 8.sp,
                                                        color = TextSecondary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    currentSlotIdx += spanSlots
                                } else {
                                    // Free slot
                                    Box(
                                        modifier = Modifier
                                            .width(slotCellWidth)
                                            .height(rowCellHeight)
                                            .border(0.5.dp, CardDarkHover.copy(alpha = 0.4f))
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "—",
                                            fontSize = 11.sp,
                                            color = TextMuted.copy(alpha = 0.4f)
                                        )
                                    }
                                    currentSlotIdx += 1
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekSessionCard(session: ClassSession, onClick: () -> Unit) {
    val typeColor = when (session.loadType.lowercase()) {
        "lab" -> LabColor
        "tutorial" -> TutorialColor
        else -> TheoryColor
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Column
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    text = session.startTime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = session.endTime,
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = session.loadType,
                        color = typeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.displayTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                if (session.facultyName.isNotBlank()) {
                    Text(
                        text = "👨‍🏫 ${session.facultyName}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Room Column
            if (session.classroom.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Room ${session.classroom}",
                        color = AccentRed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekBreakCard(breakPeriod: BreakPeriod) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BreakColor.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Coffee,
            contentDescription = null,
            tint = BreakColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Break (${breakPeriod.startTime} - ${breakPeriod.endTime}) • ${breakPeriod.formattedDuration}",
            color = BreakColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
