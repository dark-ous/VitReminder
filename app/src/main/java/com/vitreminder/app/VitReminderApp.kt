package com.vitreminder.app

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.vitreminder.app.alarm.NotificationHelper

class VitReminderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize PDFBox for Android - CRITICAL for PDF parsing
            PDFBoxResourceLoader.init(applicationContext)
            Log.d("VitReminderApp", "PDFBoxResourceLoader initialized successfully")
        } catch (e: Throwable) {
            Log.e("VitReminderApp", "Failed to initialize PDFBoxResourceLoader", e)
        }

        try {
            // Initialize notification channels on startup
            NotificationHelper.createNotificationChannels(this)
        } catch (e: Throwable) {
            Log.e("VitReminderApp", "Failed to initialize notification channels", e)
        }
    }
}
