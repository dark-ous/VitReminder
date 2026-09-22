package com.vitreminder.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vitreminder.app.alarm.NotificationHelper
import com.vitreminder.app.ui.screens.*
import com.vitreminder.app.ui.theme.*
import com.vitreminder.app.ui.viewmodel.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: TimetableViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this)

        setContent {
            VitReminderTheme {
                val userSettings by viewModel.userSettings.collectAsState()
                val currentTimetable by viewModel.currentTimetable.collectAsState()
                val pendingTimetable by viewModel.pendingTimetable.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val successMessage by viewModel.successMessage.collectAsState()
                val notes by viewModel.notes.collectAsState()
                val attendanceList by viewModel.attendanceList.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }
                var showClearDialog by remember { mutableStateOf(false) }
                var showAttendanceSheet by remember { mutableStateOf(false) }

                // Document Picker for Upload New Timetable from Top Bar
                val newPdfPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { viewModel.parsePdfUri(it) }
                }

                // Request POST_NOTIFICATIONS permission on Android 13+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {
                    // Handled
                }

                LaunchedEffect(Unit) {
                    // 1. Notification Permission (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // 2. Exact Alarm Permission (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                LaunchedEffect(errorMessage) {
                    errorMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearMessages()
                    }
                }

                var currentTab by remember {
                    val targetTab = intent.getIntExtra("OPEN_TAB", 1)
                    mutableIntStateOf(targetTab)
                }

                LaunchedEffect(successMessage) {
                    successMessage?.let {
                        if (it.contains("Loaded", ignoreCase = true) || it.contains("Switched", ignoreCase = true)) {
                            currentTab = 1
                        }
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearMessages()
                    }
                }

                LaunchedEffect(currentTimetable, userSettings) {
                    if (currentTimetable != null) {
                        NotificationHelper.updatePermanentNotification(this@MainActivity, currentTimetable, userSettings)
                    }
                }

                val isMainContentVisible = currentTimetable != null && pendingTimetable == null

                val diagnostics by viewModel.alarmDiagnostics.collectAsState()

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = BgDark,
                    topBar = {
                        if (isMainContentVisible) {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = "VIT Reminder",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${userSettings.divisionName} • Batch ${userSettings.selectedBatch}",
                                            fontSize = 11.sp,
                                            color = AccentBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                actions = {
                                    // Attendance 75% Tracker button
                                    IconButton(onClick = { showAttendanceSheet = true }) {
                                        Icon(
                                            Icons.Default.PieChart,
                                            contentDescription = "75% Attendance Tracker",
                                            tint = AccentGreen
                                        )
                                    }
                                    // Upload / Replace PDF button
                                    IconButton(onClick = { newPdfPicker.launch("application/pdf") }) {
                                        Icon(
                                            Icons.Default.UploadFile,
                                            contentDescription = "Upload New PDF",
                                            tint = AccentBlue
                                        )
                                    }
                                    // Clear Timetable button
                                    IconButton(onClick = { showClearDialog = true }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Clear Timetable",
                                            tint = AccentRed
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
                            )
                        }
                    },
                    bottomBar = {
                        if (isMainContentVisible) {
                            NavigationBar(
                                containerColor = SurfaceDark,
                                contentColor = TextPrimary
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Today") },
                                    label = { Text("Today") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentBlue,
                                        selectedTextColor = AccentBlue,
                                        indicatorColor = AccentBlue.copy(alpha = 0.2f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Week") },
                                    label = { Text("Week") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentPurple,
                                        selectedTextColor = AccentPurple,
                                        indicatorColor = AccentPurple.copy(alpha = 0.2f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Notes") },
                                    label = { Text("Notes") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentBlue,
                                        selectedTextColor = AccentBlue,
                                        indicatorColor = AccentBlue.copy(alpha = 0.2f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentGreen,
                                        selectedTextColor = AccentGreen,
                                        indicatorColor = AccentGreen.copy(alpha = 0.2f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        if (!isMainContentVisible) {
                            OnboardingScreen(
                                isLoading = isLoading,
                                pendingTimetable = pendingTimetable,
                                errorMessage = errorMessage,
                                successMessage = successMessage,
                                onPickPdf = { uri -> viewModel.parsePdfUri(uri) },
                                onFetchUrl = { url -> viewModel.parsePdfFromUrl(url) },
                                onQuickLoad = { viewModel.loadBundledSample() },
                                onConfirmBatch = { batch ->
                                    viewModel.confirmBatchAndSave(batch)
                                    currentTab = 1 // Take user directly to Weekly Grid View!
                                },
                                onSelectDirectoryDivision = { div, batch ->
                                    viewModel.loadDirectoryDivision(div, batch)
                                    currentTab = 1
                                }
                            )
                        } else {
                            val timetable = currentTimetable!!
                            when (currentTab) {
                                0 -> TodayScreen(
                                    timetable = timetable,
                                    settings = userSettings,
                                    diagnostics = diagnostics,
                                    onNavigateToWeek = { currentTab = 1 },
                                    onTestNotification = { viewModel.testNotificationNow() },
                                    onAddNote = { text, sess -> viewModel.addNote(text, sess) }
                                )
                                1 -> WeekScreen(
                                    timetable = timetable,
                                    settings = userSettings,
                                    onBatchChange = { viewModel.changeBatch(it) }
                                )
                                2 -> {
                                    val calendar = java.util.Calendar.getInstance()
                                    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                                    val currentMin = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
                                    val todaySessions = timetable.getFilteredSessions(userSettings.selectedBatch).filter { it.dayOfWeek == dayOfWeek }
                                    val activeSession = todaySessions.find { currentMin in it.startMinute until it.endMinute }

                                    NotesScreen(
                                        notes = notes,
                                        activeSession = activeSession,
                                        availableSessions = todaySessions,
                                        onToggleNote = { viewModel.toggleNote(it) },
                                        onDeleteNote = { viewModel.deleteNote(it) },
                                        onClearCompleted = { viewModel.clearCompletedNotes() },
                                        onAddNote = { text, sess -> viewModel.addNote(text, sess) }
                                    )
                                }
                                3 -> SettingsScreen(
                                    timetable = timetable,
                                    settings = userSettings,
                                    diagnostics = diagnostics,
                                    onBatchChange = { viewModel.changeBatch(it) },
                                    onLeadTimeChange = { viewModel.updateLeadTime(it) },
                                    onTogglesChange = { pre, brk, dend, mrn ->
                                        viewModel.updateNotificationToggles(pre, brk, dend, mrn)
                                    },
                                    onPickNewPdf = { uri -> viewModel.parsePdfUri(uri) },
                                    onTestNotificationNow = { viewModel.testNotificationNow() },
                                    onTestNotification3s = { viewModel.testNotificationIn3Seconds() },
                                    onClearTimetable = { viewModel.clearTimetable() }
                                )
                            }
                        }
                    }
                }

                // Global Clear Timetable Dialog
                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text("Clear Current Timetable?", fontWeight = FontWeight.Bold, color = TextPrimary) },
                        text = {
                            Text(
                                "This will clear your loaded timetable and scheduled alarms so you can upload or fetch a new timetable.",
                                color = TextSecondary
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showClearDialog = false
                                    viewModel.clearTimetable()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                            ) {
                                Text("Clear & Reset", color = TextPrimary, fontWeight = FontWeight.Bold)
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

                // Attendance 75% Tracker BottomSheet
                if (showAttendanceSheet) {
                    com.vitreminder.app.ui.components.AttendanceSheet(
                        attendanceList = attendanceList,
                        onRecordAttendance = { code, title, attended ->
                            viewModel.recordAttendance(code, title, attended)
                        },
                        onResetCourse = { code ->
                            viewModel.resetCourseAttendance(code)
                        },
                        onDismiss = { showAttendanceSheet = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.vitreminder.app.widget.TimetableGlanceWidget.updateAll(applicationContext)
                com.vitreminder.app.widget.NotesGlanceWidget.updateAll(applicationContext)
                NotificationHelper.updatePermanentNotification(
                    applicationContext,
                    viewModel.currentTimetable.value,
                    viewModel.userSettings.value
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
