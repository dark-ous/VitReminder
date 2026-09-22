package com.vitreminder.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vitreminder.app.data.local.UserSettings
import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.data.model.Timetable
import com.vitreminder.app.ui.MainActivity
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_CLASSES = "channel_class_reminders"
    const val CHANNEL_BREAKS = "channel_break_alerts"
    const val CHANNEL_SUMMARY = "channel_day_summary"
    const val CHANNEL_PERMANENT = "channel_ongoing_class"
    const val NOTIFICATION_ID_PERMANENT = 8888

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val classChannel = NotificationChannel(
                CHANNEL_CLASSES,
                "Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent reminders for upcoming classes with room number and teacher name"
                enableVibration(true)
                enableLights(true)
            }

            val breakChannel = NotificationChannel(
                CHANNEL_BREAKS,
                "Break & Free Period Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for class breaks and free periods"
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY,
                "Daily Overview & End of Day",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning timetable overview and end-of-classes alerts"
            }

            val liveChannel = NotificationChannel(
                CHANNEL_PERMANENT,
                "Current / Next Class (Permanent)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always-visible notification for current and next upcoming class details"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(classChannel, breakChannel, summaryChannel, liveChannel))
        }
    }

    fun showTestNotification(context: Context) {
        showClassReminder(
            context = context,
            notificationId = 9999,
            subjectName = "Engineering Mechanics (TEST)",
            loadType = "Lab",
            classroom = "1204",
            facultyName = "Prof. J. A. Patel",
            startTime = "10:00",
            endTime = "12:00",
            minutesBefore = 10
        )
    }

    fun showClassReminder(
        context: Context,
        notificationId: Int,
        subjectName: String,
        loadType: String,
        classroom: String,
        facultyName: String,
        startTime: String,
        endTime: String,
        minutesBefore: Int
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomTag = if (classroom.isNotBlank()) "📍 Room $classroom" else "Online / TBA"
        val profTag = if (facultyName.isNotBlank()) " • 👨‍🏫 $facultyName" else ""
        val leadTag = if (minutesBefore > 0) "in $minutesBefore mins" else "starting now"

        val title = "Class $leadTag: $subjectName"
        val body = "$roomTag$profTag ($startTime - $endTime • $loadType)"

        val notification = NotificationCompat.Builder(context, CHANNEL_CLASSES)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nDon't forget your materials for $loadType!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafely(context, notificationId, notification)
    }

    fun showBreakAlert(
        context: Context,
        notificationId: Int,
        breakStartTime: String,
        breakEndTime: String,
        durationFormatted: String,
        nextSubject: String,
        nextRoom: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Break Time: $durationFormatted free ($breakStartTime - $breakEndTime)"
        val nextClassInfo = if (nextSubject.isNotBlank()) {
            "\nNext class: $nextSubject @ Room $nextRoom at $breakEndTime"
        } else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_BREAKS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Enjoy your break! Grab lunch or relax.$nextClassInfo")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Enjoy your break! Grab lunch or relax.$nextClassInfo"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafely(context, notificationId, notification)
    }

    fun showDayEndAlert(context: Context, notificationId: Int, dayName: String) {
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("All Classes Finished For Today! 🎉")
            .setContentText("You are done with all classes for $dayName. Have a great evening!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafely(context, notificationId, notification)
    }

    fun showMorningOverview(
        context: Context,
        notificationId: Int,
        dayName: String,
        sessionCount: Int,
        firstClassTime: String,
        firstClassRoom: String,
        firstClassSubject: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Good Morning! $dayName's Schedule"
        val body = "You have $sessionCount classes today. First class: $firstClassSubject at $firstClassTime (Room $firstClassRoom)."

        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafely(context, notificationId, notification)
    }

    fun showFirstClassDayStartNotification(
        context: Context,
        notificationId: Int,
        dayName: String,
        sessionCount: Int,
        firstClassTime: String,
        firstClassRoom: String,
        firstClassSubject: String,
        facultyName: String,
        loadType: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", 0) // Open Today's classes
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomStr = if (firstClassRoom.isNotBlank()) "Room $firstClassRoom" else "Room TBA"
        val profStr = if (facultyName.isNotBlank()) " • 👨‍🏫 $facultyName" else ""
        val title = "📅 Today's 1st Class: $firstClassSubject"
        val body = "Starts at $firstClassTime in 📍 $roomStr$profStr ($loadType). $sessionCount classes scheduled today."

        val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifySafely(context, notificationId, notification)
    }

    fun updatePermanentNotification(
        context: Context,
        timetable: Timetable?,
        settings: UserSettings
    ) {
        if (!settings.isOnboarded || timetable == null) {
            dismissPermanentNotification(context)
            return
        }

        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val filteredSessions = timetable.getFilteredSessions(settings.selectedBatch)
        val todaySessions = filteredSessions
            .filter { it.dayOfWeek == currentDayOfWeek }
            .sortedBy { it.startMinute }

        val ongoing = todaySessions.firstOrNull { currentMinute in it.startMinute until it.endMinute }
        val next = todaySessions.firstOrNull { it.startMinute > currentMinute }
        val lastSession = todaySessions.lastOrNull()
        val twoHoursAfterEnd = lastSession != null && currentMinute >= (lastSession.endMinute + 120)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", 1) // Open directly into Week/Grid
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_PERMANENT,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val body: String
        val subText: String

        when {
            ongoing != null -> {
                val remainingMins = ongoing.endMinute - currentMinute
                val roomStr = if (ongoing.classroom.isNotBlank()) "Room ${ongoing.classroom}" else "Room TBA"
                val profStr = if (ongoing.facultyName.isNotBlank()) " • 👨‍🏫 ${ongoing.facultyName}" else ""
                title = "Ongoing: ${ongoing.displayTitle} ($roomStr)"
                body = "Ends in ${remainingMins}m (${ongoing.endTime}) • ${ongoing.loadType}$profStr"
                subText = if (next != null) {
                    val nextRoom = if (next.classroom.isNotBlank()) "Room ${next.classroom}" else "TBA"
                    "Next: ${next.displayTitle} @ ${next.startTime} ($nextRoom)"
                } else {
                    "Last class for today!"
                }
            }
            next != null -> {
                val untilMins = next.startMinute - currentMinute
                val roomStr = if (next.classroom.isNotBlank()) "Room ${next.classroom}" else "Room TBA"
                val profStr = if (next.facultyName.isNotBlank()) " • 👨‍🏫 ${next.facultyName}" else ""
                val isFirstClass = todaySessions.firstOrNull()?.id == next.id && currentMinute < next.startMinute
                title = if (isFirstClass) "1st Class Today: ${next.displayTitle} (${next.startTime})" else "Next: ${next.displayTitle} in ${untilMins}m (${next.startTime})"
                body = "📍 $roomStr • ${next.loadType}$profStr"
                val remainingCount = todaySessions.count { it.startMinute >= next.startMinute }
                subText = "$remainingCount classes remaining today"
            }
            twoHoursAfterEnd || todaySessions.isEmpty() -> {
                // Classes finished >2 hours ago or no classes today -> Preview next day!
                val nextDaySession = findNextAvailableSession(filteredSessions, currentDayOfWeek)
                if (nextDaySession != null) {
                    title = "Tomorrow: ${nextDaySession.displayTitle} (${nextDaySession.startTime})"
                    body = "📍 Room ${nextDaySession.classroom.ifBlank { "TBA" }} • 👨‍🏫 ${nextDaySession.facultyName.ifBlank { nextDaySession.loadType }}"
                    subText = "${nextDaySession.dayName}'s 1st Class • Batch ${settings.selectedBatch}"
                } else {
                    title = "All Classes Done For The Week! 🎉"
                    body = "Enjoy your weekend!"
                    subText = "Batch ${settings.selectedBatch}"
                }
            }
            todaySessions.isNotEmpty() -> {
                // Classes finished within the last 2 hours
                val remainingMinsToPreview = (lastSession!!.endMinute + 120) - currentMinute
                val nextDaySession = findNextAvailableSession(filteredSessions, currentDayOfWeek)
                title = "Classes Finished For Today! 🎉"
                body = if (nextDaySession != null) {
                    "Tomorrow preview in ${remainingMinsToPreview}m (1st: ${nextDaySession.displayTitle} at ${nextDaySession.startTime})"
                } else {
                    "Enjoy your evening!"
                }
                subText = "Batch ${settings.selectedBatch} • ${timetable.division}"
            }
            else -> {
                val nextDaySession = findNextAvailableSession(filteredSessions, currentDayOfWeek)
                title = "No Classes Scheduled Today"
                body = if (nextDaySession != null) {
                    "Next class on ${nextDaySession.dayName} at ${nextDaySession.startTime} (${nextDaySession.displayTitle})"
                } else {
                    "No classes in current schedule"
                }
                subText = "Batch ${settings.selectedBatch} • ${timetable.division}"
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_PERMANENT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n$subText"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "View Grid", pendingIntent)
            .build()

        notifySafely(context, NOTIFICATION_ID_PERMANENT, notification)
    }

    private fun findNextAvailableSession(
        allSessions: List<ClassSession>,
        currentDayOfWeek: Int
    ): ClassSession? {
        val daysOrder = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY
        )
        val currentIndex = daysOrder.indexOf(currentDayOfWeek)
        for (i in 1..6) {
            val targetDay = daysOrder[(currentIndex + i) % daysOrder.size]
            val session = allSessions.filter { it.dayOfWeek == targetDay }.minByOrNull { it.startMinute }
            if (session != null) return session
        }
        return null
    }

    fun dismissPermanentNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_PERMANENT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifySafely(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission check
            e.printStackTrace()
        }
    }
}
