package com.vitreminder.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitreminder.app.alarm.AlarmScheduler
import com.vitreminder.app.alarm.NotificationHelper
import com.vitreminder.app.data.local.TimetableRepository
import com.vitreminder.app.data.local.UserSettings
import com.vitreminder.app.data.model.Timetable
import com.vitreminder.app.widget.TimetableGlanceWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TimetableViewModel(application: Application) : AndroidViewModel(application) {

    val repository = TimetableRepository(application)
    val notesRepository = com.vitreminder.app.data.local.NotesRepository(application)
    private val scheduler = AlarmScheduler(application)

    val userSettings: StateFlow<UserSettings> = repository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val notes: StateFlow<List<com.vitreminder.app.data.model.ClassNote>> = notesRepository.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTimetable = MutableStateFlow<Timetable?>(repository.preferencesManager.getFastTimetable())
    val currentTimetable: StateFlow<Timetable?> = _currentTimetable.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _pendingTimetable = MutableStateFlow<Timetable?>(null)
    val pendingTimetable: StateFlow<Timetable?> = _pendingTimetable.asStateFlow()

    fun addNote(
        text: String,
        session: com.vitreminder.app.data.model.ClassSession? = null,
        dayOfWeek: Int = 0,
        dayName: String = ""
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val note = com.vitreminder.app.data.model.ClassNote(
                text = text.trim(),
                dayOfWeek = session?.dayOfWeek ?: dayOfWeek,
                dayName = session?.dayName ?: dayName,
                subjectCode = session?.subjectCode ?: "",
                subjectName = session?.displayTitle ?: "",
                classroom = session?.classroom ?: "",
                timeSlot = if (session != null) "${session.startTime} - ${session.endTime}" else "",
                batch = session?.batch ?: ""
            )
            notesRepository.addNote(note)
            com.vitreminder.app.widget.NotesGlanceWidget.updateAll(getApplication())
            _successMessage.value = "Note added!"
        }
    }

    fun toggleNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.toggleNoteCompletion(noteId)
            com.vitreminder.app.widget.NotesGlanceWidget.updateAll(getApplication())
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.deleteNote(noteId)
            com.vitreminder.app.widget.NotesGlanceWidget.updateAll(getApplication())
        }
    }

    fun clearCompletedNotes() {
        viewModelScope.launch {
            notesRepository.clearCompleted()
            com.vitreminder.app.widget.NotesGlanceWidget.updateAll(getApplication())
        }
    }

    val alarmDiagnostics: StateFlow<com.vitreminder.app.alarm.AlarmDiagnostics> = combine(
        currentTimetable,
        userSettings
    ) { tt, st ->
        scheduler.getAlarmDiagnostics(tt, st)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.vitreminder.app.alarm.AlarmDiagnostics(
            totalAlarms = 0,
            nextAlarmFormatted = "Checking schedule...",
            nextAlarmSubject = "",
            nextAlarmRoom = "",
            exactAlarmsAllowed = true,
            notificationsAllowed = true,
            isActive = false
        )
    )

    fun testNotificationNow() {
        com.vitreminder.app.alarm.NotificationHelper.showTestNotification(getApplication())
        _successMessage.value = "Test notification sent! Check your notification shade."
    }

    fun testNotificationIn3Seconds() {
        scheduler.triggerTestAlarmIn3Seconds()
        _successMessage.value = "Alarm set! Lock phone or wait 3s for reminder sound..."
    }

    init {
        // Collect disk updates into in-memory timetable
        viewModelScope.launch {
            repository.currentTimetableFlow.collect { tt ->
                if (tt != null) {
                    _currentTimetable.value = tt
                }
            }
        }

        // Auto-initialize alarms, widget, and permanent notification
        viewModelScope.launch {
            try {
                val timetable = currentTimetable.filterNotNull().first()
                val settings = userSettings.first()
                if (settings.isOnboarded) {
                    scheduler.rescheduleAll(timetable, settings)
                    TimetableGlanceWidget.updateAll(getApplication())
                    com.vitreminder.app.widget.NotesGlanceWidget.updateAll(getApplication())
                    NotificationHelper.updatePermanentNotification(getApplication(), timetable, settings)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun applyAndSaveTimetable(parsed: Timetable, forcedBatch: String? = null) {
        val currentBatch = forcedBatch ?: userSettings.value.selectedBatch
        val batchToUse = if (currentBatch in parsed.availableBatches) {
            currentBatch
        } else {
            parsed.availableBatches.firstOrNull() ?: currentBatch
        }

        _currentTimetable.value = parsed
        _pendingTimetable.value = null
        repository.preferencesManager.saveTimetable(parsed, batchToUse)

        val updatedSettings = userSettings.value.copy(
            selectedBatch = batchToUse,
            divisionName = parsed.division,
            isOnboarded = true
        )
        scheduler.rescheduleAll(parsed, updatedSettings)
        TimetableGlanceWidget.updateAll(getApplication())
        com.vitreminder.app.widget.NotesGlanceWidget.updateAll(getApplication())
        NotificationHelper.updatePermanentNotification(getApplication(), parsed, updatedSettings)
        _successMessage.value = "Loaded ${parsed.division} (Batch $batchToUse)! Showing Weekly Grid."
    }

    fun loadDirectoryDivision(division: com.vitreminder.app.data.model.DivisionItem, batch: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val cleanUrl = division.pdfUrl.trim()
                Log.d("TimetableViewModel", "Loading division ${division.divisionCode} from URL: $cleanUrl")

                val bytes = withContext(Dispatchers.IO) {
                    val conn = URL(cleanUrl).openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 25000
                    conn.readTimeout = 25000
                    conn.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    )
                    conn.setRequestProperty("Accept", "application/pdf,*/*")
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        throw IOException("Server returned HTTP $code")
                    }
                    conn.inputStream.use { it.readBytes() }
                }

                if (bytes.isEmpty()) {
                    throw IOException("Downloaded PDF is empty.")
                }

                val parsed = withContext(Dispatchers.IO) {
                    repository.parseOnly(ByteArrayInputStream(bytes))
                }

                applyAndSaveTimetable(parsed, batch)
            } catch (t: Throwable) {
                Log.e("TimetableViewModel", "Error loading division ${division.divisionCode}", t)
                _errorMessage.value = "Failed to load ${division.divisionCode}: ${t.localizedMessage ?: "Check connection and try again"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun parsePdfUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val context = getApplication<Application>()
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException("Could not read selected file from storage.")

                if (bytes.isEmpty()) {
                    throw IllegalArgumentException("Selected PDF file is empty.")
                }

                val parsed = withContext(Dispatchers.IO) {
                    repository.parseOnly(ByteArrayInputStream(bytes))
                }

                applyAndSaveTimetable(parsed)
            } catch (t: Throwable) {
                Log.e("TimetableViewModel", "Error parsing PDF from URI: $uri", t)
                _errorMessage.value = "Failed to parse PDF: ${t.localizedMessage ?: t.javaClass.simpleName}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun parsePdfFromUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val cleanUrl = url.trim()
                Log.d("TimetableViewModel", "Downloading PDF from URL: $cleanUrl")

                val bytes = withContext(Dispatchers.IO) {
                    val conn = URL(cleanUrl).openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 25000
                    conn.readTimeout = 25000
                    conn.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    )
                    conn.setRequestProperty("Accept", "application/pdf,*/*")
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        throw IOException("Server returned HTTP $code")
                    }
                    conn.inputStream.use { it.readBytes() }
                }

                if (bytes.isEmpty()) {
                    throw IOException("Downloaded PDF is empty.")
                }

                val parsed = withContext(Dispatchers.IO) {
                    repository.parseOnly(ByteArrayInputStream(bytes))
                }

                applyAndSaveTimetable(parsed)
            } catch (t: Throwable) {
                Log.e("TimetableViewModel", "Error downloading/parsing PDF from URL", t)
                _errorMessage.value = "Failed to download PDF: ${t.localizedMessage ?: "Check connection and link"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBundledSample() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val parsed = repository.preferencesManager.loadBundledDefault()
                    ?: throw IllegalStateException("Bundled timetable not found")
                applyAndSaveTimetable(parsed)
            } catch (t: Throwable) {
                Log.e("TimetableViewModel", "Error loading bundled sample timetable", t)
                _errorMessage.value = "Failed to load sample: ${t.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmBatchAndSave(batch: String) {
        val pending = _pendingTimetable.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentTimetable.value = pending
                _pendingTimetable.value = null
                repository.preferencesManager.saveTimetable(pending, batch)

                val settings = userSettings.value.copy(selectedBatch = batch, isOnboarded = true)
                scheduler.rescheduleAll(pending, settings)
                TimetableGlanceWidget.updateAll(getApplication())
                NotificationHelper.updatePermanentNotification(getApplication(), pending, settings)

                _successMessage.value = "Schedule set for Batch $batch!"
            } catch (t: Throwable) {
                Log.e("TimetableViewModel", "Error saving timetable", t)
                _errorMessage.value = "Failed to save: ${t.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changeBatch(batch: String) {
        viewModelScope.launch {
            val timetable = currentTimetable.value ?: return@launch
            repository.preferencesManager.updateBatch(batch)

            val updatedSettings = userSettings.value.copy(selectedBatch = batch)
            scheduler.rescheduleAll(timetable, updatedSettings)
            TimetableGlanceWidget.updateAll(getApplication())
            NotificationHelper.updatePermanentNotification(getApplication(), timetable, updatedSettings)

            _successMessage.value = "Switched to Batch $batch"
        }
    }

    fun updateLeadTime(minutes: Int) {
        viewModelScope.launch {
            repository.preferencesManager.updateLeadTime(minutes)
            val timetable = currentTimetable.value ?: return@launch
            val updatedSettings = userSettings.value.copy(leadTimeMinutes = minutes)
            scheduler.rescheduleAll(timetable, updatedSettings)
        }
    }

    fun updateNotificationToggles(preClass: Boolean, breaks: Boolean, dayEnd: Boolean, morning: Boolean) {
        viewModelScope.launch {
            repository.preferencesManager.updateNotificationToggles(preClass, breaks, dayEnd, morning)
            val timetable = currentTimetable.value ?: return@launch
            val updatedSettings = userSettings.value.copy(
                preClassEnabled = preClass,
                breakAlertsEnabled = breaks,
                dayEndAlertsEnabled = dayEnd,
                morningOverviewEnabled = morning
            )
            scheduler.rescheduleAll(timetable, updatedSettings)
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun resetOnboarding() {
        _pendingTimetable.value = null
    }

    fun clearTimetable() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                scheduler.cancelAll()
                NotificationHelper.dismissPermanentNotification(getApplication())
                repository.preferencesManager.clearAll()
                _pendingTimetable.value = null
                _currentTimetable.value = null
                TimetableGlanceWidget.updateAll(getApplication())
                _successMessage.value = "Timetable cleared. You can upload a new one."
            } catch (t: Throwable) {
                _errorMessage.value = "Failed to clear timetable: ${t.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
