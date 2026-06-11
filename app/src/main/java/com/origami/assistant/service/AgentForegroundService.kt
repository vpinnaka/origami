package com.origami.assistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.origami.assistant.MainActivity
import com.origami.assistant.OrigamiApp
import com.origami.assistant.R
import com.origami.assistant.agent.AgentEvent
import com.origami.assistant.agent.AgentLoop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AgentForegroundService : Service() {

    @Inject lateinit var agentLoop: AgentLoop

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null

    companion object {
        const val ACTION_START = "com.origami.assistant.START_AGENT"
        const val ACTION_STOP = "com.origami.assistant.STOP_AGENT"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_SYSTEM_PROMPT = "system_prompt"
        const val NOTIFICATION_ID = 1001

        fun startIntent(ctx: Context, conversationId: String, message: String, systemPrompt: String) =
            Intent(ctx, AgentForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_SYSTEM_PROMPT, systemPrompt)
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val convId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return START_NOT_STICKY
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return START_NOT_STICKY
                val systemPrompt = intent.getStringExtra(EXTRA_SYSTEM_PROMPT) ?: ""
                startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
                runAgent(convId, message, systemPrompt)
            }
            ACTION_STOP -> {
                currentJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun runAgent(conversationId: String, message: String, systemPrompt: String) {
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                agentLoop.run(conversationId, message, systemPrompt).collect { event ->
                    when (event) {
                        is AgentEvent.ToolCalling -> updateNotification("Using ${event.toolName}…")
                        is AgentEvent.FinalResponse -> {
                            updateNotification("Done")
                            delay(1000)
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                        is AgentEvent.Error -> {
                            Timber.e("Foreground service agent error: ${event.message}")
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                        else -> {}
                    }
                }
            } catch (e: CancellationException) {
                Timber.d("Agent job cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Foreground service error")
                stopSelf()
            }
        }
    }

    private fun buildNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, OrigamiApp.CHANNEL_AGENT)
            .setContentTitle(getString(R.string.notification_agent_title))
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(status: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(status))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
