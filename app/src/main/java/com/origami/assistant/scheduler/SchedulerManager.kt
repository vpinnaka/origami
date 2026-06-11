package com.origami.assistant.scheduler

import android.content.Context
import androidx.work.*
import com.origami.assistant.data.db.entity.AssistantEntity
import com.origami.assistant.data.repository.AssistantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assistantRepo: AssistantRepository
) {
    private val workManager = WorkManager.getInstance(context)

    /** Schedule (or re-schedule) all active assistants with non-manual schedules */
    suspend fun rescheduleAll() {
        val scheduled = assistantRepo.getScheduled()
        Timber.d("Rescheduling ${scheduled.size} assistants")
        scheduled.forEach { scheduleAssistant(it) }
    }

    fun scheduleAssistant(assistant: AssistantEntity) {
        val delay = calculateInitialDelay(assistant)
        val request = when (assistant.scheduleType) {
            "daily" -> PeriodicWorkRequestBuilder<AssistantWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(AssistantWorker.KEY_ASSISTANT_ID to assistant.id))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("assistant:${assistant.id}")
                .build()

            "weekly" -> PeriodicWorkRequestBuilder<AssistantWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(AssistantWorker.KEY_ASSISTANT_ID to assistant.id))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("assistant:${assistant.id}")
                .build()

            else -> return
        }

        workManager.enqueueUniquePeriodicWork(
            "assistant_${assistant.id}",
            ExistingPeriodicWorkPolicy.UPDATE,
            request as PeriodicWorkRequest
        )
        Timber.i("Scheduled ${assistant.name} (${assistant.scheduleType}, delay=${delay}ms)")
    }

    fun cancelAssistant(assistantId: String) {
        workManager.cancelUniqueWork("assistant_$assistantId")
    }

    fun runNow(assistantId: String) {
        val request = AssistantWorker.buildRequest(assistantId)
        workManager.enqueue(request)
    }

    private fun calculateInitialDelay(assistant: AssistantEntity): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, assistant.scheduleTimeHour)
            set(Calendar.MINUTE, assistant.scheduleTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (assistant.scheduleType == "weekly") {
                set(Calendar.DAY_OF_WEEK, assistant.scheduleDayOfWeek)
                if (before(now)) add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0)
    }
}
