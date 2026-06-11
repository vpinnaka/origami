package com.origami.assistant.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var schedulerManager: SchedulerManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Timber.i("Boot completed — rescheduling assistants")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                schedulerManager.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
