package com.origami.assistant.scheduler

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.origami.assistant.agent.AgentEvent
import com.origami.assistant.agent.AgentLoop
import com.origami.assistant.data.repository.AssistantRepository
import com.origami.assistant.data.repository.ChatRepository
import com.origami.assistant.inference.ModelManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.UUID

@HiltWorker
class AssistantWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val agentLoop: AgentLoop,
    private val assistantRepo: AssistantRepository,
    private val chatRepo: ChatRepository,
    private val modelManager: ModelManager
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_ASSISTANT_ID = "assistant_id"

        fun buildRequest(assistantId: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<AssistantWorker>()
                .setInputData(workDataOf(KEY_ASSISTANT_ID to assistantId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("assistant:$assistantId")
                .build()
    }

    override suspend fun doWork(): Result {
        val assistantId = inputData.getString(KEY_ASSISTANT_ID) ?: return Result.failure()
        val assistant = assistantRepo.getAssistant(assistantId) ?: return Result.failure()

        Timber.i("Running scheduled assistant: ${assistant.name}")

        if (!modelManager.isReady()) {
            Timber.w("Model not ready — skipping scheduled run for ${assistant.name}")
            return Result.retry()
        }

        val conversationId = chatRepo.createConversation(assistantId)
        val enabledTools = assistant.enabledTools.split(",").map { it.trim() }.toSet()

        val triggerMessage = "This is your scheduled run. Please complete your assigned tasks."

        try {
            agentLoop.run(
                conversationId = conversationId,
                userMessage = triggerMessage,
                systemPrompt = assistant.systemPrompt,
                enabledTools = enabledTools
            ).collect { event ->
                when (event) {
                    is AgentEvent.FinalResponse -> Timber.d("Scheduled run complete: ${event.text.take(100)}")
                    is AgentEvent.Error -> Timber.e("Scheduled run error: ${event.message}")
                    else -> {}
                }
            }
            assistantRepo.markRun(assistantId)
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Scheduled assistant worker failed")
            return Result.failure()
        }
    }
}
