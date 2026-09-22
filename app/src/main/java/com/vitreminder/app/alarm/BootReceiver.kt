package com.vitreminder.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vitreminder.app.data.local.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val repository = TimetableRepository(context)
            val scheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val timetable = repository.currentTimetableFlow.firstOrNull() ?: return@launch
                val settings = repository.userSettingsFlow.firstOrNull() ?: return@launch
                scheduler.rescheduleAll(timetable, settings)
                NotificationHelper.updatePermanentNotification(context, timetable, settings)
            }
        }
    }
}
