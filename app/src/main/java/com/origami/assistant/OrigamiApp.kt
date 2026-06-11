package com.origami.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class OrigamiApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_AGENT,
                getString(R.string.notification_channel_agent),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shown while the agent is running a task" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SCHEDULER,
                getString(R.string.notification_channel_scheduler),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications from scheduled assistants" }
        )
    }

    companion object {
        const val CHANNEL_AGENT = "origami_agent"
        const val CHANNEL_SCHEDULER = "origami_scheduler"
    }
}
