package com.vitreminder.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vitreminder.app.data.local.UserSettings
import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.data.model.Timetable
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun rescheduleAll(timetable: Timetable, settings: UserSettings) {
        // Cancel all existing scheduled alarms
        cancelAll()

        if (!settings.isOnboarded) return

        val sessions = timetable.getFilteredSessions(settings.selectedBatch)
        val groupedByDay = sessions.groupBy { it.dayOfWeek }

        var notifIdCounter = 2000

        groupedByDay.forEach { (dayOfWeek, daySessions) ->
            if (daySessions.isEmpty()) return@forEach

            val sortedSessions = daySessions.sortedBy { it.startMinute }
            val firstSession = sortedSessions.first()
            val lastSession = sortedSessions.last()

            // 0. Day Start First Class Notification (fires at 00:01 AM when the new day starts)
            val dayStartTrigger = calculateNextOccurrence(dayOfWeek, 1)
            val dayStartIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                action = ClassAlarmReceiver.ACTION_FIRST_CLASS_DAY_START
                putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifIdCounter++)
                putExtra(ClassAlarmReceiver.EXTRA_DAY_NAME, firstSession.dayName)
                putExtra(ClassAlarmReceiver.EXTRA_SESSION_COUNT, sortedSessions.size)
                putExtra(ClassAlarmReceiver.EXTRA_START_TIME, firstSession.startTime)
                putExtra(ClassAlarmReceiver.EXTRA_CLASSROOM, firstSession.classroom)
                putExtra(ClassAlarmReceiver.EXTRA_SUBJECT, firstSession.displayTitle)
                putExtra(ClassAlarmReceiver.EXTRA_FACULTY, firstSession.facultyName)
                putExtra(ClassAlarmReceiver.EXTRA_LOAD_TYPE, firstSession.loadType)
            }
            scheduleExactAlarm(dayStartTrigger, dayStartIntent, notifIdCounter)

            // 1. Morning Overview Alarm
            if (settings.morningOverviewEnabled) {
                // 30 mins before first class or 07:30, whichever is earlier
                val morningMin = (firstSession.startMinute - 30).coerceAtLeast(7 * 60)
                val morningTrigger = calculateNextOccurrence(dayOfWeek, morningMin)

                val morningIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                    action = ClassAlarmReceiver.ACTION_MORNING_OVERVIEW
                    putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifIdCounter++)
                    putExtra(ClassAlarmReceiver.EXTRA_DAY_NAME, firstSession.dayName)
                    putExtra(ClassAlarmReceiver.EXTRA_SESSION_COUNT, sortedSessions.size)
                    putExtra(ClassAlarmReceiver.EXTRA_START_TIME, firstSession.startTime)
                    putExtra(ClassAlarmReceiver.EXTRA_CLASSROOM, firstSession.classroom)
                    putExtra(ClassAlarmReceiver.EXTRA_SUBJECT, firstSession.displayTitle)
                }
                scheduleExactAlarm(morningTrigger, morningIntent, notifIdCounter)
            }

            // 2. Pre-Class Reminders
            if (settings.preClassEnabled) {
                sortedSessions.forEach { session ->
                    val leadMin = settings.leadTimeMinutes
                    val triggerMin = (session.startMinute - leadMin).coerceAtLeast(0)
                    val triggerMillis = calculateNextOccurrence(dayOfWeek, triggerMin)

                    val classIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                        action = ClassAlarmReceiver.ACTION_CLASS_REMINDER
                        putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifIdCounter++)
                        putExtra(ClassAlarmReceiver.EXTRA_SUBJECT, session.displayTitle)
                        putExtra(ClassAlarmReceiver.EXTRA_LOAD_TYPE, session.loadType)
                        putExtra(ClassAlarmReceiver.EXTRA_CLASSROOM, session.classroom)
                        putExtra(ClassAlarmReceiver.EXTRA_FACULTY, session.facultyName)
                        putExtra(ClassAlarmReceiver.EXTRA_START_TIME, session.startTime)
                        putExtra(ClassAlarmReceiver.EXTRA_END_TIME, session.endTime)
                        putExtra(ClassAlarmReceiver.EXTRA_LEAD_MINUTES, leadMin)
                    }
                    scheduleExactAlarm(triggerMillis, classIntent, notifIdCounter)
                }
            }

            // 3. Break Alerts
            if (settings.breakAlertsEnabled) {
                val breaks = timetable.getBreaksForDay(dayOfWeek, sortedSessions)
                breaks.forEachIndexed { idx, breakPeriod ->
                    val triggerMillis = calculateNextOccurrence(dayOfWeek, breakPeriod.startMinute)
                    val nextSession = sortedSessions.firstOrNull { it.startMinute >= breakPeriod.endMinute }

                    val breakIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                        action = ClassAlarmReceiver.ACTION_BREAK_ALERT
                        putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifIdCounter++)
                        putExtra(ClassAlarmReceiver.EXTRA_START_TIME, breakPeriod.startTime)
                        putExtra(ClassAlarmReceiver.EXTRA_END_TIME, breakPeriod.endTime)
                        putExtra(ClassAlarmReceiver.EXTRA_DURATION_STR, breakPeriod.formattedDuration)
                        putExtra(ClassAlarmReceiver.EXTRA_NEXT_SUBJECT, nextSession?.displayTitle ?: "")
                        putExtra(ClassAlarmReceiver.EXTRA_NEXT_ROOM, nextSession?.classroom ?: "")
                    }
                    scheduleExactAlarm(triggerMillis, breakIntent, notifIdCounter)
                }
            }

            // 4. End-of-Day Alert
            if (settings.dayEndAlertsEnabled) {
                val triggerMillis = calculateNextOccurrence(dayOfWeek, lastSession.endMinute)
                val endIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                    action = ClassAlarmReceiver.ACTION_DAY_END
                    putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifIdCounter++)
                    putExtra(ClassAlarmReceiver.EXTRA_DAY_NAME, lastSession.dayName)
                }
                scheduleExactAlarm(triggerMillis, endIntent, notifIdCounter)
            }

            // 5. Real-Time Widget & Notification Updates (at class start and end times)
            sortedSessions.forEach { session ->
                // Class Start Time update (switches widget immediately to "ONGOING CLASS")
                val startTrigger = calculateNextOccurrence(dayOfWeek, session.startMinute)
                val startIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                    action = ClassAlarmReceiver.ACTION_SCHEDULE_UPDATE
                    putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifIdCounter++)
                }
                scheduleExactAlarm(startTrigger, startIntent, notifIdCounter)

                // Class End Time update (switches widget to break / next class)
                val endTrigger = calculateNextOccurrence(dayOfWeek, session.endMinute)
                val endIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                    action = ClassAlarmReceiver.ACTION_SCHEDULE_UPDATE
                    putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifIdCounter++)
                }
                scheduleExactAlarm(endTrigger, endIntent, notifIdCounter)
            }

            // 6. Evening Transition to Tomorrow's Timetable (2 hours after last class ends)
            val eveningPreviewMin = (lastSession.endMinute + 120).coerceAtMost(23 * 60 + 59)
            val eveningTrigger = calculateNextOccurrence(dayOfWeek, eveningPreviewMin)
            val eveningIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                action = ClassAlarmReceiver.ACTION_SCHEDULE_UPDATE
                putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, notifIdCounter++)
            }
            scheduleExactAlarm(eveningTrigger, eveningIntent, notifIdCounter)
        }
    }

    fun cancelAll() {
        // We cancel up to 500 possible pending intents
        for (id in 2000..2500) {
            val intent = Intent(context, ClassAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun scheduleExactAlarm(triggerMillis: Long, intent: Intent, requestCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun triggerTestAlarmIn3Seconds() {
        val triggerMillis = System.currentTimeMillis() + 3000L
        val testIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ClassAlarmReceiver.ACTION_CLASS_REMINDER
            putExtra(ClassAlarmReceiver.EXTRA_NOTIFICATION_ID, 9999)
            putExtra(ClassAlarmReceiver.EXTRA_SUBJECT, "Engineering Mechanics (TEST)")
            putExtra(ClassAlarmReceiver.EXTRA_LOAD_TYPE, "Lab")
            putExtra(ClassAlarmReceiver.EXTRA_CLASSROOM, "1204")
            putExtra(ClassAlarmReceiver.EXTRA_FACULTY, "Prof. J. A. Patel")
            putExtra(ClassAlarmReceiver.EXTRA_START_TIME, "10:00")
            putExtra(ClassAlarmReceiver.EXTRA_END_TIME, "12:00")
            putExtra(ClassAlarmReceiver.EXTRA_LEAD_MINUTES, 10)
        }
        scheduleExactAlarm(triggerMillis, testIntent, 9999)
    }

    fun getAlarmDiagnostics(timetable: Timetable?, settings: UserSettings): AlarmDiagnostics {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        val notifGranted = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

        if (timetable == null || !settings.isOnboarded) {
            return AlarmDiagnostics(
                totalAlarms = 0,
                nextAlarmFormatted = "No timetable loaded",
                nextAlarmSubject = "",
                nextAlarmRoom = "",
                exactAlarmsAllowed = canExact,
                notificationsAllowed = notifGranted,
                isActive = false
            )
        }

        val sessions = timetable.getFilteredSessions(settings.selectedBatch)
        var total = 0
        var earliestMillis = Long.MAX_VALUE
        var nextSubject = ""
        var nextRoom = ""
        var nextDayName = ""
        var nextTimeStr = ""

        sessions.forEach { s ->
            if (settings.preClassEnabled) {
                total++
                val triggerMin = (s.startMinute - settings.leadTimeMinutes).coerceAtLeast(0)
                val trigger = calculateNextOccurrence(s.dayOfWeek, triggerMin)
                if (trigger < earliestMillis) {
                    earliestMillis = trigger
                    nextSubject = s.displayTitle
                    nextRoom = s.classroom
                    nextDayName = s.dayName
                    nextTimeStr = s.startTime
                }
            }
        }

        val daysWithClasses = sessions.map { it.dayOfWeek }.distinct()
        if (settings.morningOverviewEnabled) total += daysWithClasses.size
        if (settings.dayEndAlertsEnabled) total += daysWithClasses.size
        if (settings.breakAlertsEnabled) {
            daysWithClasses.forEach { d ->
                val ds = sessions.filter { it.dayOfWeek == d }
                total += timetable.getBreaksForDay(d, ds).size
            }
        }

        val nextAlarmText = if (earliestMillis != Long.MAX_VALUE) {
            val cal = Calendar.getInstance().apply { timeInMillis = earliestMillis }
            val timeFmt = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
            "$nextDayName at $timeFmt (Class: $nextTimeStr)"
        } else {
            "None scheduled"
        }

        return AlarmDiagnostics(
            totalAlarms = total,
            nextAlarmFormatted = nextAlarmText,
            nextAlarmSubject = nextSubject,
            nextAlarmRoom = nextRoom,
            exactAlarmsAllowed = canExact,
            notificationsAllowed = notifGranted,
            isActive = total > 0 && canExact && notifGranted
        )
    }

    /**
     * Calculates the exact Epoch millisecond timestamp of the next occurrence of [dayOfWeek] at [minuteOfDay].
     */
    fun calculateNextOccurrence(dayOfWeek: Int, minuteOfDay: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time is in the past, advance to next week (7 days)
        if (target.before(now) || target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 7)
        }

        return target.timeInMillis
    }
}

data class AlarmDiagnostics(
    val totalAlarms: Int,
    val nextAlarmFormatted: String,
    val nextAlarmSubject: String,
    val nextAlarmRoom: String,
    val exactAlarmsAllowed: Boolean,
    val notificationsAllowed: Boolean,
    val isActive: Boolean
)
