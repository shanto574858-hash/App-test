package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.messaging.FirebaseMessaging
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class McqLearningApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        createNotificationChannel()
        initFirebaseComponents()
    }

    private fun initFirebaseComponents() {
        try {
            FirebaseApp.initializeApp(this)
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            }
            FirebaseMessaging.getInstance().subscribeToTopic("all_students")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.notification_channel_id)
            val channelName = getString(R.string.notification_channel_name)
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "McqLearningApp"
    }
}
