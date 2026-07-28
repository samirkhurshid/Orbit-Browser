package com.orbit.browser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OBApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannels()
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Orbit Browser download progress" }

            val securityChannel = NotificationChannel(
                CHANNEL_SECURITY,
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Security warnings from Orbit Browser" }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannels(listOf(downloadChannel, securityChannel))
        }
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "orbit_downloads"
        const val CHANNEL_SECURITY  = "orbit_security"
    }
}
