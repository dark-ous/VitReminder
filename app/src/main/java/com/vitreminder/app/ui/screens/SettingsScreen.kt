package com.vitreminder.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitreminder.app.data.local.UserSettings
import com.vitreminder.app.data.model.Timetable
import com.vitreminder.app.ui.theme.*

@Composable
fun SettingsScreen(
    timetable: Timetable?,
    settings: UserSettings,
    diagnostics: com.vitreminder.app.alarm.AlarmDiagnostics? = null,
    onBatchChange: (String) -> Unit,
    onLeadTimeChange: (Int) -> Unit,
    onTogglesChange: (preClass: Boolean, breaks: Boolean, dayEnd: Boolean, morning: Boolean) -> Unit,
    onPickNewPdf: (Uri) -> Unit,
    onTestNotificationNow: () -> Unit = {},
    onTestNotification3s: () -> Unit = {},
    onClearTimetable: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPickNewPdf(it) }
    }

    var preClass by remember(settings.preClassEnabled) { mutableStateOf(settings.preClassEnabled) }
    var breaks by remember(settings.breakAlertsEnabled) { mutableStateOf(settings.breakAlertsEnabled) }
    var dayEnd by remember(settings.dayEndAlertsEnabled) { mutableStateOf(settings.dayEndAlertsEnabled) }
    var morning by remember(settings.morningOverviewEnabled) { mutableStateOf(settings.morningOverviewEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Preferences",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // 1. Batch Selection Section
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "My Batch",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Select your lab and tutorial batch. Theory classes are automatically included.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                val batches = timetable?.availableBatches?.ifEmpty { listOf("B1", "B2", "B3") } ?: listOf("B1", "B2", "B3")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    batches.forEach { batch ->
                        val isSelected = settings.selectedBatch == batch
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentBlue else CardDarkHover)
                                .clickable { onBatchChange(batch) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = batch,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSelected) SurfaceDark else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2. Pre-Class Lead Time Section
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pre-Class Reminder Time",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "How many minutes before each class should the reminder ring?",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                val leadOptions = listOf(5, 10, 15, 30)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    leadOptions.forEach { minutes ->
                        val isSelected = settings.leadTimeMinutes == minutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentPurple else CardDarkHover)
                                .clickable { onLeadTimeChange(minutes) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$minutes min",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) SurfaceDark else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 3. Smart Notifications Toggles
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Notification Alerts",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                NotificationToggleRow(
                    title = "Pre-Class Alarms",
                    subtitle = "Alert with room number & teacher before class",
                    checked = preClass,
                    onCheckedChange = {
                        preClass = it
                        onTogglesChange(preClass, breaks, dayEnd, morning)
                    }
                )

                HorizontalDivider(color = CardDarkHover, modifier = Modifier.padding(vertical = 8.dp))

                NotificationToggleRow(
                    title = "Break & Lunch Alerts",
                    subtitle = "Notifies when free periods start",
                    checked = breaks,
                    onCheckedChange = {
                        breaks = it
                        onTogglesChange(preClass, breaks, dayEnd, morning)
                    }
                )

                HorizontalDivider(color = CardDarkHover, modifier = Modifier.padding(vertical = 8.dp))

                NotificationToggleRow(
                    title = "End of Day Alert",
                    subtitle = "Notification when all classes are finished",
                    checked = dayEnd,
                    onCheckedChange = {
                        dayEnd = it
                        onTogglesChange(preClass, breaks, dayEnd, morning)
                    }
                )

                HorizontalDivider(color = CardDarkHover, modifier = Modifier.padding(vertical = 8.dp))

                NotificationToggleRow(
                    title = "Daily Morning Overview",
                    subtitle = "Summary of classes for the day at 07:30 AM",
                    checked = morning,
                    onCheckedChange = {
                        morning = it
                        onTogglesChange(preClass, breaks, dayEnd, morning)
                    }
                )
            }
        }

        // 4. Live Verification & Diagnostics (Is the app actually working?)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardDarkHover),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alarm & Notification Health",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (diagnostics != null) {
                        Text(
                            text = if (diagnostics.isActive) "● ALL SYSTEMS OK" else "● ATTENTION NEEDED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (diagnostics.isActive) AccentGreen else AccentYellow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (diagnostics != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Notifications Permission:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = if (diagnostics.notificationsAllowed) "✓ Allowed" else "✗ Denied (Enable in Settings)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (diagnostics.notificationsAllowed) AccentGreen else AccentRed
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Exact Alarms Permission:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = if (diagnostics.exactAlarmsAllowed) "✓ Allowed" else "✗ Denied (Required for precise alarms)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (diagnostics.exactAlarmsAllowed) AccentGreen else AccentRed
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Scheduled Alarms:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = "${diagnostics.totalAlarms} alarms for this week",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Next Upcoming Alert:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = diagnostics.nextAlarmFormatted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Verify sound, vibration & lockscreen heads-up display on this device:",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onTestNotificationNow,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = SurfaceDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Alert (Now)", color = SurfaceDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onTestNotification3s,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentYellow),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentYellow),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Alarm (3s)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 5. Timetable PDF Info & Re-upload
        if (timetable != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current Timetable Info",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Division: ${timetable.division}", fontSize = 13.sp, color = TextSecondary)
                    Text(text = "Academic Year: ${timetable.academicYear}", fontSize = 13.sp, color = TextSecondary)
                    Text(text = "Semester: ${timetable.semester}", fontSize = 13.sp, color = TextSecondary)
                    Text(text = "Effective: ${timetable.wefDate} to ${timetable.toDate}", fontSize = 13.sp, color = TextSecondary)
                    Text(text = "Total Faculty Mapped: ${timetable.facultyList.size}", fontSize = 13.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { filePicker.launch("application/pdf") },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = SurfaceDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload New Timetable PDF", color = SurfaceDark, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Current Timetable", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear Timetable?", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Text(
                        "This will remove the current timetable and all alarms, allowing you to upload a new timetable from scratch.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearDialog = false
                            onClearTimetable()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("Clear", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = CardDark
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SurfaceDark,
                checkedTrackColor = AccentGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardDarkHover
            )
        )
    }
}
