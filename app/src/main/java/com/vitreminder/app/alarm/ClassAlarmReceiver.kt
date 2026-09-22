package com.vitreminder.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitreminder.app.data.local.TimetableRepository
import com.vitreminder.app.widget.TimetableGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ClassAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CLASS_REMINDER = "com.vitreminder.app.ACTION_CLASS_REMINDER"
        const val ACTION_BREAK_ALERT = "com.vitreminder.app.ACTION_BREAK_ALERT"
        const val ACTION_DAY_END = "com.vitreminder.app.ACTION_DAY_END"
        const val ACTION_MORNING_OVERVIEW = "com.vitreminder.app.ACTION_MORNING_OVERVIEW"
        const val ACTION_FIRST_CLASS_DAY_START = "com.vitreminder.app.ACTION_FIRST_CLASS_DAY_START"
        const val ACTION_SCHEDULE_UPDATE = "com.vitreminder.app.ACTION_SCHEDULE_UPDATE"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_LOAD_TYPE = "extra_load_type"
        const val EXTRA_CLASSROOM = "extra_classroom"
        const val EXTRA_FACULTY = "extra_faculty"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
        const val EXTRA_DAY_NAME = "extra_day_name"
        const val EXTRA_SESSION_COUNT = "extra_session_count"
        const val EXTRA_DURATION_STR = "extra_duration_str"
        const val EXTRA_NEXT_SUBJECT = "extra_next_subject"
        const val EXTRA_NEXT_ROOM = "extra_next_room"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)

        when (intent.action) {
            ACTION_CLASS_REMINDER -> {
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Class"
                val loadType = intent.getStringExtra(EXTRA_LOAD_TYPE) ?: "Lecture"
                val room = intent.getStringExtra(EXTRA_CLASSROOM) ?: ""
                val faculty = intent.getStringExtra(EXTRA_FACULTY) ?: ""
                val start = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                val end = intent.getStringExtra(EXTRA_END_TIME) ?: ""
                val lead = intent.getIntExtra(EXTRA_LEAD_MINUTES, 10)

                NotificationHelper.showClassReminder(
                    context = context,
                    notificationId = notifId,
                    subjectName = subject,
                    loadType = loadType,
                    classroom = room,
                    facultyName = faculty,
                    startTime = start,
                    endTime = end,
                    minutesBefore = lead
                )
            }

            ACTION_BREAK_ALERT -> {
                val start = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                val end = intent.getStringExtra(EXTRA_END_TIME) ?: ""
                val duration = intent.getStringExtra(EXTRA_DURATION_STR) ?: ""
                val nextSub = intent.getStringExtra(EXTRA_NEXT_SUBJECT) ?: ""
                val nextRoom = intent.getStringExtra(EXTRA_NEXT_ROOM) ?: ""

                NotificationHelper.showBreakAlert(
                    context = context,
                    notificationId = notifId,
                    breakStartTime = start,
                    breakEndTime = end,
                    durationFormatted = duration,
                    nextSubject = nextSub,
                    nextRoom = nextRoom
                )
            }

            ACTION_DAY_END -> {
                val day = intent.getStringExtra(EXTRA_DAY_NAME) ?: "today"
                NotificationHelper.showDayEndAlert(context, notifId, day)
            }

            ACTION_MORNING_OVERVIEW -> {
                val day = intent.getStringExtra(EXTRA_DAY_NAME) ?: "Today"
                val count = intent.getIntExtra(EXTRA_SESSION_COUNT, 0)
                val start = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                val room = intent.getStringExtra(EXTRA_CLASSROOM) ?: ""
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Classes"

                NotificationHelper.showMorningOverview(
                    context = context,
                    notificationId = notifId,
                    dayName = day,
                    sessionCount = count,
                    firstClassTime = start,
                    firstClassRoom = room,
                    firstClassSubject = subject
                )
            }

            ACTION_FIRST_CLASS_DAY_START -> {
                val day = intent.getStringExtra(EXTRA_DAY_NAME) ?: "Today"
                val count = intent.getIntExtra(EXTRA_SESSION_COUNT, 0)
                val start = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                val room = intent.getStringExtra(EXTRA_CLASSROOM) ?: ""
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Class"
                val faculty = intent.getStringExtra(EXTRA_FACULTY) ?: ""
                val loadType = intent.getStringExtra(EXTRA_LOAD_TYPE) ?: "Lecture"

                NotificationHelper.showFirstClassDayStartNotification(
                    context = context,
                    notificationId = notifId,
                    dayName = day,
                    sessionCount = count,
                    firstClassTime = start,
                    firstClassRoom = room,
                    firstClassSubject = subject,
                    facultyName = faculty,
                    loadType = loadType
                )
            }
        }

        // Reschedule for next week (7 days later) so alarms recur every week automatically
        if (notifId != 9999 && intent.action in listOf(ACTION_CLASS_REMINDER, ACTION_BREAK_ALERT, ACTION_DAY_END, ACTION_MORNING_OVERVIEW, ACTION_FIRST_CLASS_DAY_START, ACTION_SCHEDULE_UPDATE)) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val nextWeekMillis = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000L
                val nextIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    notifId,
                    nextIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextWeekMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextWeekMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextWeekMillis, pendingIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Trigger Home Screen Widget and Permanent Notification update asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TimetableGlanceWidget.updateAll(context)
                val repo = TimetableRepository(context)
                val timetable = repo.currentTimetableFlow.firstOrNull()
                val settings = repo.userSettingsFlow.firstOrNull() ?: com.vitreminder.app.data.local.UserSettings()
                NotificationHelper.updatePermanentNotification(context, timetable, settings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
