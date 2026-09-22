package com.vitreminder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitreminder.app.data.model.ClassNote
import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.ui.components.AddNoteBottomSheet
import com.vitreminder.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class NoteFilter(val title: String) {
    ALL("All"),
    ACTIVE_CLASS("Active Class"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

@Composable
fun NotesScreen(
    notes: List<ClassNote>,
    activeSession: ClassSession?,
    availableSessions: List<ClassSession> = emptyList(),
    onToggleNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onAddNote: (String, ClassSession?) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(NoteFilter.ALL) }
    var showAddModal by remember { mutableStateOf(false) }
    var preselectedSession by remember { mutableStateOf<ClassSession?>(null) }

    val filteredNotes = remember(notes, selectedFilter, activeSession) {
        when (selectedFilter) {
            NoteFilter.ALL -> notes
            NoteFilter.ACTIVE_CLASS -> {
                if (activeSession != null) {
                    notes.filter {
                        (it.subjectCode.isNotBlank() && it.subjectCode.equals(activeSession.subjectCode, ignoreCase = true)) ||
                        (it.subjectName.isNotBlank() && it.subjectName.equals(activeSession.displayTitle, ignoreCase = true))
                    }
                } else notes
            }
            NoteFilter.PENDING -> notes.filterNot { it.isCompleted }
            NoteFilter.COMPLETED -> notes.filter { it.isCompleted }
        }
    }

    Scaffold(
        containerColor = BgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    preselectedSession = activeSession
                    showAddModal = true
                },
                containerColor = AccentBlue,
                contentColor = SurfaceDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Active Class Banner (if class is ongoing right now)
            if (activeSession != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ONGOING: ${activeSession.displayTitle}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Room ${activeSession.classroom.ifBlank { "N/A" }} • ${activeSession.startTime}-${activeSession.endTime}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                preselectedSession = activeSession
                                showAddModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("+ Note", color = SurfaceDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Filter Chips & Clear Completed
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(NoteFilter.values()) { filter ->
                        if (filter == NoteFilter.ACTIVE_CLASS && activeSession == null) {
                            // Don't show active class filter if no class currently ongoing
                            return@items
                        }
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else CardDark)
                                .border(1.dp, if (isSelected) AccentBlue else GlassBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = filter.title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                val hasCompleted = notes.any { it.isCompleted }
                if (hasCompleted) {
                    TextButton(
                        onClick = onClearCompleted,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Clear done", color = AccentRed, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes / Tasks List
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (notes.isEmpty()) "No reminders or notes yet" else "No matching tasks",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add reminders during lectures or for upcoming labs!",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteItemCard(
                            note = note,
                            onToggle = { onToggleNote(note.id) },
                            onDelete = { onDeleteNote(note.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddModal) {
        AddNoteBottomSheet(
            activeSession = activeSession,
            availableSessions = availableSessions,
            preselectedSession = preselectedSession,
            onDismiss = {
                showAddModal = false
                preselectedSession = null
            },
            onSaveNote = onAddNote
        )
    }
}

@Composable
fun NoteItemCard(
    note: ClassNote,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (note.isCompleted) CardDark.copy(alpha = 0.5f) else CardDark
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (note.isCompleted) GlassBorder else GlassBorderActive
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Interactive Checkbox
            Checkbox(
                checked = note.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentGreen,
                    uncheckedColor = TextMuted,
                    checkmarkColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.text,
                    fontSize = 14.5.sp,
                    color = if (note.isCompleted) TextMuted else TextPrimary,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = if (note.isCompleted) FontWeight.Normal else FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (note.isClassBound) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentPurple.copy(alpha = 0.15f))
                                .border(0.5.dp, AccentPurple.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = note.displayTag,
                                fontSize = 10.sp,
                                color = AccentPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val dateStr = remember(note.createdAt) {
                        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        sdf.format(Date(note.createdAt))
                    }

                    Text(
                        text = dateStr,
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
